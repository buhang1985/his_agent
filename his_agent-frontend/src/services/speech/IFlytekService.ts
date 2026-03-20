import CryptoJS from 'crypto-js';
import type { RecognitionResult } from '@/types/voice';

interface IFlytekConfig {
  appId: string;
  apiKey: string;
  domain?: 'medical' | 'general';
  continuousTimeout?: number;
}

export class IFlytekService {
  private config: Required<IFlytekConfig>;
  private ws: WebSocket | null = null;
  private audioContext: AudioContext | null = null;
  private mediaStream: MediaStream | null = null;
  private scriptProcessor: ScriptProcessorNode | null = null;

  private partialResultBuffer: string = '';
  private resultTimeoutId: number | null = null;

  private heartbeatInterval: number | null = null;
  private lastAudioSent: number = 0;

  public isConnected = false;
  public isRecording = false;
  private audioReady = false;
  public error: string | null = null;
  public sid: string = '';
  
  private async initializeAudioContext() {
    if (!this.audioContext) {
      console.log('🎵 初始化 AudioContext...');
      this.audioContext = new AudioContext({ 
        sampleRate: 16000,
        latencyHint: 'playback'
      });
      console.log('🎵 AudioContext 状态:', this.audioContext.state);
      
      if (this.audioContext.state === 'suspended') {
        console.log('🎵 恢复 AudioContext...');
        await this.audioContext.resume();
      }
    }
  }

  public onResult: ((result: RecognitionResult) => void) | null = null;
  public onError: ((error: string) => void) | null = null;
  public onStatusChange: ((status: string) => void) | null = null;

  constructor(config: IFlytekConfig) {
    if (!config.appId || !config.apiKey) {
      throw new Error('缺少必要的配置：appId, apiKey');
    }
    
    this.config = {
      appId: config.appId,
      apiKey: config.apiKey,
      domain: config.domain || 'general',
      continuousTimeout: config.continuousTimeout || 1500,
    };
  }

  private buildWsUrl(): string {
    const baseUrl = 'wss://rtasr.xfyun.cn/v1/ws';
    const ts = Math.floor(Date.now() / 1000).toString();
    
    const md5Hash = CryptoJS.MD5(this.config.appId + ts).toString();
    const hmac = CryptoJS.HmacSHA1(md5Hash, this.config.apiKey);
    const signa = CryptoJS.enc.Base64.stringify(hmac);
    
    const appId = encodeURIComponent(this.config.appId);
    const tsEncoded = encodeURIComponent(ts);
    const signaEncoded = encodeURIComponent(signa);
    
    let url = `${baseUrl}?appid=${appId}&ts=${tsEncoded}&signa=${signaEncoded}`;
    
    url += '&pd=' + (this.config.domain === 'medical' ? 'medical' : 'general');
    url += '&lang=cn';
    url += '&punc=0';
    
    console.log('🏥 使用领域模型:', this.config.domain);
    console.log('🔑 签名参数:', { appId: this.config.appId, ts, signa: signa.substring(0, 20) + '...' });
    console.log('🔗 WebSocket URL:', `${baseUrl}?appid=${appId}&ts=${tsEncoded}&signa=***&pd=${this.config.domain}&lang=cn&punc=0`);
    
    return url;
  }

  async start(): Promise<void> {
    if (this.isRecording || this.isConnected) {
      await this.stop();
    }

    try {
      console.log('🎤 开始获取麦克风权限...');
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: 16000,
        },
      });
      console.log('✅ 麦克风权限已获取');

      this.isRecording = true;
      this.onStatusChange?.('recording');
      
      console.log('🔗 开始连接 WebSocket...');
      await this.initializeAudioContext();
      await this.connectWebSocketWithRetry();
      console.log('✅ WebSocket 已连接，等待服务端确认...');
      
      if (!this.scriptProcessor) {
        await this.startAudioProcessing();
      }
      
    } catch (err) {
      this.isRecording = false;
      const errorMsg = err instanceof Error ? err.message : '录音失败';
      this.error = errorMsg;
      this.onError?.(errorMsg);
      console.error('❌ 启动失败:', err);
      throw err;
    }
  }

  private async connectWebSocketWithRetry(maxRetries: number = 3): Promise<void> {
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        await this.connectWebSocket();
        return;
      } catch (error) {
        if (attempt === maxRetries) {
          throw error;
        }
        
        if (this.error && (this.error.includes('10800') || this.error.includes('35006'))) {
          console.log(`连接限制错误，等待 ${2000 * (attempt + 1)}ms 后重试...`);
          await new Promise(resolve => setTimeout(resolve, 2000 * (attempt + 1)));
        } else {
          console.log(`连接失败，等待 1000ms 后重试...`);
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
      }
    }
  }

  private async connectWebSocket(): Promise<void> {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.close();
    } else if (this.ws && this.ws.readyState === WebSocket.CONNECTING) {
      await new Promise<void>((resolve) => {
        this.ws!.onopen = () => {
          this.ws?.close();
          resolve();
        };
      });
    }

    return new Promise((resolve, reject) => {
      const url = this.buildWsUrl();
      this.ws = new WebSocket(url);

      this.ws.onopen = () => {
        console.log('✅ WebSocket 连接已建立');
        this.isConnected = true;
        this.onStatusChange?.('recording');
        this.startHeartbeat();
        resolve();
      };

      this.ws.onerror = () => {
        const errorMsg = 'WebSocket 连接失败';
        this.error = errorMsg;
        this.onError?.(errorMsg);
        this.isConnected = false;
        this.stopHeartbeat();
        reject(new Error(errorMsg));
      };

      this.ws.onmessage = (event) => {
        this.lastAudioSent = Date.now();
        this.handleMessage(event.data);
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket 关闭，code:', event.code, 'reason:', event.reason);
        this.isConnected = false;
        this.stopHeartbeat();
      };
    });
  }

  private handleMessage(data: string) {
    try {
      console.log('[WS 收到消息]:', data);
      const response = JSON.parse(data);
      console.log('讯飞 ASR 响应:', response);

      const action = response.action;
      
      if (action === 'started') {
        this.sid = response.sid;
        this.audioReady = true;
        console.log('✅ 讯飞 ASR 握手成功，SID:', this.sid);
        if (this.isRecording && this.mediaStream) {
          if (!this.scriptProcessor) {
            this.startAudioProcessing().catch(err => {
              console.error('启动音频处理失败:', err);
            });
          } else {
            console.log('✅ 音频处理器已准备就绪，开始发送音频数据');
          }
        }
        return;
      }
        }
        return;
      }

      if (action === 'result') {
        console.log('[处理识别结果] data:', response.data);
        this.handleRecognitionResult(response.data);
        return;
      }

      if (action === 'error') {
        const errorMsg = `讯飞 ASR 错误：${response.desc || '未知错误'} (code: ${response.code || 'N/A'})`;
        console.error('❌', errorMsg);
        this.error = errorMsg;
        this.onError?.(errorMsg);
        
        if (response.code === '35006' || response.code === '10800') {
          console.error('达到最大连接数限制，稍后重试...');
        }
        
        this.stop();
      }
      
      if (!['started', 'result', 'error'].includes(action)) {
        console.log('🔍 未知的响应动作:', action, response);
      }
    } catch (err) {
      console.error('解析讯飞 ASR 响应失败:', err);
    }
  }

  private handleRecognitionResult(dataStr: string) {
    try {
      console.log('[识别原始数据]:', dataStr);
      const data = JSON.parse(dataStr);
      console.log('[解析后的 data]:', data);
      
      if (!data.cn?.st) {
        console.warn('⚠️ 数据格式异常，缺少 cn.st 字段');
        return;
      }

      const segId = data.seg_id || 0;
      
      const st = data.cn.st;
      const isFinal = st.type === '0';
      console.log('[识别类型] isFinal:', isFinal, 'type:', st.type, 'seg_id:', segId);

      let text = '';
      let confidence = 1.0;

      const rtArr = st.rt;
      if (Array.isArray(rtArr)) {
        for (let i = 0; i < rtArr.length; i++) {
          const rtItem = rtArr[i];
          const wsArr = rtItem.ws;
          if (Array.isArray(wsArr)) {
            for (let j = 0; j < wsArr.length; j++) {
              const wsItem = wsArr[j];
              const cwArr = wsItem.cw;
              if (Array.isArray(cwArr)) {
                for (let k = 0; k < cwArr.length; k++) {
                  const cwItem = cwArr[k];
                  const wStr = cwItem.w;
                  text += wStr;
                }
              }
            }
          }
        }
      }

      console.log('[识别出的文字]:', text);

      if (isFinal) {
        const result: RecognitionResult = {
          text: text,
          isFinal: true,
          confidence,
          startTime: parseInt(st.bg) || 0,
          endTime: parseInt(st.ed) || 0,
        };
        
        console.log('✅ 最终结果:', result.text);
        this.onResult?.(result);
      } else {
        console.log('⏳ 中间结果:', text);
        
        if (text) {
          const intermediateResult: RecognitionResult = {
            text: text,
            isFinal: false,
            confidence,
            startTime: parseInt(st.bg) || 0,
            endTime: parseInt(st.ed) || 0,
          };
          
          this.onResult?.(intermediateResult);
        }
      }
    } catch (err) {
      console.error('❌ 解析识别结果失败:', err);
    }
  }

  private async startAudioProcessing() {
    try {
      console.log('🎵 使用现有 AudioContext...');
      if (!this.audioContext) {
        throw new Error('AudioContext 未初始化');
      }
      
      console.log('🎵 AudioContext 状态:', this.audioContext.state);
      
      if (this.audioContext.state === 'suspended') {
        console.warn('⚠️ AudioContext 被挂起，尝试恢复...');
        await this.audioContext.resume();
      }
      
      console.log('🎵 创建媒体流源...');
      const source = this.audioContext.createMediaStreamSource(this.mediaStream!);

      console.log('🎵 创建 ScriptProcessor (1024)...');
      this.scriptProcessor = this.audioContext.createScriptProcessor(1024, 1, 1);
      console.log('🎤 音频处理已启动，采样率：16000');

      let audioBuffer: Float32Array = new Float32Array(0);
      let frameCount = 0;
      let processCount = 0;
      const samplesPer40ms = Math.floor(16000 * 0.04);
      
        this.scriptProcessor.onaudioprocess = (event) => {
          processCount++;
          if (processCount <= 3 || processCount % 50 === 0) {
            console.log(`📊 onaudioprocess 调用 #${processCount}`);
          }
          
          if (!this.audioReady) {
            if (processCount <= 3) {
              console.log('⏳ 音频未就绪，等待服务端确认...');
            }
            return;
          }
          
          if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            if (processCount <= 3) {
              console.warn('⚠️ WebSocket 未连接，跳过音频发送，readyState:', this.ws?.readyState);
            }
            return;
          }

          const inputData = event.inputBuffer.getChannelData(0);
          
          // 检查是否有实际音频数据（静音检测）
          let maxAmplitude = 0;
          for (let i = 0; i < inputData.length; i++) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(inputData[i]));
          }
          if (processCount <= 3 || processCount % 50 === 0) {
            console.log(`📊 音频数据长度：${inputData.length}, 最大振幅：${maxAmplitude.toFixed(4)}`);
          }

          const newBuffer = new Float32Array(audioBuffer.length + inputData.length);
          newBuffer.set(audioBuffer);
          newBuffer.set(inputData, audioBuffer.length);
          audioBuffer = newBuffer;

          while (audioBuffer.length >= samplesPer40ms) {
            const chunk = audioBuffer.slice(0, samplesPer40ms);
            audioBuffer = audioBuffer.slice(samplesPer40ms);

            const pcmData = this.floatTo16BitPCM(chunk);
            const int8Array = new Int8Array(pcmData);

            if (this.ws.bufferedAmount < 16384) {
              this.ws.send(int8Array);
              this.lastAudioSent = Date.now();
              frameCount++;
              if (frameCount <= 5 || frameCount % 25 === 0) {
                console.log(`📤 已发送 ${frameCount} 帧音频数据，大小: ${int8Array.byteLength} bytes`);
              }
            } else {
              console.warn('⚠️ WebSocket 缓冲区已满，丢弃音频帧');
            }
          }
          
          if (processCount % 10 === 0) {
            console.log(`🎧 音频振幅监测: ${maxAmplitude > 0.01 ? '检测到声音' : '静音或低音量'}, 振幅: ${maxAmplitude.toFixed(4)}`);
          }
        };

      source.connect(this.scriptProcessor);
      console.log('✅ 音频源已连接到 ScriptProcessor');
      console.log('🎵 不连接到 destination（避免回声）');
      
    } catch (err) {
      console.error('❌ 音频处理启动失败:', err);
      throw err;
    }
  }

  private floatTo16BitPCM(float32Array: Float32Array): ArrayBuffer {
    const buffer = new ArrayBuffer(float32Array.length * 2);
    const view = new DataView(buffer);
    let offset = 0;

    for (let i = 0; i < float32Array.length; i++, offset += 2) {
      let s = Math.max(-1, Math.min(1, float32Array[i]));
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
    }

    return buffer;
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    
    this.heartbeatInterval = window.setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        const now = Date.now();
        if (now - this.lastAudioSent > 10000) {
          const silentAudio = new Int8Array(320);
          this.ws.send(silentAudio);
          this.lastAudioSent = now;
        }
      }
    }, 1000);
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  async stop(): Promise<void> {
    if (this.resultTimeoutId) {
      clearTimeout(this.resultTimeoutId);
      this.resultTimeoutId = null;
    }

    this.stopHeartbeat();

    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send('{"end": true}');
      console.log('📤 发送结束信号到服务器');
    }

    this.cleanupResources();

    this.isRecording = false;
    this.isConnected = false;
    this.audioReady = false;
    this.onStatusChange?.('stopped');
  }

  private cleanupResources(): void {
    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect();
      this.scriptProcessor = null;
    }

    if (this.audioContext) {
      this.audioContext.close().catch(console.error);
      this.audioContext = null;
    }

    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((track) => track.stop());
      this.mediaStream = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}
