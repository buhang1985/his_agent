<template>
  <div class="voice-consultation-page">
    <!-- 顶部状态栏 -->
    <el-header class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon :size="24"><microphone /></el-icon>
          智能语音病历录入系统
        </h2>
        <el-tag v-if="patientInfo" type="info" size="large">
          <el-icon><user /></el-icon>
          {{ patientInfo.name }} | {{ patientInfo.gender }} | {{ patientInfo.age }}岁
        </el-tag>
      </div>
      <div class="header-right">
        <el-tag :type="statusTagType" size="large" effect="dark">
          {{ statusText }}
        </el-tag>
      </div>
    </el-header>

    <el-main class="page-main">
      <el-row :gutter="20">
        <!-- 左侧：语音控制和转写 -->
        <el-col :span="10" class="left-panel">
          <!-- 语音控制面板 -->
          <el-card class="voice-control-card">
            <template #header>
              <div class="card-header">
                <el-icon><headset /></el-icon>
                <span>语音录入</span>
              </div>
            </template>
            
            <div class="voice-controls">
              <el-button
                v-if="!isRecording"
                type="primary"
                size="large"
                class="record-btn"
                :loading="connecting"
                @click="handleStartRecording"
              >
                <el-icon :size="20"><microphone /></el-icon>
                开始录音
              </el-button>
              
              <el-button
                v-else
                type="danger"
                size="large"
                class="record-btn recording"
                @click="handleStopRecording"
              >
                <el-icon :size="20"><video-pause /></el-icon>
                结束录音 ({{ recordingDuration }}s)
              </el-button>
              
              <el-button
                type="success"
                size="large"
                :disabled="!transcript"
                @click="handleGenerateRecord"
              >
                <el-icon :size="20"><document /></el-icon>
                生成病历
              </el-button>
            </div>

              <!-- 转写文本展示 -->
              <div v-if="transcript || isRecording" class="transcript-section">
                <div class="transcript-header">
                  <span class="transcript-title">
                    <el-icon><document-copy /></el-icon>
                    语音转写
                  </span>
                  <el-tag size="small">{{ segments.length }} 段对话</el-tag>
                </div>
                
                <div class="transcript-content">
                  <div
                    v-for="segment in segments"
                    :key="segment.id"
                    class="transcript-segment"
                    :class="segment.speaker"
                  >
                    <el-tag :type="segment.speaker === 'doctor' ? 'primary' : 'info'" size="small">
                      {{ segment.speaker === 'doctor' ? '医生' : '患者' }}
                    </el-tag>
                    <span class="segment-text">{{ segment.text }}</span>
                    <span class="segment-time">{{ formatTime(segment.timestamp) }}</span>
                  </div>
                  <!-- 显示实时转写内容 -->
                  <div v-if="isRecording && transcript" class="transcript-segment doctor">
                    <el-tag type="primary" size="small">医生</el-tag>
                    <span class="segment-text realtime">{{ transcript }}</span>
                  </div>
                </div>
              </div>

            <!-- 错误提示 -->
            <el-alert
              v-if="error"
              :title="error"
              type="error"
              show-icon
              :closable="false"
            />
          </el-card>

          <!-- 调试日志 -->
          <el-card v-if="debugLogs.length > 0" class="debug-card">
            <template #header>
              <div class="card-header">
                <el-icon><document-copy /></el-icon>
                <span>调试日志</span>
              </div>
            </template>
            <div class="debug-logs">
              <div v-for="(log, index) in debugLogs" :key="index" class="log-item">
                {{ log }}
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：病历编辑 -->
        <el-col :span="14" class="right-panel">
          <el-card class="record-editor-card">
            <template #header>
              <div class="editor-header">
                <div class="header-title">
                  <el-icon><edit /></el-icon>
                  <span>病历编辑</span>
                </div>
                <div class="header-actions">
                  <el-button
                    type="success"
                    size="large"
                    :loading="writebackAllLoading"
                    :disabled="!hasContent"
                    @click="handleWritebackAll"
                  >
                    <el-icon><upload /></el-icon>
                    全部回写 HIS
                  </el-button>
                  <el-button @click="handleClearAll">
                    <el-icon><delete /></el-icon>
                    清空全部
                  </el-button>
                </div>
              </div>
            </template>

            <el-form label-position="top">
              <!-- 主诉 -->
              <RecordSection
                ref="chiefComplaintRef"
                v-model="medicalRecord.chiefComplaint.content"
                title="主诉"
                icon="Edit"
                placeholder="患者主要症状及持续时间..."
                :required="true"
                @writeback="handleSectionWriteback"
              />

              <!-- 现病史 -->
              <RecordSection
                ref="hpiRef"
                v-model="medicalRecord.historyOfPresentIllness.onset"
                title="现病史"
                icon="Document"
                placeholder="起病情况、主要症状、伴随症状、诊疗经过..."
                :required="true"
                @writeback="handleSectionWriteback"
              />

              <!-- 既往史 -->
              <RecordSection
                ref="pmhRef"
                v-model="medicalRecord.pastMedicalHistory.pastDiseases"
                title="既往史"
                icon="Files"
                placeholder="既往疾病史、手术史、过敏史、用药史..."
                @writeback="handleSectionWriteback"
              />

              <!-- 体格检查 -->
              <RecordSection
                ref="peRef"
                v-model="medicalRecord.physicalExamination.generalExam"
                title="体格检查"
                icon="Document"
                placeholder="生命体征、一般情况、系统查体..."
                :required="true"
                @writeback="handleSectionWriteback"
              />

              <!-- 辅助检查 -->
              <RecordSection
                ref="aeRef"
                v-model="medicalRecord.auxiliaryExamination.results"
                title="辅助检查"
                icon="Document"
                placeholder="实验室检查、影像学检查、其他检查结果..."
                @writeback="handleSectionWriteback"
              />

              <!-- 初步诊断 -->
              <RecordSection
                ref="pdRef"
                v-model="medicalRecord.preliminaryDiagnosis.primaryDiagnosis"
                title="初步诊断"
                icon="CircleCheck"
                placeholder="初步诊断意见..."
                :required="true"
                @writeback="handleSectionWriteback"
              />

              <!-- 诊疗计划 -->
              <RecordSection
                ref="tpRef"
                v-model="treatmentPlanText"
                title="诊疗计划"
                icon="Setting"
                placeholder="进一步检查、药物治疗、非药物治疗、随访建议..."
                :required="true"
                @writeback="handleTreatmentPlanWriteback"
              />
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </el-main>

    <!-- HIS 回写结果对话框 -->
    <el-dialog
      v-model="writebackDialogVisible"
      title="HIS 回写结果"
      width="500px"
    >
      <el-result
        :icon="writebackSuccess ? 'success' : 'error'"
        :title="writebackSuccess ? '回写成功' : '回写失败'"
        :sub-title="writebackMessage"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Microphone,
  VideoPause,
  Document,
  DocumentCopy,
  Edit,
  Upload,
  Delete,
  User,
  Headset,
} from '@element-plus/icons-vue';
import { useMedicalSpeechRecognition } from '@/composables/useMedicalSpeechRecognition';
import type { MedicalRecord } from '@/types/medical-record';
import RecordSection from '@/components/RecordSection.vue';
import axios from 'axios';

// 患者信息（从 HIS 获取）
const patientInfo = ref<{
  name: string;
  gender: string;
  age: number;
} | null>(null);

// 语音识别
const appId = import.meta.env.VITE_IFLYTEK_APP_ID;
const apiKey = import.meta.env.VITE_IFLYTEK_API_KEY;

const {
  transcript,
  segments,
  status,
  error,
  start,
  stop,
} = useMedicalSpeechRecognition({
  appId,
  apiKey,
  domain: 'medical',
  continuousTimeout: 2000,
  onTranscriptUpdate: (text, isFinal) => {
    if (isFinal) {
      addLog(`识别结果：${text}`);
    }
  },
});

const isRecording = computed(() => status.value === 'recording');
const connecting = computed(() => status.value === 'processing');
const debugLogs = ref<string[]>([]);
const recordingStartTime = ref<number>(0);
const recordingDuration = ref<number>(0);
let durationTimer: number | null = null;

// 病历数据
const medicalRecord = ref<MedicalRecord>({
  chiefComplaint: { content: '', duration: '' },
  historyOfPresentIllness: { onset: '', symptoms: '', progression: '', treatment: '' },
  pastMedicalHistory: { pastDiseases: '', surgeries: '', allergies: '', medications: '' },
  physicalExamination: {
    vitalSigns: {},
    generalExam: '',
    systemExams: {},
  },
  auxiliaryExamination: {
    labTests: [],
    imagingTests: [],
    otherTests: [],
    results: '',
  },
  preliminaryDiagnosis: { primaryDiagnosis: '', icdCode: '', confidence: 0 },
  differentialDiagnoses: [],
  treatmentPlan: {
    furtherTests: [],
    medications: [],
    nonDrugTreatment: '',
    followup: '',
    advice: '',
  },
  isCompleted: false,
  isSigned: false,
});

// 回写状态
const writebackAllLoading = ref(false);
const writebackDialogVisible = ref(false);
const writebackSuccess = ref(false);
const writebackMessage = ref('');

// 计算属性
const statusTagType = computed(() => {
  const map: Record<string, any> = {
    idle: 'info',
    recording: 'warning',
    processing: 'primary',
    review: 'success',
    stopped: 'info',
  };
  return map[status.value] || 'info';
});

const statusText = computed(() => {
  const map: Record<string, string> = {
    idle: '待机',
    recording: '录音中',
    processing: '处理中',
    review: '审阅中',
    stopped: '已停止',
  };
  return map[status.value] || '未知';
});

const hasContent = computed(() => {
  return medicalRecord.value.chiefComplaint.content ||
    medicalRecord.value.historyOfPresentIllness.onset ||
    medicalRecord.value.physicalExamination.generalExam ||
    medicalRecord.value.preliminaryDiagnosis.primaryDiagnosis;
});

// 诊疗计划文本（用于 v-model 双向绑定）
const treatmentPlanText = computed({
  get: () => medicalRecord.value.treatmentPlan.medications.join('; '),
  set: (value: string) => {
    medicalRecord.value.treatmentPlan.medications = value.split(';').map(s => s.trim()).filter(s => s);
  }
});

// 方法
const addLog = (message: string) => {
  const timestamp = new Date().toLocaleTimeString();
  debugLogs.value.push(`[${timestamp}] ${message}`);
  if (debugLogs.value.length > 50) {
    debugLogs.value.shift();
  }
};

const formatTime = (ms: number) => {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${minutes}:${secs.toString().padStart(2, '0')}`;
};

const handleStartRecording = async () => {
  addLog('开始录音...');
  try {
    await start();
    recordingStartTime.value = Date.now();
    durationTimer = window.setInterval(() => {
      recordingDuration.value = Math.floor((Date.now() - recordingStartTime.value) / 1000);
    }, 1000);
    addLog('录音已开始');
  } catch (err) {
    addLog(`录音失败：${err instanceof Error ? err.message : '未知错误'}`);
    ElMessage.error('录音失败：' + (err instanceof Error ? err.message : '未知错误'));
  }
};

const handleStopRecording = async () => {
  addLog('结束录音');
  if (durationTimer) {
    clearInterval(durationTimer);
    durationTimer = null;
  }
  await stop();
  recordingDuration.value = 0;
  addLog('录音已停止');
};

const handleGenerateRecord = async () => {
  if (!transcript.value) {
    ElMessage.warning('没有转写文本，无法生成病历');
    return;
  }

  addLog('正在生成病历...');
  
  try {
    const response = await axios.post('/api/test/voice/generate', {
      transcript: transcript.value,
    });

    const data = response.data.data;
    
    // 填充病历数据
    if (data.soap) {
      medicalRecord.value.chiefComplaint.content = data.soap.subjective?.chiefComplaint || '';
      medicalRecord.value.historyOfPresentIllness.onset = data.soap.subjective?.historyOfPresentIllness || '';
      medicalRecord.value.physicalExamination.generalExam = data.soap.objective?.physicalExamFindings || '';
      medicalRecord.value.preliminaryDiagnosis.primaryDiagnosis = data.soap.assessment?.primaryDiagnosis || '';
      
      if (data.soap.plan?.treatment) {
        medicalRecord.value.treatmentPlan.medications = [data.soap.plan.treatment];
      }
      
      addLog('病历生成成功');
      ElMessage.success('病历生成成功');
    }
  } catch (err: any) {
    console.error('生成病历失败:', err);
    addLog(`生成失败：${err.response?.data?.message || err.message}`);
    ElMessage.error('生成病历失败：' + (err.response?.data?.message || err.message));
  }
};

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有病历内容吗？', '提示', {
      type: 'warning',
    });
    
    medicalRecord.value = {
      chiefComplaint: { content: '', duration: '' },
      historyOfPresentIllness: { onset: '', symptoms: '', progression: '', treatment: '' },
      pastMedicalHistory: { pastDiseases: '', surgeries: '', allergies: '', medications: '' },
      physicalExamination: { vitalSigns: {}, generalExam: '', systemExams: {} },
      auxiliaryExamination: { labTests: [], imagingTests: [], otherTests: [], results: '' },
      preliminaryDiagnosis: { primaryDiagnosis: '', icdCode: '', confidence: 0 },
      differentialDiagnoses: [],
      treatmentPlan: { furtherTests: [], medications: [], nonDrugTreatment: '', followup: '', advice: '' },
      isCompleted: false,
      isSigned: false,
    };
    
    addLog('已清空全部病历');
    ElMessage.success('已清空全部病历');
  } catch {
    // 取消操作
  }
};

const handleSectionWriteback = async (section: string, _content: string) => {
  addLog(`回写 ${section} 到 HIS...`);
  
  try {
    // TODO: 调用 HIS 接口回写
    await new Promise(resolve => setTimeout(resolve, 1000)); // 模拟
    
    writebackSuccess.value = true;
    writebackMessage.value = `${section} 已成功回写到 HIS`;
    writebackDialogVisible.value = true;
    addLog(`${section} 回写成功`);
  } catch (err: any) {
    writebackSuccess.value = false;
    writebackMessage.value = `${section} 回写失败：${err.message}`;
    writebackDialogVisible.value = true;
    addLog(`${section} 回写失败`);
  }
};

const handleTreatmentPlanWriteback = async (_content: string) => {
  await handleSectionWriteback('诊疗计划', _content);
};

const handleWritebackAll = async () => {
  if (!hasContent.value) {
    ElMessage.warning('病历内容为空，无法回写');
    return;
  }

  writebackAllLoading.value = true;
  addLog('开始全部回写到 HIS...');

  try {
    // TODO: 调用 HIS 接口批量回写
    await new Promise(resolve => setTimeout(resolve, 2000)); // 模拟
    
    writebackSuccess.value = true;
    writebackMessage.value = '病历已全部回写到 HIS';
    writebackDialogVisible.value = true;
    addLog('全部回写成功');
    ElMessage.success('病历已全部回写到 HIS');
  } catch (err: any) {
    writebackSuccess.value = false;
    writebackMessage.value = `回写失败：${err.message}`;
    writebackDialogVisible.value = true;
    addLog(`全部回写失败`);
    ElMessage.error('回写失败：' + err.message);
  } finally {
    writebackAllLoading.value = false;
  }
};

onUnmounted(() => {
  if (durationTimer) {
    clearInterval(durationTimer);
  }
});
</script>

<style scoped>
.voice-consultation-page {
  height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
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

.left-panel,
.right-panel {
  height: 100%;
  overflow-y: auto;
}

.voice-control-card,
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

.voice-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.record-btn {
  flex: 1;
  height: 50px;
  font-size: 16px;
}

.record-btn.recording {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.transcript-section {
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;
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
  font-weight: 600;
  color: #303133;
}

.transcript-content {
  max-height: 300px;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px;
}

.transcript-segment {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
}

.transcript-segment.doctor {
  border-left: 3px solid #409EFF;
}

.transcript-segment.patient {
  border-left: 3px solid #67C23A;
}

.segment-text {
  flex: 1;
  line-height: 1.6;
  color: #303133;
}

.segment-text.realtime {
  font-style: italic;
  color: #909399;
}

.segment-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
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
  word-break: break-all;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
