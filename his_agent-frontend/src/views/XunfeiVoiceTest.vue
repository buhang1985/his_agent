<template>
  <div class="xunfei-voice-test">
    <el-header class="page-header">
      <h2 class="page-title">
        <el-icon :size="24"><microphone /></el-icon>
        讯飞语音转写测试 (官方 Demo 参考实现)
      </h2>
    </el-header>

    <el-main class="page-main">
      <!-- 配置区域 -->
      <el-card class="config-card">
        <template #header>
          <div class="card-header">
            <el-icon><setting /></el-icon>
            <span>讯飞配置</span>
          </div>
        </template>
        <el-form :inline="true" size="default">
          <el-form-item label="AppID">
            <el-input v-model="config.appId" placeholder="8bc8b3ca" style="width: 200px" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="config.apiKey" placeholder="请输入 API Key" style="width: 300px" />
          </el-form-item>
          <el-form-item label="领域">
            <el-select v-model="config.domain" style="width: 120px">
              <el-option label="医疗" value="medical" />
              <el-option label="通用" value="general" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 控制区域 -->
      <el-card class="control-card">
        <div class="control-panel">
          <el-button
            v-if="!isRecording"
            type="primary"
            size="large"
            :loading="connecting"
            @click="startRecording"
          >
            <el-icon :size="20"><microphone /></el-icon>
            开始录音
          </el-button>
          
          <el-button
            v-else
            type="danger"
            size="large"
            @click="stopRecording"
          >
            <el-icon :size="20"><video-pause /></el-icon>
            结束录音 ({{ recordingDuration }}s)
          </el-button>

          <el-tag :type="statusTagType" size="large" effect="dark">
            {{ statusText }}
          </el-tag>
        </div>
      </el-card>

      <!-- 转写结果 -->
      <el-card class="result-card">
        <template #header>
          <div class="card-header">
            <el-icon><document-copy /></el-icon>
            <span>转写结果</span>
            <el-tag v-if="segments.length > 0" type="success" size="small">
              {{ segments.length }} 段
            </el-tag>
          </div>
        </template>

        <div v-if="segments.length === 0 && !currentTranscript" class="empty-result">
          <el-empty description="等待语音输入..." />
        </div>

        <div v-else class="transcript-display">
          <!-- 历史片段 -->
          <div
            v-for="(segment, index) in segments"
            :key="segment.id"
            class="transcript-segment"
          >
            <span class="segment-index">{{ index + 1 }}</span>
            <span class="segment-text">{{ segment.text }}</span>
            <span class="segment-time">{{ formatTime(segment.timestamp) }}</span>
          </div>

          <!-- 实时转写 -->
          <div v-if="currentTranscript && isRecording" class="transcript-segment current">
            <span class="segment-index">⏳</span>
            <span class="segment-text">{{ currentTranscript }}</span>
            <el-tag size="small" type="warning">中间结果</el-tag>
          </div>
        </div>
      </el-card>

      <!-- 调试日志 -->
      <el-card v-if="showDebug" class="debug-card">
        <template #header>
          <div class="card-header">
            <el-icon><bug /></el-icon>
            <span>调试日志</span>
            <el-button size="small" @click="clearLogs">清空</el-button>
          </div>
        </template>
        <div class="debug-logs">
          <div v-for="(log, index) in debugLogs" :key="index" class="log-item">
            {{ log }}
          </div>
        </div>
      </el-card>

      <!-- 错误提示 -->
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        closable
        @close="errorMessage = ''"
      />
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Microphone,
  VideoPause,
  DocumentCopy,
  Setting,
  Bug,
} from '@element-plus/icons-vue';
import CryptoJS from 'crypto-js';

// 配置
interface XunfeiConfig {
  appId: string;
  apiKey: string;
  domain: 'medical' | 'general';
}

const config = ref<XunfeiConfig>({
  appId: '8bc8b3ca',
  apiKey: '',
  domain: 'medical',
});

// 状态
const isRecording = ref(false);
const connecting = ref(false);
const segments = ref<Array<{
  id: string;
  text: string;
  timestamp: number;
}>>([]);
const currentTranscript = ref('');
const errorMessage = ref('');
const debugLogs = ref<string[]>([]);
const showDebug = ref(true);

// 录音时长
const recordingStartTime = ref<number>(0);
const recordingDuration = ref<number>(0);
let durationTimer: number | null = null;

// WebSocket 相关
let ws: WebSocket | null = null;
let audioContext: AudioContext | null = null;
let mediaStream: MediaStream | null = null;
let scriptProcessor: ScriptProcessorNode | null = null;
let heartbeatInterval: number | null = null;
let lastAudioSent: number = 0;
let audioReady = false;

// 计算属性
const statusTagType = computed(() => {
  if (connecting.value) return 'primary';
  if (isRecording.value) return 'warning';
  return 'info';
});

const statusText = computed(() => {
  if (connecting.value) return '连接中...';
  if (isRecording.value) return '录音中';
  return '待机';
});

// 工具函数
const addLog = (message: string) => {
  const timestamp = new Date().toLocaleTimeString();
  debugLogs.value.push(`[${timestamp}] ${message}`);
  if (debugLogs.value.length > 100) {
    debugLogs.value.shift();
  }
};

const clearLogs = () => {
  debugLogs.value = [];
};

const formatTime = (ms: number) => {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${minutes}:${secs.toString().padStart(2, '0')}`;
};

// 生成鉴权签名 (参照官方 demo)
const getSigna = (ts: number): string => {
  const md5Hash = CryptoJS.MD5(config.value.appId + ts).toString();
  const hmac = CryptoJS.HmacSHA1(md5Hash, config.value.apiKey);
  const base64 = CryptoJS.enc.Base64.stringify(hmac);
  return encodeURIComponent(base64);
};

// 构建 WebSocket URL (参照官方 demo)
const buildWsUrl = (): string => {
  const ts = Math.floor(Date.now() / 1000);
  const signa = getSigna(ts);
  const baseUrl = 'wss://rtasr.xfyun.cn/v1/ws';
  
  const url = `${baseUrl}?appid=${config.value.appId}&ts=${ts}&signa=${signa}` +
              `&pd=${config.value.domain}&lang=cn&punc=0`;
  
  addLog(`WebSocket URL: ${baseUrl}?appid=${config.value.appId}&ts=${ts}&signa=***&pd=${config.value.domain}`);
  return url;
};

// 开始录音
const startRecording = async () => {
  if (!config.value.apiKey) {
    ElMessage.warning('请先填写 API Key');
    return;
  }

  try {
    connecting.value = true;
    errorMessage.value = '';
    segments.value = [];
    currentTranscript.value = '';
    addLog('开始连接讯飞 ASR...');

    // 1. 获取麦克风权限
    addLog('请求麦克风权限...');
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        sampleRate: 16000,
      },
    });
    addLog('✅ 麦克风权限已获取');

    // 2. 初始化 AudioContext
    audioContext = new AudioContext({ sampleRate: 16000 });
    if (audioContext.state === 'suspended') {
      await audioContext.resume();
    }
    addLog('✅ AudioContext 已初始化');

    // 3. 连接 WebSocket
    addLog('建立 WebSocket 连接...');
    await connectWebSocket();

    // 4. 启动音频处理
    addLog('启动音频处理...');
    await startAudioProcessing();

    // 5. 更新状态
    isRecording.value = true;
    connecting.value = false;
    recordingStartTime.value = Date.now();
    durationTimer = window.setInterval(() => {
      recordingDuration.value = Math.floor((Date.now() - recordingStartTime.value) / 1000);
    }, 1000);

    addLog('✅ 录音已开始');
    ElMessage.success('录音已开始，请说话');

  } catch (err: any) {
    connecting.value = false;
    isRecording.value = false;
    const errorMsg = err instanceof Error ? err.message : '未知错误';
    errorMessage.value = `启动失败：${errorMsg}`;
    addLog(`❌ 启动失败：${errorMsg}`);
    ElMessage.error('启动失败：' + errorMsg);
    cleanupResources();
  }
};

// 连接 WebSocket (参照官方 demo)
const connectWebSocket = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    const url = buildWsUrl();
    ws = new WebSocket(url);

    ws.onopen = () => {
      addLog('✅ WebSocket 连接已建立');
      resolve();
    };

    ws.onerror = () => {
      const errorMsg = 'WebSocket 连接失败';
      addLog(`❌ ${errorMsg}`);
      reject(new Error(errorMsg));
    };

    ws.onmessage = (event) => {
      handleMessage(event.data);
    };

    ws.onclose = () => {
      addLog('WebSocket 连接已关闭');
      stopHeartbeat();
    };
  });
};

// 处理 WebSocket 消息 (参照官方 demo)
const handleMessage = (data: string) => {
  try {
    addLog(`收到消息：${data.substring(0, 100)}...`);
    const response = JSON.parse(data);
    const action = response.action;

    switch (action) {
      case 'error':
        addLog(`❌ 错误：code=${response.code}, desc=${response.desc}`);
        errorMessage.value = `讯飞错误：${response.desc} (code: ${response.code})`;
        stopRecording();
        break;

      case 'started':
        addLog(`✅ 连接成功，SID: ${response.sid}`);
        audioReady = true;
        break;

      case 'result':
        handleRecognitionResult(response.data);
        break;
    }
  } catch (err) {
    addLog(`解析消息失败：${err instanceof Error ? err.message : err}`);
  }
};

// 处理识别结果 (参照官方 demo 解析逻辑)
const handleRecognitionResult = (dataStr: string) => {
  try {
    const data = JSON.parse(dataStr);
    
    if (!data.cn?.st) {
      addLog('⚠️ 数据格式异常，缺少 cn.st 字段');
      return;
    }

    const st = data.cn.st;
    const isFinal = st.type === '0';

    // 解析转写文本 (参照官方 demo)
    let text = '';
    if (Array.isArray(st.rt)) {
      for (const rtItem of st.rt) {
        if (Array.isArray(rtItem.ws)) {
          for (const wsItem of rtItem.ws) {
            if (Array.isArray(wsItem.cw)) {
              for (const cwItem of wsItem.cw) {
                text += cwItem.w;
              }
            }
          }
        }
      }
    }

    if (text) {
      if (isFinal) {
        // 最终结果
        addLog(`✅ 最终结果：${text}`);
        segments.value.push({
          id: `seg_${Date.now()}`,
          text: text,
          timestamp: parseInt(st.bg) || Date.now(),
        });
        currentTranscript.value = '';
        ElMessage.success(`识别：${text}`);
      } else {
        // 中间结果
        addLog(`⏳ 中间结果：${text}`);
        currentTranscript.value = text;
      }
    }
  } catch (err) {
    addLog(`解析识别结果失败：${err instanceof Error ? err.message : err}`);
  }
};

// 启动音频处理
const startAudioProcessing = async () => {
  if (!audioContext || !mediaStream) {
    throw new Error('AudioContext 或 MediaStream 未初始化');
  }

  const source = audioContext.createMediaStreamSource(mediaStream);
  scriptProcessor = audioContext.createScriptProcessor(1024, 1, 1);

  let audioBuffer: Float32Array = new Float32Array(0);
  const samplesPer40ms = Math.floor(16000 * 0.04); // 640 采样点
  let frameCount = 0;

  scriptProcessor.onaudioprocess = (event) => {
    if (!audioReady) return;
    if (!ws || ws.readyState !== WebSocket.OPEN) return;

    const inputData = event.inputBuffer.getChannelData(0);
    
    // 累积音频数据
    const newBuffer = new Float32Array(audioBuffer.length + inputData.length);
    newBuffer.set(audioBuffer);
    newBuffer.set(inputData, audioBuffer.length);
    audioBuffer = newBuffer;

    // 每 40ms 发送一帧
    while (audioBuffer.length >= samplesPer40ms) {
      const chunk = audioBuffer.slice(0, samplesPer40ms);
      audioBuffer = audioBuffer.slice(samplesPer40ms);

      const pcmData = floatTo16BitPCM(chunk);
      const int8Array = new Int8Array(pcmData);

      if (ws.bufferedAmount < 16384) {
        ws.send(int8Array);
        lastAudioSent = Date.now();
        frameCount++;
        if (frameCount <= 3 || frameCount % 50 === 0) {
          addLog(`📤 已发送 ${frameCount} 帧音频`);
        }
      }
    }
  };

  source.connect(scriptProcessor);
  addLog('✅ 音频处理已启动');

  // 启动心跳
  startHeartbeat();
};

// 16 位 PCM 编码
const floatTo16BitPCM = (float32Array: Float32Array): ArrayBuffer => {
  const buffer = new ArrayBuffer(float32Array.length * 2);
  const view = new DataView(buffer);
  let offset = 0;

  for (let i = 0; i < float32Array.length; i++, offset += 2) {
    let s = Math.max(-1, Math.min(1, float32Array[i]));
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
  }

  return buffer;
};

// 心跳保活 (防止连接超时)
const startHeartbeat = () => {
  stopHeartbeat();
  
  heartbeatInterval = window.setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const now = Date.now();
      if (now - lastAudioSent > 10000) {
        // 10 秒无音频，发送静音帧
        const silentAudio = new Int8Array(320);
        ws.send(silentAudio);
        lastAudioSent = now;
        addLog('💓 发送心跳');
      }
    }
  }, 1000);
};

const stopHeartbeat = () => {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
    heartbeatInterval = null;
  }
};

// 停止录音
const stopRecording = async () => {
  addLog('停止录音...');

  // 停止定时器
  if (durationTimer) {
    clearInterval(durationTimer);
    durationTimer = null;
  }

  // 发送结束信号
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send('{"end": true}');
    addLog('📤 发送结束信号');
  }

  // 清理资源
  cleanupResources();

  // 更新状态
  isRecording.value = false;
  connecting.value = false;
  audioReady = false;
  recordingDuration.value = 0;

  addLog('✅ 录音已停止');
  ElMessage.success('录音已停止');
};

// 清理资源
const cleanupResources = () => {
  stopHeartbeat();

  if (scriptProcessor) {
    scriptProcessor.disconnect();
    scriptProcessor = null;
  }

  if (audioContext) {
    audioContext.close();
    audioContext = null;
  }

  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop());
    mediaStream = null;
  }

  if (ws) {
    ws.close();
    ws = null;
  }
};

// 页面卸载时清理
onUnmounted(() => {
  stopRecording();
});
</script>

<style scoped>
.xunfei-voice-test {
  height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 60px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.page-main {
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.config-card,
.control-card,
.result-card,
.debug-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.control-panel {
  display: flex;
  gap: 16px;
  align-items: center;
}

.control-panel .el-button {
  min-width: 140px;
  height: 46px;
  font-size: 16px;
}

.empty-result {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.transcript-display {
  max-height: 400px;
  overflow-y: auto;
}

.transcript-segment {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 4px solid #409EFF;
}

.transcript-segment.current {
  border-left-color: #E6A23C;
  background: #fdf6ec;
}

.segment-index {
  font-size: 14px;
  color: #909399;
  font-weight: 600;
  min-width: 30px;
}

.segment-text {
  flex: 1;
  line-height: 1.6;
  color: #303133;
  font-size: 15px;
}

.segment-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.debug-card {
  background: #1e1e1e;
}

.debug-card .card-header {
  color: #d4d4d4;
}

.debug-logs {
  max-height: 300px;
  overflow-y: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #2d2d2d;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 4px;
}

.log-item {
  margin-bottom: 4px;
  word-break: break-all;
}

.log-item:first-child {
  color: #4ec9b0;
}
</style>
