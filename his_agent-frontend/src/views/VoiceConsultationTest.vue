<template>
  <div class="voice-consultation-test">
    <el-container>
      <el-header>
        <h2>🎤 语音录入测试 - 讯飞 ASR</h2>
      </el-header>

      <el-main>
        <!-- 状态栏 -->
        <el-alert
          v-if="status === 'recording'"
          title="正在录音..."
          type="warning"
          show-icon
          :closable="false"
        >
          <template #default>
            <span>会话 ID: {{ sid }}</span>
          </template>
        </el-alert>

        <el-alert
          v-if="error"
          :title="error"
          type="error"
          show-icon
          @close="error = null"
        />

        <!-- 调试日志 -->
        <el-alert
          v-if="debugLogs.length > 0"
          title="调试日志"
          type="info"
          :closable="false"
        >
          <div class="debug-logs">
            <div v-for="(log, index) in debugLogs" :key="index" class="log-item">
              {{ log }}
            </div>
          </div>
        </el-alert>

        <!-- 控制按钮 -->
        <div class="control-panel">
          <el-button
            v-if="status === 'idle' || status === 'stopped'"
            type="primary"
            size="large"
            @click="handleStart"
            :loading="status === 'processing'"
          >
            🎤 开始录音
          </el-button>

          <el-button
            v-if="status === 'recording'"
            type="danger"
            size="large"
            @click="handleStop"
          >
            ⏹ 结束录音
          </el-button>

          <el-button
            v-if="transcript && status !== 'recording'"
            type="success"
            size="large"
            @click="handleGenerate"
            :loading="generating"
          >
            ✨ 生成病历
          </el-button>
        </div>

        <!-- 转写文本展示 -->
        <el-card v-if="transcript" class="transcript-card">
          <template #header>
            <div class="card-header">
              <span>📝 转写文本</span>
              <el-tag size="small">{{ segments.length }} 段</el-tag>
            </div>
          </template>
          <div class="transcript-content">
            <div v-for="segment in segments" :key="segment.id" class="segment">
              <el-tag size="small" :type="segment.speaker === 'doctor' ? 'primary' : 'info'">
                {{ segment.speaker === 'doctor' ? '医生' : '患者' }}
              </el-tag>
              <span class="segment-text">{{ segment.text }}</span>
            </div>
          </div>
        </el-card>

        <!-- 病历展示 -->
        <el-card v-if="soapNote" class="soap-card">
          <template #header>
            <div class="card-header">
              <span>📋 SOAP 病历</span>
              <el-tag v-if="soapNote" size="small" type="success">已生成</el-tag>
            </div>
          </template>

          <el-form :model="soapNote" label-position="top">
            <el-tabs>
              <el-tab-pane label="S - 主观资料">
                <el-form-item label="主诉">
                  <el-input
                    v-model="soapNote.subjective.chiefComplaint"
                    type="textarea"
                    :rows="2"
                  />
                </el-form-item>
                <el-form-item label="现病史">
                  <el-input
                    v-model="soapNote.subjective.historyOfPresentIllness"
                    type="textarea"
                    :rows="4"
                  />
                </el-form-item>
              </el-tab-pane>

              <el-tab-pane label="O - 客观资料">
                <el-form-item label="体格检查">
                  <el-input
                    v-model="soapNote.objective.physicalExamFindings"
                    type="textarea"
                    :rows="4"
                  />
                </el-form-item>
              </el-tab-pane>

              <el-tab-pane label="A - 评估">
                <el-form-item label="初步诊断">
                  <el-input v-model="soapNote.assessment.primaryDiagnosis" />
                </el-form-item>
                <el-form-item label="鉴别诊断">
                  <el-select v-model="soapNote.assessment.differentialDiagnoses" multiple>
                    <el-option
                      v-for="d in soapNote.assessment.differentialDiagnoses"
                      :key="d"
                      :label="d"
                      :value="d"
                    />
                  </el-select>
                </el-form-item>
              </el-tab-pane>

              <el-tab-pane label="P - 计划">
                <el-form-item label="检查检验">
                  <el-select
                    v-model="soapNote.plan.diagnosticTests"
                    multiple
                    allow-create
                    filterable
                  />
                </el-form-item>
                <el-form-item label="治疗方案">
                  <el-input v-model="soapNote.plan.treatment" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item label="健康建议">
                  <el-input v-model="soapNote.plan.advice" type="textarea" :rows="2" />
                </el-form-item>
              </el-tab-pane>
            </el-tabs>
          </el-form>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useMedicalSpeechRecognition } from '@/composables/useMedicalSpeechRecognition';
import type { SOAPNote } from '@/types/voice';
import axios from 'axios';

const appId = import.meta.env.VITE_IFLYTEK_APP_ID;
const apiKey = import.meta.env.VITE_IFLYTEK_API_KEY;

const { transcript, segments, status, error, sid, start, stop } =
  useMedicalSpeechRecognition({
    appId,
    apiKey,
    domain: 'medical',
  });

const generating = ref(false);
const soapNote = ref<SOAPNote | null>(null);
const debugLogs = ref<string[]>([]);

const addLog = (message: string) => {
  const timestamp = new Date().toLocaleTimeString();
  debugLogs.value.push(`[${timestamp}] ${message}`);
  if (debugLogs.value.length > 20) {
    debugLogs.value.shift();
  }
};

const handleStart = async () => {
  soapNote.value = null;
  debugLogs.value = [];
  addLog('开始录音...');
  try {
    await start();
    addLog('录音已开始');
  } catch (err) {
    addLog(`录音失败：${err instanceof Error ? err.message : '未知错误'}`);
  }
};

const handleStop = async () => {
  addLog('结束录音');
  await stop();
  addLog('录音已停止');
};

const handleGenerate = async () => {
  if (!transcript.value) {
    addLog('没有转写文本，无法生成病历');
    return;
  }

  generating.value = true;
  addLog('正在生成病历...');

  try {
    const response = await axios.post('/api/test/voice/generate', {
      transcript: transcript.value,
    });

    soapNote.value = response.data.data.soap;
    addLog('病历生成成功');
    console.log('生成的病历:', soapNote.value);
  } catch (err: any) {
    console.error('生成病历失败:', err);
    addLog(`生成失败：${err.response?.data?.message || err.message}`);
  } finally {
    generating.value = false;
  }
};
</script>

<style scoped>
.voice-consultation-test {
  height: 100vh;
}

.el-header {
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
}

.control-panel {
  margin: 20px 0;
  display: flex;
  gap: 10px;
}

.transcript-card,
.soap-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.transcript-content {
  max-height: 400px;
  overflow-y: auto;
}

.segment {
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.segment-text {
  flex: 1;
}

.debug-logs {
  max-height: 200px;
  overflow-y: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.log-item {
  margin-bottom: 4px;
  word-break: break-all;
}
</style>
