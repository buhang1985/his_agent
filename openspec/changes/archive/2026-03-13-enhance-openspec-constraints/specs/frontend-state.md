# 前端状态管理规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：Pinia Store 模块划分

必须按业务域划分 Pinia Store 模块，保持状态管理清晰。

#### 场景：Store 模块结构
- **当** 定义 Store 模块时
- **那么** 必须按以下结构组织：
```typescript
// stores/index.ts
export { useUserStore } from './user';
export { useConsultationStore } from './consultation';
export { usePatientStore } from './patient';
export { useSpeechStore } from './speech';
export { useLLMStore } from './llm';
export { useUIStore } from './ui';

// stores/user.ts - 用户认证状态
export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(null);
  const userInfo = ref<UserInfo | null>(null);
  const permissions = ref<string[]>([]);
  
  // Getters
  const isAuthenticated = computed(() => !!token.value);
  const hasPermission = (perm: string) => permissions.value.includes(perm);
  
  // Actions
  async function login(credentials: LoginCredentials) {
    const response = await api.login(credentials);
    token.value = response.token;
    userInfo.value = response.userInfo;
  }
  
  function logout() {
    token.value = null;
    userInfo.value = null;
    permissions.value = [];
  }
  
  return { token, userInfo, permissions, isAuthenticated, hasPermission, login, logout };
});
```

#### 场景：问诊会话 Store
- **当** 管理问诊会话状态时
- **那么** 必须：
```typescript
// stores/consultation.ts
export const useConsultationStore = defineStore('consultation', () => {
  // State
  const session = ref<ConsultationSession | null>(null);
  const status = ref<'idle' | 'recording' | 'processing' | 'review' | 'completed'>('idle');
  const segments = ref<ConversationSegment[]>([]);
  const soapNote = ref<SOAPNote | null>(null);
  const diagnosisSuggestions = ref<DiagnosisSuggestion[]>([]);
  const error = ref<string | null>(null);
  
  // Getters
  const isRecording = computed(() => status.value === 'recording');
  const isProcessing = computed(() => status.value === 'processing');
  const fullTranscript = computed(() => 
    segments.value.map(s => s.text).join('\n')
  );
  
  // Actions
  function startSession(patientId: string) {
    session.value = {
      id: uuidv4(),
      patientId,
      status: 'idle',
      startTime: Date.now(),
      segments: [],
      fullTranscript: '',
    };
  }
  
  function addSegment(segment: ConversationSegment) {
    segments.value.push(segment);
  }
  
  function setSoapNote(note: SOAPNote) {
    soapNote.value = note;
    status.value = 'review';
  }
  
  function reset() {
    session.value = null;
    status.value = 'idle';
    segments.value = [];
    soapNote.value = null;
    diagnosisSuggestions.value = [];
    error.value = null;
  }
  
  return {
    session, status, segments, soapNote, diagnosisSuggestions, error,
    isRecording, isProcessing, fullTranscript,
    startSession, addSegment, setSoapNote, reset
  };
});
```

#### 场景：语音识别 Store
- **当** 管理语音识别状态时
- **那么** 必须：
```typescript
// stores/speech.ts
export const useSpeechStore = defineStore('speech', () => {
  // State
  const isRecording = ref(false);
  const isRecognizing = ref(false);
  const provider = ref<'iflytek' | 'aliyun' | 'whisper'>('iflytek');
  const error = ref<string | null>(null);
  
  // Getters
  const canRecord = computed(() => !isRecognizing.value && !error.value);
  
  // Actions
  async function startRecording() {
    // Request microphone permission
    // Start recording
    isRecording.value = true;
  }
  
  async function stopRecording() {
    isRecording.value = false;
  }
  
  return { isRecording, isRecognizing, provider, error, canRecord, startRecording, stopRecording };
});
```

### 需求：跨组件数据流规范

必须规范跨组件数据流动，避免状态混乱。

#### 场景：父子组件通信
- **当** 父子组件通信时
- **那么** 必须：
```vue
<!-- 父传子：使用 props -->
<template>
  <PatientInfo :patient="currentPatient" :loading="isLoading" />
</template>

<!-- 子组件定义 -->
<script setup lang="ts">
const props = defineProps<{
  patient: Patient | null;
  loading: boolean;
}>();
</script>
```

#### 场景：子传父通信
- **当** 子组件向父组件通信时
- **那么** 必须：
```vue
<!-- 子组件 emit -->
<script setup lang="ts">
const emit = defineEmits<{
  (e: 'save', data: SOAPNote): void;
  (e: 'cancel'): void;
}>();

function onSave() {
  emit('save', soapNote.value);
}
</script>

<!-- 父组件监听 -->
<template>
  <SOAPNoteEditor 
    @save="handleSave" 
    @cancel="handleCancel" 
  />
</template>
```

#### 场景：跨层级通信
- **当** 跨层级组件通信时
- **那么** 必须使用 provide/inject：
```vue
<!-- 祖先组件 provide -->
<script setup lang="ts">
const consultationId = ref<string | null>(null);
provide('consultationId', consultationId);
</script>

<!-- 后代组件 inject -->
<script setup lang="ts">
const consultationId = inject('consultationId');
</script>
```

#### 场景：全局状态访问
- **当** 访问全局状态时
- **那么** 必须：
```typescript
// 在组件内使用 Store
<script setup lang="ts">
const userStore = useUserStore();
const consultationStore = useConsultationStore();

// 计算属性跨 Store
const canEditConsultation = computed(() => 
  userStore.hasPermission('CONSULTATION_EDIT') &&
  consultationStore.status === 'review'
);
</script>
```

### 需求：状态持久化

必须持久化关键状态，支持页面刷新后恢复。

#### 场景：持久化配置
- **当** 配置状态持久化时
- **那么** 必须：
```typescript
// plugins/pinia-persist.ts
import { createPersistedState } from 'pinia-plugin-persistedstate';

export const persistPlugin = createPersistedState({
  storage: localStorage,
  serializer: {
    serialize: JSON.stringify,
    deserialize: JSON.parse,
  },
  key: (storeId) => `his_agent:${storeId}`,
});

// stores/user.ts - 持久化用户 token
export const useUserStore = defineStore('user', () => {
  // ...
}, {
  persist: {
    paths: ['token', 'userInfo'],  // 只持久化必要字段
    storage: localStorage,
  },
});
```

#### 场景：敏感数据保护
- **当** 持久化敏感数据时
- **那么** 必须：
  - 禁止持久化患者完整信息
  - 禁止持久化密码和完整 token
  - 使用加密存储（如需要）
  - 设置合理的过期时间
