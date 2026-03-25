<template>
  <div class="voice-to-soap-page">
    <el-header class="page-header">
      <h2 class="page-title">
        <el-icon :size="24"><headset /></el-icon>
        语音问诊 → SOAP 病历测试（端到端）
      </h2>
    </el-header>

    <el-main class="page-main">
      <!-- 步骤指示器 -->
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="语音录入" description="点击开始录音，对着麦克风说话" />
        <el-step title="语音转写" description="实时显示转写文字" />
        <el-step title="病历生成" description="调用 LLM 生成 SOAP 病历" />
      </el-steps>

      <el-row :gutter="20" style="margin-top: 24px;">
        <!-- 左侧：语音控制 -->
        <el-col :span="12">
          <el-card class="voice-card">
            <template #header>
              <div class="card-header">
                <el-icon><microphone /></el-icon>
                <span>语音录入</span>
              </div>
            </template>

            <div class="voice-controls">
              <el-button
                v-if="!isRecording"
                type="primary"
                size="large"
                :loading="isConnecting"
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
                停止录音 ({{ recordingDuration }}s)
              </el-button>

              <el-tag :type="statusTagType" effect="dark">
                {{ statusText }}
              </el-tag>
            </div>

            <!-- 转写文本 -->
            <div class="transcript-section">
              <div class="transcript-header">
                <span class="transcript-title">
                  <el-icon><document-copy /></el-icon>
                  转写文本
                </span>
                <el-tag size="small">{{ segments.length }} 段</el-tag>
              </div>
              
              <div class="transcript-content">
                <div
                  v-for="segment in segments"
                  :key="segment.id"
                  class="transcript-segment"
                >
                  <span class="segment-text">{{ segment.text }}</span>
                  <span class="segment-time">{{ formatTime(segment.timestamp) }}</span>
                </div>
              </div>
            </div>

            <!-- 生成病历按钮 -->
            <el-button
              type="success"
              size="large"
              :disabled="segments.length === 0 || isRecording"
              @click="generateSoapNote"
              :loading="isGenerating"
            >
              <el-icon :size="20"><magic-stick /></el-icon>
              生成 SOAP 病历
            </el-button>
          </el-card>
        </el-col>

        <!-- 右侧：SOAP 病历展示 -->
        <el-col :span="12">
          <el-card class="soap-card">
            <template #header>
              <div class="card-header">
                <el-icon><folder-opened /></el-icon>
                <span>SOAP 病历</span>
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="toggleEditMode"
                  v-if="soapNote"
                >
                  <el-icon><edit /></el-icon>
                  {{ isEditMode ? '查看' : '编辑' }}
                </el-button>
              </div>
            </template>

            <div v-if="!soapNote" class="empty-soap">
              <el-empty description='点击"生成 SOAP 病历"按钮生成病历' />
            </div>

            <div v-else class="soap-content">
              <!-- 查看模式 -->
              <el-descriptions :column="1" border v-if="!isEditMode">
                <el-descriptions-item label="S - 主观资料">
                  <div class="soap-section">
                    <strong>主诉:</strong> {{ soapNote.subjective.chiefComplaint }}
                  </div>
                  <div class="soap-section">
                    <strong>现病史:</strong> {{ soapNote.subjective.historyOfPresentIllness }}
                  </div>
                </el-descriptions-item>

                <el-descriptions-item label="O - 客观资料">
                  <div class="soap-section">
                    <strong>生命体征:</strong> {{ formatVitalSigns(soapNote.objective.vitalSigns) }}
                  </div>
                  <div class="soap-section">
                    <strong>体格检查:</strong> {{ soapNote.objective.physicalExamFindings }}
                  </div>
                </el-descriptions-item>

                <el-descriptions-item label="A - 评估">
                  <div class="soap-section">
                    <strong>初步诊断:</strong> {{ soapNote.assessment.primaryDiagnosis }}
                  </div>
                  <div class="soap-section">
                    <strong>鉴别诊断:</strong>
                    <el-tag
                      v-for="(diag, index) in soapNote.assessment.differentialDiagnoses"
                      :key="index"
                      size="small"
                      style="margin-right: 4px;"
                    >
                      {{ diag }}
                    </el-tag>
                  </div>
                </el-descriptions-item>

                <el-descriptions-item label="P - 计划">
                  <div class="soap-section">
                    <strong>检查检验:</strong>
                    <div v-if="soapNote.plan.diagnosticTests.length > 0">
                      <el-tag
                        v-for="(test, index) in soapNote.plan.diagnosticTests"
                        :key="index"
                        type="info"
                        size="small"
                        style="margin-right: 4px;"
                      >
                        {{ test }}
                      </el-tag>
                    </div>
                    <span v-else>无</span>
                  </div>
                  <div class="soap-section">
                    <strong>治疗方案:</strong> {{ soapNote.plan.treatment }}
                  </div>
                  <div class="soap-section">
                    <strong>医嘱:</strong> {{ soapNote.plan.advice }}
                  </div>
                </el-descriptions-item>
              </el-descriptions>

              <!-- 编辑模式 -->
              <el-form v-else :model="soapNote" label-position="top" size="small">
                <el-divider content-position="left">S - 主观资料</el-divider>
                <el-form-item label="主诉">
                  <el-input 
                    v-model="soapNote.subjective.chiefComplaint" 
                    type="textarea" 
                    :rows="2"
                    placeholder="如：头痛、发热 3 天"
                  />
                </el-form-item>
                <el-form-item label="现病史">
                  <el-input 
                    v-model="soapNote.subjective.historyOfPresentIllness" 
                    type="textarea" 
                    :rows="4"
                    placeholder="详细描述病情发展过程"
                  />
                </el-form-item>

                <el-divider content-position="left">O - 客观资料</el-divider>
                <el-form-item label="生命体征">
                  <el-input 
                    v-model="soapNote.objective.vitalSignsString" 
                    placeholder="如：体温：36.5°C，脉搏：80 次/分"
                  />
                </el-form-item>
                <el-form-item label="体格检查">
                  <el-input 
                    v-model="soapNote.objective.physicalExamFindings" 
                    type="textarea" 
                    :rows="3"
                    placeholder="描述体格检查发现"
                  />
                </el-form-item>

                <el-divider content-position="left">A - 评估</el-divider>
                <el-form-item label="初步诊断">
                  <el-input 
                    v-model="soapNote.assessment.primaryDiagnosis" 
                    placeholder="如：急性上呼吸道感染"
                  />
                </el-form-item>
                <el-form-item label="鉴别诊断">
                  <el-select 
                    v-model="soapNote.assessment.differentialDiagnoses" 
                    multiple 
                    allow-create
                    filterable
                    default-first-option
                    placeholder="输入或选择鉴别诊断"
                    style="width: 100%"
                  >
                    <el-option label="流行性感冒" value="流行性感冒" />
                    <el-option label="急性支气管炎" value="急性支气管炎" />
                    <el-option label="新冠肺炎" value="新冠肺炎" />
                    <el-option label="急性胃肠炎" value="急性胃肠炎" />
                  </el-select>
                </el-form-item>

                <el-divider content-position="left">P - 计划</el-divider>
                <el-form-item label="检查检验">
                  <el-select 
                    v-model="soapNote.plan.diagnosticTests" 
                    multiple 
                    allow-create
                    filterable
                    default-first-option
                    placeholder="输入或选择检查项目"
                    style="width: 100%"
                  >
                    <el-option label="血常规" value="血常规" />
                    <el-option label="C 反应蛋白" value="C 反应蛋白" />
                    <el-option label="尿常规" value="尿常规" />
                    <el-option label="胸部 X 线" value="胸部 X 线" />
                  </el-select>
                </el-form-item>
                <el-form-item label="治疗方案">
                  <el-input 
                    v-model="soapNote.plan.treatment" 
                    type="textarea" 
                    :rows="3"
                    placeholder="药物治疗、非药物治疗等"
                  />
                </el-form-item>
                <el-form-item label="医嘱">
                  <el-input 
                    v-model="soapNote.plan.advice" 
                    type="textarea" 
                    :rows="2"
                    placeholder="休息、饮食、复诊等建议"
                  />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="saveSoapNote">保存修改</el-button>
                  <el-button @click="toggleEditMode">取消</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 调试日志 -->
      <el-card class="debug-card" v-if="showDebug">
        <template #header>
          <div class="card-header">
            <el-icon><info-filled /></el-icon>
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

      <div class="debug-toggle">
        <el-button size="small" @click="showDebug = !showDebug">
          {{ showDebug ? '隐藏调试' : '显示调试' }}
        </el-button>
      </div>
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  Microphone, VideoPause, DocumentCopy, Headset, 
  MagicStick, FolderOpened, InfoFilled, Edit 
} from '@element-plus/icons-vue';
import { useMedicalSpeechRecognition } from '@/composables/useMedicalSpeechRecognition';
import type { SOAPNote } from '@/types/voice';

// 状态
const currentStep = ref(0);
const isRecording = ref(false);
const isConnecting = ref(false);
const isGenerating = ref(false);
const recordingDuration = ref(0);
const segments = ref<Array<{ id: string; text: string; timestamp: number }>>([]);
const soapNote = ref<SOAPNote | null>(null);
const showDebug = ref(false);
const debugLogs = ref<string[]>([]);

// 使用语音识别 Composable
const speechRecognition = useMedicalSpeechRecognition({
  appId: import.meta.env.VITE_IFLYTEK_APP_ID || '8bc8b3ca',
  apiKey: import.meta.env.VITE_IFLYTEK_API_KEY || 'ef71686251f3f7b42bbb56c3d737f938',
  domain: 'medical',
  onTranscriptUpdate: (text, isFinal) => {
    if (isFinal) {
      addLog(`✅ 最终结果：${text}`);
      // 添加到 segments 列表
      segments.value.push({
        id: `seg_${Date.now()}`,
        text: text,
        timestamp: Date.now()
      });
    } else {
      addLog(`⏳ 中间结果：${text}`);
    }
  }
});

const { transcript, status, start, stop } = speechRecognition;

// 编辑模式相关
const isEditMode = ref(false);
const originalSoapNote = ref<SOAPNote | null>(null);

// 切换编辑模式
const toggleEditMode = () => {
  if (isEditMode.value) {
    // 退出编辑模式，不保存
    isEditMode.value = false;
  } else {
    // 进入编辑模式，保存原始数据
    originalSoapNote.value = JSON.parse(JSON.stringify(soapNote.value));
    // 添加 vitalSignsString 字段用于编辑
    if (soapNote.value) {
      (soapNote.value as any).vitalSignsString = formatVitalSigns(soapNote.value.objective.vitalSigns);
    }
    isEditMode.value = true;
  }
};

// 保存修改
const saveSoapNote = () => {
  if (!soapNote.value) return;
  
  // 解析 vitalSignsString 回 vitalSigns 对象
  const vitalSignsStr = (soapNote.value as any).vitalSignsString || '';
  soapNote.value.objective.vitalSigns = parseVitalSignsText(vitalSignsStr);
  
  isEditMode.value = false;
  addLog('💾 病历已保存');
  ElMessage.success('病历已保存');
};

// 解析生命体征文本为对象
const parseVitalSignsText = (text: string): Record<string, any> => {
  if (!text || text.trim() === '') {
    return {};
  }
  const vitals: Record<string, any> = {};
  const parts = text.split(/[,,]/);
  parts.forEach(part => {
    const [key, value] = part.split(':');
    if (key && value) {
      vitals[key.trim()] = value.trim();
    }
  });
  return vitals;
};

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

const formatVitalSigns = (vitals: Record<string, any> | string): string => {
  if (!vitals) {
    return '未记录';
  }
  if (typeof vitals === 'string') {
    return vitals || '待测量';
  }
  if (Object.keys(vitals).length === 0) {
    return '未记录';
  }
  return Object.entries(vitals)
    .map(([key, value]) => `${key}: ${value}`)
    .join(', ');
};

// 状态计算
const statusText = computed(() => {
  switch (status.value) {
    case 'recording':
      return '录音中';
    case 'processing':
      return '处理中';
    case 'stopped':
      return '已停止';
    default:
      return '就绪';
  }
});

const statusTagType = computed(() => {
  switch (status.value) {
    case 'recording':
      return 'success';
    case 'processing':
      return 'warning';
    case 'stopped':
      return 'info';
    default:
      return 'info';
  }
});

// 开始录音
const startRecording = async () => {
  try {
    isConnecting.value = true;
    currentStep.value = 0;
    addLog('开始录音...');
    
    await start();
    
    isRecording.value = true;
    currentStep.value = 1;
    isConnecting.value = false;
    
    // 开始计时
    const startTime = Date.now();
    const durationTimer = setInterval(() => {
      recordingDuration.value = Math.floor((Date.now() - startTime) / 1000);
    }, 1000);
    
    ElMessage.success('录音已开始');
    
  } catch (error) {
    addLog(`录音失败：${error instanceof Error ? error.message : '未知错误'}`);
    ElMessage.error('录音失败');
    isConnecting.value = false;
  }
};

// 停止录音
const stopRecording = async () => {
  try {
    addLog('停止录音...');
    await stop();
    isRecording.value = false;
    recordingDuration.value = 0;
    currentStep.value = 2;
    ElMessage.success('录音已停止');
  } catch (error) {
    addLog(`停止失败：${error instanceof Error ? error.message : '未知错误'}`);
    ElMessage.error('停止失败');
  }
};

// 生成 SOAP 病历
const generateSoapNote = async () => {
  if (transcript.value.length === 0) {
    ElMessage.warning('没有转写文本，无法生成病历');
    return;
  }

  try {
    isGenerating.value = true;
    currentStep.value = 2;
    addLog('开始生成 SOAP 病历...');
    addLog(`转写文本长度：${transcript.value.length} 字符`);

    // 调用后端 API
    const response = await fetch('http://localhost:8080/api/test/voice/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        transcript: transcript.value,
        department: 'general'
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const result = await response.json();
    
    // 检查响应是否成功 (code 可能是 200 或 0)
    if ((result.code === 0 || result.code === 200) && result.data) {
      soapNote.value = {
        subjective: {
          chiefComplaint: result.data.soap.subjective.chiefComplaint,
          historyOfPresentIllness: result.data.soap.subjective.historyOfPresentIllness
        },
        objective: {
          vitalSigns: typeof result.data.soap.objective.vitalSigns === 'string' 
            ? parseVitalSigns(result.data.soap.objective.vitalSigns)
            : result.data.soap.objective.vitalSigns,
          physicalExamFindings: result.data.soap.objective.physicalExamFindings
        },
        assessment: {
          primaryDiagnosis: result.data.soap.assessment.primaryDiagnosis,
          differentialDiagnoses: result.data.soap.assessment.differentialDiagnoses
        },
        plan: {
          diagnosticTests: result.data.soap.plan.diagnosticTests,
          treatment: result.data.soap.plan.treatment,
          advice: result.data.soap.plan.advice
        }
      };

      addLog('✅ SOAP 病历生成成功');
      ElMessage.success('病历生成成功');
    } else {
      throw new Error(result.message || '生成失败');
    }

  } catch (error) {
    addLog(`生成失败：${error instanceof Error ? error.message : '未知错误'}`);
    ElMessage.error('病历生成失败');
  } finally {
    isGenerating.value = false;
  }
};

// 解析生命体征字符串
const parseVitalSigns = (text: string): Record<string, any> => {
  if (!text || text === '待测量') {
    return {};
  }
  // 简单解析，例如："体温：36.5°C，脉搏：80 次/分"
  const vitals: Record<string, any> = {};
  const parts = text.split(/[,,]/);
  parts.forEach(part => {
    const [key, value] = part.split(':');
    if (key && value) {
      vitals[key.trim()] = value.trim();
    }
  });
  return vitals;
};

// 编辑病历
const editSoap = () => {
  ElMessageBox.alert('病历编辑功能开发中...', '提示', {
    confirmButtonText: '确定'
  });
};
</script>

<style scoped>
.voice-to-soap-page {
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

.voice-card,
.soap-card {
  height: 100%;
  min-height: 600px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.voice-controls {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 20px;
}

.transcript-section {
  margin: 16px 0;
}

.transcript-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.transcript-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.transcript-content {
  max-height: 300px;
  overflow-y: auto;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.transcript-segment {
  display: flex;
  justify-content: space-between;
  padding: 8px;
  margin-bottom: 8px;
  background: #fff;
  border-radius: 4px;
  border-left: 3px solid #409EFF;
}

.segment-text {
  flex: 1;
  line-height: 1.6;
}

.segment-time {
  font-size: 12px;
  color: #909399;
  margin-left: 12px;
}

.soap-content {
  max-height: 600px;
  overflow-y: auto;
}

.soap-section {
  margin-bottom: 12px;
  line-height: 1.6;
}

.soap-section strong {
  color: #606266;
  margin-right: 8px;
}

.empty-soap {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.debug-card {
  margin-top: 16px;
}

.debug-logs {
  max-height: 200px;
  overflow-y: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 4px;
}

.log-item {
  margin-bottom: 4px;
}

.debug-toggle {
  margin-top: 16px;
  text-align: center;
}
</style>
