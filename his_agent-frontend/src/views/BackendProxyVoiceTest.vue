<template>
  <div class="voice-test-page">
    <el-header class="page-header">
      <h2 class="page-title">
        <el-icon :size="24"><headset /></el-icon>
        语音转写测试 (后端代理模式)
      </h2>
    </el-header>

    <el-main class="page-main">
      <!-- 状态面板 -->
      <el-card class="status-card">
        <div class="status-panel">
          <el-tag :type="connectionStatus === 'connected' ? 'success' : 'info'">
            {{ connectionStatus === 'connected' ? '● 已连接' : '● 未连接' }}
          </el-tag>
          <span class="session-id" v-if="sessionId">SID: {{ sessionId }}</span>
        </div>
      </el-card>

      <!-- 控制按钮 -->
      <el-card class="control-card">
        <!-- 未连接时显示连接按钮 -->
        <el-button
          v-if="connectionStatus === 'disconnected'"
          type="primary"
          size="large"
          :loading="isConnecting"
          @click="connectToBackend"
        >
          <el-icon :size="20"><link /></el-icon>
          {{ isConnecting ? '连接中...' : '连接后端' }}
        </el-button>
        
        <!-- 已连接但未录音时显示开始按钮 -->
        <el-button
          v-else-if="!isRecording"
          type="success"
          size="large"
          @click="startRecording"
        >
          <el-icon :size="20"><microphone /></el-icon>
          开始录音
        </el-button>
        
        <!-- 录音中显示停止按钮 -->
        <el-button
          v-else
          type="danger"
          size="large"
          @click="stopRecording"
        >
          <el-icon :size="20"><video-pause /></el-icon>
          停止录音 ({{ recordingDuration }}s)
        </el-button>
      </el-card>

      <!-- 转写结果 -->
      <el-card class="result-card">
        <template #header>
          <div class="card-header">
            <el-icon><document-copy /></el-icon>
            <span>转写结果</span>
            <el-tag v-if="results.length > 0" type="success" size="small">
              {{ results.length }} 条
            </el-tag>
          </div>
        </template>

        <div v-if="results.length === 0" class="empty-result">
          <el-empty description="等待语音输入..." />
        </div>

        <div v-else class="result-list">
          <div
            v-for="(item, index) in results"
            :key="index"
            :class="['result-item', item.type]"
          >
            <el-tag :type="item.type === 'final' ? 'success' : 'warning'" size="small">
              {{ item.type === 'final' ? '最终' : '中间' }}
            </el-tag>
            <span class="result-text">{{ item.text }}</span>
            <span class="result-time">{{ formatTime(item.timestamp) }}</span>
          </div>
        </div>
      </el-card>

      <!-- 调试信息 -->
      <el-card class="debug-card" v-if="showDebug">
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

      <el-button size="small" @click="showDebug = !showDebug">
        {{ showDebug ? '隐藏调试' : '显示调试' }}
      </el-button>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Microphone, VideoPause, DocumentCopy, Headset, Bug, Link } from '@element-plus/icons-vue';

// 状态
const isRecording = ref(false);
const isConnecting = ref(false);
const connectionStatus = ref('disconnected');
const sessionId = ref('');
const recordingDuration = ref(0);
const results = ref<Array<{ text: string; type: 'intermediate' | 'final'; timestamp: number }>>([]);
const showDebug = ref(false);
const debugLogs = ref<string[]>([]);

// WebSocket 和音频
let ws: WebSocket | null = null;
let mediaStream: MediaStream | null = null;
let audioContext: AudioContext | null = null;
let scriptProcessor: ScriptProcessorNode | null = null;
let durationTimer: number | null = null;
let audioSendInterval: number | null = null;

// 工具函数
const addLog = (message: string) => {
  const time = new Date().toLocaleTimeString();
  debugLogs.value.push(`[${time}] ${message}`);
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

// 连接到后端
const connectToBackend = async () => {
  try {
    isConnecting.value = true;
    addLog('开始连接后端 WebSocket...');

    const wsUrl = `ws://localhost:8080/ws/voice`;
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      addLog('✅ WebSocket 已连接');
      connectionStatus.value = 'connected';
      isConnecting.value = false;
      ElMessage.success('已连接到后端');
    };

    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      addLog(`收到消息：${JSON.stringify(msg)}`);

      if (msg.type === 'ready') {
        sessionId.value = msg.sessionId;
        addLog(`✅ 会话就绪，SID: ${sessionId.value}`);
      } else if (msg.type === 'result') {
        if (isRecording.value) {
          results.value.push({
            text: msg.text,
            type: 'intermediate',
            timestamp: Date.now()
          });
          addLog(`转写结果：${msg.text}`);
        }
      } else if (msg.type === 'error') {
        ElMessage.error(`错误：${msg.message}`);
        addLog(`❌ 错误：${msg.message}`);
        disconnect();
      }
    };

    ws.onerror = () => {
      addLog('❌ WebSocket 错误');
      ElMessage.error('连接失败');
      isConnecting.value = false;
      connectionStatus.value = 'disconnected';
    };

    ws.onclose = () => {
      addLog('🔌 WebSocket 已关闭');
      connectionStatus.value = 'disconnected';
      isRecording.value = false;
      if (durationTimer) {
        clearInterval(durationTimer);
        durationTimer = null;
      }
    };

  } catch (error) {
    addLog(`连接失败：${error instanceof Error ? error.message : '未知错误'}`);
    ElMessage.error('连接失败');
    isConnecting.value = false;
  }
};

// 开始录音
const startRecording = async () => {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    ElMessage.warning('请先连接后端');
    return;
  }
  
  try {
    await startAudioCapture();
  } catch (error) {
    addLog(`录音失败：${error instanceof Error ? error.message : '未知错误'}`);
    ElMessage.error('录音失败');
  }
};

// 开始音频采集
const startAudioCapture = async () => {
  try {
    // 获取麦克风权限
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        sampleRate: 16000,
      },
    });
    addLog('✅ 麦克风权限已获取');

    audioContext = new AudioContext({ sampleRate: 16000 });
    const source = audioContext.createMediaStreamSource(mediaStream!);
    
    // 使用 ScriptProcessor（虽然已废弃，但兼容性好）
    scriptProcessor = audioContext.createScriptProcessor(4096, 1, 1);
    
    let audioBuffer = new Float32Array(0);
    const samplesPer40ms = Math.floor(16000 * 0.04);

    scriptProcessor.onaudioprocess = (event) => {
      if (!ws || ws.readyState !== WebSocket.OPEN) return;

      const inputData = event.inputBuffer.getChannelData(0);
      const newBuffer = new Float32Array(audioBuffer.length + inputData.length);
      newBuffer.set(audioBuffer);
      newBuffer.set(inputData, audioBuffer.length);
      audioBuffer = newBuffer;

      // 每 40ms 发送一次
      while (audioBuffer.length >= samplesPer40ms) {
        const chunk = audioBuffer.slice(0, samplesPer40ms);
        audioBuffer = audioBuffer.slice(samplesPer40ms);

        // 转换为 16 位 PCM
        const pcmData = floatTo16BitPCM(chunk);
        const base64 = arrayBufferToBase64(pcmData);

        // 发送音频数据
        ws.send(JSON.stringify({
          type: 'audio',
          data: base64
        }));
      }
    };

    source.connect(scriptProcessor);
    scriptProcessor.connect(audioContext.destination);
    
    addLog('🎤 音频采集已启动');
    isRecording.value = true;
    isConnecting.value = false;

    // 开始计时
    const startTime = Date.now();
    durationTimer = window.setInterval(() => {
      recordingDuration.value = Math.floor((Date.now() - startTime) / 1000);
    }, 1000);

    ElMessage.success('录音已开始');

  } catch (error) {
    addLog(`音频采集失败：${error instanceof Error ? error.message : '未知错误'}`);
    ElMessage.error('音频采集失败');
    isConnecting.value = false;
  }
};

// Float32 转 16 位 PCM
const floatTo16BitPCM = (input: Float32Array): ArrayBuffer => {
  const buffer = new ArrayBuffer(input.length * 2);
  const view = new DataView(buffer);
  let offset = 0;

  for (let i = 0; i < input.length; i++, offset += 2) {
    let s = Math.max(-1, Math.min(1, input[i]));
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
  }

  return buffer;
};

// ArrayBuffer 转 Base64
const arrayBufferToBase64 = (buffer: ArrayBuffer): string => {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
};

// 断开连接
const disconnect = () => {
  addLog('断开连接...');
  
  if (durationTimer) {
    clearInterval(durationTimer);
    durationTimer = null;
  }

  // 清理音频资源
  if (scriptProcessor) {
    scriptProcessor.disconnect();
    scriptProcessor = null;
  }

  if (audioContext) {
    audioContext.close().catch(console.error);
    audioContext = null;
  }

  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop());
    mediaStream = null;
  }

  // 关闭 WebSocket
  if (ws) {
    ws.close();
    ws = null;
  }

  connectionStatus.value = 'disconnected';
  isRecording.value = false;
  recordingDuration.value = 0;
  sessionId.value = '';
  addLog('✅ 已断开连接');
};

// 停止录音
const stopRecording = async () => {
  addLog('停止录音...');

  if (durationTimer) {
    clearInterval(durationTimer);
    durationTimer = null;
  }

  // 发送结束信号
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'end' }));
    addLog('📤 已发送结束信号');
  }

  // 清理音频资源
  if (scriptProcessor) {
    scriptProcessor.disconnect();
    scriptProcessor = null;
  }

  if (audioContext) {
    await audioContext.close();
    audioContext = null;
  }

  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop());
    mediaStream = null;
  }

  isRecording.value = false;
  recordingDuration.value = 0;
  addLog('✅ 录音已停止');
  ElMessage.success('录音已停止');
};

// 页面卸载清理
onUnmounted(() => {
  stopRecording();
});
</script>

<style scoped>
.voice-test-page {
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
}

.page-main {
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.status-card,
.control-card,
.result-card,
.debug-card {
  margin-bottom: 16px;
}

.status-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 16px;
}

.session-id {
  color: #909399;
  font-family: monospace;
}

.control-card .el-button {
  width: 100%;
  height: 46px;
  font-size: 16px;
}

.empty-result {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-list {
  max-height: 400px;
  overflow-y: auto;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 4px solid #409EFF;
}

.result-item.intermediate {
  border-left-color: #E6A23C;
  background: #fdf6ec;
}

.result-item.final {
  border-left-color: #67C23A;
  background: #f0f9ff;
}

.result-text {
  flex: 1;
  line-height: 1.6;
  color: #303133;
  font-size: 15px;
}

.result-time {
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
</style>
