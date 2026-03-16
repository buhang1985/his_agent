import CryptoJS from 'crypto-js';
import type { RecognitionResult } from '@/types/voice';

interface IFlytekConfig {
  appId: string;
  apiKey: string;
  domain?: 'medical' | 'general';
}

export class IFlytekService {
  private config: IFlytekConfig;
  private ws: WebSocket | null = null;
  private audioContext: AudioContext | null = null;
  private mediaStream: MediaStream | null = null;
  private scriptProcessor: ScriptProcessorNode | null = null;

  public isConnected = false;
  public isRecording = false;
  public error: string | null = null;
  public sid: string = '';

  public onResult: ((result: RecognitionResult) => void) | null = null;
  public onError: ((error: string) => void) | null = null;
  public onStatusChange: ((status: string) => void) | null = null;

  constructor(config: IFlytekConfig) {
    this.config = config;
  }

  private generateSigna(ts: number): string {
    const baseString = this.config.appId + ts;
    const md5Hash = CryptoJS.MD5(baseString).toString();
    const signa = CryptoJS.HmacSHA1(md5Hash, this.config.apiKey);
    return CryptoJS.enc.Base64.stringify(signa);
  }

  private buildWsUrl(): string {
    const ts = Math.floor(Date.now() / 1000);
    const signa = this.generateSigna(ts);

    // 所有参数都需要 URL 编码
    const appId = encodeURIComponent(this.config.appId);
    const tsEncoded = encodeURIComponent(ts.toString());
    const signaEncoded = encodeURIComponent(signa);
    const pd = encodeURIComponent(this.config.domain || 'general');
    const lang = encodeURIComponent('cn');
    const punc = encodeURIComponent('0');
    const roleType = encodeURIComponent('2');

    return `wss://rtasr.xfyun.cn/v1/ws?appid=${appId}&ts=${tsEncoded}&signa=${signaEncoded}&pd=${pd}&lang=${lang}&punc=${punc}&roleType=${roleType}`;
  }

  async start(): Promise<void> {
    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: 16000,
        },
      });

      this.isRecording = true;
      this.onStatusChange?.('recording');
      
      await this.connectWebSocket();
      this.startAudioProcessing();
      
    } catch (err) {
      this.isRecording = false;
      const errorMsg = err instanceof Error ? err.message : '录音失败';
      this.error = errorMsg;
      this.onError?.(errorMsg);
      throw err;
    }
  }

  private connectWebSocket(): Promise<void> {
    return new Promise((resolve, reject) => {
      const url = this.buildWsUrl();
      this.ws = new WebSocket(url);

      this.ws.onopen = () => {
        this.isConnected = true;
        this.onStatusChange?.('connected');
        resolve();
      };

      this.ws.onerror = (event) => {
        const errorMsg = 'WebSocket 连接失败';
        this.error = errorMsg;
        this.onError?.(errorMsg);
        this.isConnected = false;
        reject(new Error(errorMsg));
      };

      this.ws.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket 关闭，code:', event.code, 'reason:', event.reason);
        this.isConnected = false;
        // 注意：不在这里设置 isRecording = false，因为可能是正常关闭
      };
    });
  }

  private handleMessage(data: string) {
    try {
      const response = JSON.parse(data);
      console.log('讯飞 ASR 响应:', response);

      if (response.action === 'started') {
        this.sid = response.sid;
        console.log('讯飞 ASR 握手成功，SID:', this.sid);
      } else if (response.action === 'result') {
        this.handleRecognitionResult(response.data);
      } else if (response.action === 'error') {
        const errorMsg = `讯飞 ASR 错误：${response.desc} (code: ${response.code})`;
        console.error(errorMsg);
        this.error = errorMsg;
        this.onError?.(errorMsg);
        // 发生错误时停止录音
        this.stop();
      }
    } catch (err) {
      console.error('解析讯飞 ASR 响应失败:', err);
    }
  }

  private handleRecognitionResult(dataStr: string) {
    try {
      const data = JSON.parse(dataStr);

      if (!data.cn?.st) return;

      const st = data.cn.st;
      const isFinal = st.type === '0';

      let text = '';
      let confidence = 1.0;

      st.rt?.forEach((rtItem: any) => {
        rtItem.ws?.forEach((wsItem: any) => {
          wsItem.cw?.forEach((cwItem: any) => {
            text += cwItem.w;
            if (cwItem.wp === 's') {
              confidence *= 0.8;
            }
          });
        });
      });

      const result: RecognitionResult = {
        text,
        isFinal,
        confidence,
        startTime: parseInt(st.bg) || 0,
        endTime: parseInt(st.ed) || 0,
      };

      this.onResult?.(result);
    } catch (err) {
      console.error('解析识别结果失败:', err);
    }
  }

  private startAudioProcessing() {
    try {
      this.audioContext = new AudioContext({ sampleRate: 16000 });
      const source = this.audioContext.createMediaStreamSource(this.mediaStream!);

      this.scriptProcessor = this.audioContext.createScriptProcessor(4096, 1, 1);
      console.log('音频处理已启动，bufferSize: 4096');

      this.scriptProcessor.onaudioprocess = (event) => {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
          console.log('WebSocket 未连接，跳过音频发送');
          return;
        }

        const inputData = event.inputBuffer.getChannelData(0);
        const pcmData = this.floatTo16BitPCM(inputData);

        this.ws.send(pcmData);
      };

      source.connect(this.scriptProcessor);
      this.scriptProcessor.connect(this.audioContext.destination);
      
    } catch (err) {
      console.error('音频处理启动失败:', err);
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

  async stop(): Promise<void> {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ end: true }));
    }

    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect();
      this.scriptProcessor = null;
    }

    if (this.audioContext) {
      await this.audioContext.close();
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

    this.isRecording = false;
    this.isConnected = false;
    this.onStatusChange?.('stopped');
  }
}
