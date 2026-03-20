<template>
  <div class="voice-test-container">
    <h2>语音转写测试</h2>
    
    <div class="control-panel">
      <button 
        @click="toggleRecording" 
        :class="['record-button', { 'recording': isRecording }]"
        :disabled="isLoading"
      >
        {{ isRecording ? '停止录音' : '开始录音' }}
      </button>
      
      <div v-if="isLoading" class="loading">
        加载中...
      </div>
    </div>

    <div class="status-info">
      <p>状态: <span :class="statusClass">{{ status }}</span></p>
      <p v-if="sid">SID: {{ sid }}</p>
    </div>

    <div class="transcript-area">
      <h3>转写结果:</h3>
      <div class="transcript-display">
        <p v-if="!transcripts.length" class="placeholder">等待语音转写结果...</p>
        <div 
          v-for="(transcript, index) in transcripts" 
          :key="index" 
          :class="['transcript-item', { 'final': transcript.isFinal, 'intermediate': !transcript.isFinal }]"
        >
          <span class="timestamp">[{{ formatDate(transcript.timestamp) }}]</span>
          <span class="text">{{ transcript.text }}</span>
          <span class="type">{{ transcript.isFinal ? '(最终)' : '(中间)' }}</span>
        </div>
      </div>
    </div>

    <div class="debug-info" v-if="showDebug">
      <h3>调试信息:</h3>
      <pre>{{ debugInfo }}</pre>
    </div>

    <button @click="toggleDebug" class="debug-toggle">
      {{ showDebug ? '隐藏调试' : '显示调试' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { IFlytekService } from '../services/speech/IFlytekService';

// 从环境变量或配置获取API密钥
const appId = import.meta.env.VITE_IFLYTEK_APP_ID || '8bc8b3ca';
const apiKey = import.meta.env.VITE_IFLYTEK_API_KEY || 'your_api_key_here';

// 状态管理
const isRecording = ref(false);
const isLoading = ref(false);
const status = ref('就绪');
const sid = ref('');
const transcripts = ref<Array<{text: string, isFinal: boolean, timestamp: Date}>>([]);
const showDebug = ref(false);
const debugInfo = ref('');

// 服务实例
let service: IFlytekService | null = null;

// 状态样式
const statusClass = computed(() => {
  return {
    'status-ready': status.value === '就绪',
    'status-recording': status.value === '录音中',
    'status-connecting': status.value.includes('连接'),
    'status-error': status.value.includes('错误')
  };
});

// 格式化时间
const formatDate = (date: Date) => {
  return date.toLocaleTimeString();
};

// 切换录音状态
const toggleRecording = async () => {
  if (isRecording.value) {
    await stopRecording();
  } else {
    await startRecording();
  }
};

// 开始录音
const startRecording = async () => {
  try {
    isLoading.value = true;
    status.value = '初始化中...';
    
    // 创建服务实例
    service = new IFlytekService({
      appId,
      apiKey,
      domain: 'medical',
      continuousTimeout: 3000
    });

    // 设置事件处理器
    service.onResult = (result) => {
      console.log('收到转写结果:', result);
      transcripts.value.push({
        text: result.text,
        isFinal: result.isFinal,
        timestamp: new Date()
      });
      
      // 滚动到底部
      setTimeout(() => {
        const transcriptDiv = document.querySelector('.transcript-display');
        if (transcriptDiv) {
          transcriptDiv.scrollTop = transcriptDiv.scrollHeight;
        }
      }, 100);
    };

    service.onError = (error) => {
      console.error('语音识别错误:', error);
      status.value = `错误: ${error}`;
    };

    service.onStatusChange = (newStatus) => {
      console.log('状态变化:', newStatus);
      switch (newStatus) {
        case 'connecting':
          status.value = '正在连接...';
          break;
        case 'recording':
          status.value = '录音中';
          isRecording.value = true;
          break;
        case 'stopped':
          status.value = '已停止';
          isRecording.value = false;
          break;
        default:
          status.value = newStatus;
      }
    };

    // 开始录音
    await service.start();
    sid.value = service.sid;
    status.value = '录音中';
    
  } catch (error) {
    console.error('开始录音失败:', error);
    status.value = `录音失败: ${error instanceof Error ? error.message : '未知错误'}`;
    isRecording.value = false;
  } finally {
    isLoading.value = false;
  }
};

// 停止录音
const stopRecording = async () => {
  try {
    isLoading.value = true;
    status.value = '停止中...';
    
    if (service) {
      await service.stop();
      service = null;
    }
    
    status.value = '已停止';
    isRecording.value = false;
  } catch (error) {
    console.error('停止录音失败:', error);
    status.value = `停止失败: ${error instanceof Error ? error.message : '未知错误'}`;
  } finally {
    isLoading.value = false;
  }
};

// 切换调试信息显示
const toggleDebug = () => {
  showDebug.value = !showDebug.value;
  if (showDebug.value) {
    // 更新调试信息
    debugInfo.value = JSON.stringify({
      appId,
      apiKey: apiKey ? '已配置' : '未配置',
      isRecording: isRecording.value,
      status: status.value,
      transcriptCount: transcripts.value.length
    }, null, 2);
  }
};

// 页面卸载时清理
onUnmounted(() => {
  if (service) {
    service.stop().catch(console.error);
  }
});
</script>

<style scoped>
.voice-test-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  font-family: Arial, sans-serif;
}

.control-panel {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-bottom: 20px;
}

.record-button {
  padding: 12px 24px;
  font-size: 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.record-button:not(:disabled) {
  background-color: #007bff;
  color: white;
}

.record-button.recording {
  background-color: #dc3545;
  animation: pulse 1.5s infinite;
}

.record-button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

.loading {
  color: #007bff;
  font-style: italic;
}

.status-info {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.status-ready { color: #28a745; }
.status-recording { color: #dc3545; }
.status-connecting { color: #ffc107; }
.status-error { color: #dc3545; }

.transcript-area {
  margin-bottom: 20px;
}

.transcript-display {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 10px;
  background-color: #f8f9fa;
}

.placeholder {
  color: #6c757d;
  font-style: italic;
  text-align: center;
  margin-top: 100px;
}

.transcript-item {
  padding: 8px;
  margin: 5px 0;
  border-radius: 4px;
  border-left: 3px solid #007bff;
}

.transcript-item.final {
  border-left-color: #28a745;
  background-color: rgba(40, 167, 69, 0.1);
}

.transcript-item.intermediate {
  border-left-color: #17a2b8;
  background-color: rgba(23, 162, 184, 0.1);
}

.timestamp {
  color: #6c757d;
  font-size: 0.8em;
  margin-right: 8px;
}

.type {
  float: right;
  font-size: 0.8em;
  color: #6c757d;
}

.debug-info {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #e9ecef;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.debug-toggle {
  padding: 8px 16px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>