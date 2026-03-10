# 智能问诊功能实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现智能问诊核心功能，支持语音录入、实时转写、SOAP 病历生成和诊断建议。

**Architecture:** 采用组件化设计，VoiceConsultation 为主容器，通过自定义 hooks 管理状态，services 层封装语音识别和 LLM 服务，Zustand store 管理全局状态。

**Tech Stack:** React 18+, TypeScript 5+, Vercel AI SDK 4+, Zustand 5+, Deepgram SDK

---

## 文件结构

### 新建文件

```
src/
├── components/
│   └── VoiceConsultation/
│       ├── VoiceConsultation.tsx       # 主容器组件
│       ├── VoiceInput.tsx              # 语音录入控件
│       ├── TranscriptView.tsx          # 转写文本展示
│       ├── SOAPNoteEditor.tsx          # SOAP 病历编辑器
│       ├── DiagnosisPanel.tsx          # 诊断建议面板
│       └── PatientInfoBanner.tsx       # 患者信息栏
├── hooks/
│   ├── useVoiceConsultation.ts         # 问诊状态管理
│   ├── useMedicalSpeechRecognition.ts  # 语音识别 hook
│   └── useSOAPNoteGenerator.ts         # 病历生成 hook
├── services/
│   ├── speech/
│   │   ├── DeepgramService.ts          # Deepgram 服务
│   │   └── WhisperService.ts           # Whisper 服务
│   └── llm/
│       ├── SOAPNoteGenerator.ts        # SOAP 生成服务
│       └── DiagnosisSuggester.ts       # 诊断建议服务
├── stores/
│   └── consultationStore.ts            # Zustand 状态存储
└── types/
    └── consultation.ts                 # 问诊相关类型（补充 medical.ts）
```

### 修改文件

```
src/types/medical.ts - 补充缺失类型
package.json - 添加新依赖
```

---

## Chunk 1: 基础架构

### Task 1: 安装依赖

**Files:**
- Modify: `package.json`

- [ ] **Step 1: 添加依赖**

```bash
cd /home/yuzihao/workspace/his_agent
npm install @deepgram/sdk zod
npm install -D @types/node
```

- [ ] **Step 2: 验证安装**

```bash
npm list @deepgram/sdk zod
```

Expected: 显示已安装的版本号

- [ ] **Step 3: 提交**

```bash
git add package.json package-lock.json
git commit -m "chore: add deepgram and zod dependencies"
```

---

### Task 2: 创建问诊类型定义

**Files:**
- Create: `src/types/consultation.ts`

- [ ] **Step 1: 创建类型文件**

```typescript
// src/types/consultation.ts
import { SOAPNote, DiagnosisSuggestion, ConversationSegment } from './medical';

// 问诊会话状态
export type ConsultationStatus = 'idle' | 'recording' | 'processing' | 'review' | 'completed';

// 问诊会话
export interface ConsultationSession {
  id: string;
  patientId: string;
  status: ConsultationStatus;
  startTime: number;
  endTime?: number;
  
  // 语音转写
  segments: ConversationSegment[];
  fullTranscript: string;
  
  // 生成的病历
  soapNote?: SOAPNote;
  
  // 诊断建议
  diagnosisSuggestions?: DiagnosisSuggestion[];
  
  // 错误状态
  error: string | null;
}

// 语音识别配置
export interface SpeechRecognitionConfig {
  provider: 'deepgram' | 'whisper';
  language: 'zh-CN';
  medicalTerms: string[];
  realTime: boolean;
}

// LLM 配置
export interface LLMConfig {
  provider: 'qwen' | 'claude' | 'ollama';
  model: string;
  temperature: number;
  maxTokens: number;
}

// 语音识别状态
export interface SpeechRecognitionState {
  isListening: boolean;
  isProcessing: boolean;
  error: string | null;
  segments: ConversationSegment[];
  fullTranscript: string;
}
```

- [ ] **Step 2: 提交**

```bash
git add src/types/consultation.ts
git commit -m "feat(types): add consultation type definitions"
```

---

### Task 3: 创建 Zustand 状态存储

**Files:**
- Create: `src/stores/consultationStore.ts`

- [ ] **Step 1: 创建存储**

```typescript
// src/stores/consultationStore.ts
import { create } from 'zustand';
import { ConsultationSession, ConsultationStatus } from '../types/consultation';
import { ConversationSegment } from '../types/medical';

interface ConsultationStore {
  // 当前会话
  session: ConsultationSession | null;
  
  // 动作
  startSession: (patientId: string) => void;
  addSegment: (segment: ConversationSegment) => void;
  updateTranscript: (transcript: string) => void;
  setSoapNote: (soapNote: SOAPNote) => void;
  setDiagnosisSuggestions: (suggestions: DiagnosisSuggestion[]) => void;
  setStatus: (status: ConsultationStatus) => void;
  setError: (error: string | null) => void;
  resetSession: () => void;
}

const createInitialSession = (patientId: string): ConsultationSession => ({
  id: crypto.randomUUID(),
  patientId,
  status: 'idle',
  startTime: Date.now(),
  segments: [],
  fullTranscript: '',
  error: null,
});

export const useConsultationStore = create<ConsultationStore>((set) => ({
  session: null,
  
  startSession: (patientId) =>
    set({ session: createInitialSession(patientId) }),
  
  addSegment: (segment) =>
    set((state) => ({
      session: state.session
        ? {
            ...state.session,
            segments: [...state.session.segments, segment],
          }
        : null,
    })),
  
  updateTranscript: (transcript) =>
    set((state) => ({
      session: state.session
        ? { ...state.session, fullTranscript: transcript }
        : null,
    })),
  
  setSoapNote: (soapNote) =>
    set((state) => ({
      session: state.session ? { ...state.session, soapNote } : null,
    })),
  
  setDiagnosisSuggestions: (suggestions) =>
    set((state) => ({
      session: state.session
        ? { ...state.session, diagnosisSuggestions: suggestions }
        : null,
    })),
  
  setStatus: (status) =>
    set((state) => ({
      session: state.session ? { ...state.session, status } : null,
    })),
  
  setError: (error) =>
    set((state) => ({
      session: state.session ? { ...state.session, error } : null,
    })),
  
  resetSession: () => set({ session: null }),
}));
```

- [ ] **Step 2: 提交**

```bash
git add src/stores/consultationStore.ts
git commit -m "feat(store): create consultation zustand store"
```

---

## Chunk 2: 语音识别服务

### Task 4: Deepgram 语音识别服务

**Files:**
- Create: `src/services/speech/DeepgramService.ts`

- [ ] **Step 1: 创建 Deepgram 服务**

```typescript
// src/services/speech/DeepgramService.ts
import { createClient, LiveTranscriptionEvents } from '@deepgram/sdk';
import { ConversationSegment } from '../../types/medical';

export interface DeepgramConfig {
  apiKey: string;
  model?: string;
  language?: string;
}

export class DeepgramService {
  private apiKey: string;
  private ws: WebSocket | null = null;
  private mediaRecorder: MediaRecorder | null = null;

  constructor(config: DeepgramConfig) {
    this.apiKey = config.apiKey;
  }

  async startStreaming(
    onTranscript: (text: string, isFinal: boolean) => void,
    onSegment?: (segment: ConversationSegment) => void
  ): Promise<void> {
    // 创建 WebSocket 连接到 Deepgram
    const wsUrl = new URL('wss://api.deepgram.com/v1/listen');
    wsUrl.searchParams.set('model', 'nova-3-medical');
    wsUrl.searchParams.set('language', 'zh-CN');
    wsUrl.searchParams.set('punctuate', 'true');
    wsUrl.searchParams.set('interim_results', 'true');
    wsUrl.searchParams.set('smart_format', 'true');
    wsUrl.searchParams.set('diarize', 'true');

    this.ws = new WebSocket(wsUrl.toString(), ['token', this.apiKey]);

    return new Promise((resolve, reject) => {
      this.ws!.onopen = () => {
        console.log('Deepgram connection established');
        this.startAudioCapture();
        resolve();
      };

      this.ws!.onmessage = (event) => {
        const data = JSON.parse(event.data);
        
        if (data.channel?.alternatives?.[0]) {
          const transcript = data.channel.alternatives[0].transcript;
          const isFinal = data.is_final;
          
          if (transcript.trim()) {
            onTranscript(transcript, isFinal);
            
            if (onSegment && isFinal) {
              onSegment({
                id: crypto.randomUUID(),
                speaker: 'doctor', // TODO: 通过声纹区分
                text: transcript,
                timestamp: Date.now(),
                confidence: data.channel.alternatives[0].confidence,
              });
            }
          }
        }
      };

      this.ws!.onerror = (error) => {
        reject(new Error('Deepgram connection error'));
      };

      this.ws!.onclose = () => {
        console.log('Deepgram connection closed');
      };
    });
  }

  private startAudioCapture() {
    navigator.mediaDevices.getUserMedia({ 
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      } 
    }).then(stream => {
      this.mediaRecorder = new MediaRecorder(stream, {
        mimeType: 'audio/webm;codecs=opus',
        audioBitsPerSecond: 128000,
      });

      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0 && this.ws?.readyState === WebSocket.OPEN) {
          this.ws.send(event.data);
        }
      };

      this.mediaRecorder.start(250); // 每 250ms 发送一次
    });
  }

  stopStreaming(): void {
    this.mediaRecorder?.stop();
    this.mediaRecorder?.stream.getTracks().forEach(track => track.stop());
    this.ws?.close();
    this.ws = null;
    this.mediaRecorder = null;
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/services/speech/DeepgramService.ts
git commit -m "feat(service): add Deepgram speech recognition service"
```

---

### Task 5: Whisper 本地识别服务（备用）

**Files:**
- Create: `src/services/speech/WhisperService.ts`

- [ ] **Step 1: 创建 Whisper 服务**

```typescript
// src/services/speech/WhisperService.ts
import { ConversationSegment } from '../../types/medical';

export interface WhisperConfig {
  modelPath?: string;
  language?: string;
}

export class WhisperService {
  private config: WhisperConfig;

  constructor(config: WhisperConfig = {}) {
    this.config = {
      modelPath: config.modelPath || '/models/whisper-tiny-zh.bin',
      language: config.language || 'zh',
    };
  }

  async transcribe(audioBlob: Blob): Promise<string> {
    // TODO: 集成 whisper.cpp WASM
    // 临时实现：调用后端 API
    const formData = new FormData();
    formData.append('file', audioBlob);
    formData.append('model', 'whisper-1');
    formData.append('language', 'zh');

    const response = await fetch('/api/whisper/transcribe', {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error('Whisper transcription failed');
    }

    const data = await response.json();
    return data.text;
  }

  async transcribeWithSegments(
    audioBlob: Blob
  ): Promise<{ transcript: string; segments: ConversationSegment[] }> {
    const formData = new FormData();
    formData.append('file', audioBlob);
    formData.append('model', 'whisper-1');
    formData.append('language', 'zh');
    formData.append('timestamp_granularities[]', 'segment');

    const response = await fetch('/api/whisper/transcribe?timestamp_granularities=segment', {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error('Whisper transcription failed');
    }

    const data = await response.json();
    
    const segments: ConversationSegment[] = data.segments?.map((seg: any) => ({
      id: crypto.randomUUID(),
      speaker: 'doctor',
      text: seg.text,
      timestamp: seg.start * 1000,
      confidence: 1.0,
    })) || [];

    return {
      transcript: data.text,
      segments,
    };
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/services/speech/WhisperService.ts
git commit -m "feat(service): add Whisper local speech recognition service"
```

---

## Chunk 3: LLM 服务

### Task 6: SOAP 病历生成服务

**Files:**
- Create: `src/services/llm/SOAPNoteGenerator.ts`

- [ ] **Step 1: 创建 SOAP 生成服务**

```typescript
// src/services/llm/SOAPNoteGenerator.ts
import { generateObject } from 'ai';
import { z } from 'zod';
import { createRegistry } from './llmRegistry';

const SOAPNoteSchema = z.object({
  subjective: z.object({
    chiefComplaint: z.object({
      symptom: z.string(),
      duration: z.string(),
      severity: z.number().min(1).max(10).optional(),
    }),
    historyOfPresentIllness: z.object({
      onset: z.string(),
      progression: z.string(),
      aggravatingFactors: z.array(z.string()).optional(),
      relievingFactors: z.array(z.string()).optional(),
    }),
  }),
  objective: z.object({
    vitalSigns: z.object({
      temperature: z.number().optional(),
      bloodPressure: z.object({
        systolic: z.number(),
        diastolic: z.number(),
      }).optional(),
      heartRate: z.number().optional(),
    }).optional(),
    physicalExamFindings: z.record(z.string()).optional(),
  }),
  assessment: z.object({
    primaryDiagnosis: z.object({
      condition: z.string(),
      icd10Code: z.string().optional(),
    }),
    differentialDiagnoses: z.array(z.object({
      condition: z.string(),
      likelihood: z.enum(['high', 'medium', 'low']),
    })).optional(),
  }),
  plan: z.object({
    diagnosticTests: z.array(z.string()).optional(),
    treatment: z.object({
      medications: z.array(z.object({
        name: z.string(),
        dosage: z.string(),
        frequency: z.string(),
      })).optional(),
    }).optional(),
  }),
});

export class SOAPNoteGenerator {
  private registry = createRegistry();

  async generate(transcript: string, provider: 'qwen' | 'claude' | 'ollama' = 'qwen') {
    const model = this.registry.languageModel(`${provider}>chat`);

    const { object } = await generateObject({
      model,
      schema: SOAPNoteSchema,
      prompt: `你是一位经验丰富的临床医生，请根据以下医患对话生成结构化病历。

【要求】
1. 严格按照 SOAP 格式组织内容
2. 使用专业医学术语
3. 保持客观、准确
4. 不确定的信息标注"待确认"

【对话内容】
${transcript}

请返回符合 schema 的 JSON 对象。`,
    });

    return object;
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/services/llm/SOAPNoteGenerator.ts
git commit -m "feat(service): add SOAP note generation service"
```

---

### Task 7: LLM Provider 注册表

**Files:**
- Create: `src/services/llm/llmRegistry.ts`

- [ ] **Step 1: 创建 LLM 注册表**

```typescript
// src/services/llm/llmRegistry.ts
import { createProviderRegistry } from 'ai';
import { openai } from '@ai-sdk/openai';
import { anthropic } from '@ai-sdk/anthropic';

export const createRegistry = () => {
  return createProviderRegistry(
    {
      // 云端 API
      anthropic: anthropic({
        apiKey: process.env.ANTHROPIC_API_KEY,
      }),
      openai: openai({
        apiKey: process.env.OPENAI_API_KEY,
      }),
      
      // 通义千问 (OpenAI 兼容接口)
      qwen: openai({
        apiKey: process.env.DASHSCOPE_API_KEY,
        baseURL: 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1',
      }),
      
      // 本地 Ollama
      ollama: openai({
        baseURL: 'http://localhost:11434/v1',
        apiKey: 'ollama',
      }),
    },
    { separator: '>' }
  );
};

export type LLMProvider = 'qwen' | 'claude' | 'ollama';
```

- [ ] **Step 2: 提交**

```bash
git add src/services/llm/llmRegistry.ts
git commit -m "feat(service): add LLM provider registry"
```

---

## Chunk 4: React Hooks

### Task 8: 语音识别 Hook

**Files:**
- Create: `src/hooks/useMedicalSpeechRecognition.ts`

- [ ] **Step 1: 创建 Hook**

```typescript
// src/hooks/useMedicalSpeechRecognition.ts
import { useState, useRef, useCallback } from 'react';
import { DeepgramService } from '../services/speech/DeepgramService';
import { WhisperService } from '../services/speech/WhisperService';
import { ConversationSegment } from '../types/medical';

export interface UseSpeechRecognitionOptions {
  provider?: 'deepgram' | 'whisper';
  language?: string;
  realTime?: boolean;
  onTranscript?: (text: string, isFinal: boolean) => void;
}

export function useMedicalSpeechRecognition(options: UseSpeechRecognitionOptions = {}) {
  const {
    provider = 'deepgram',
    language = 'zh-CN',
    realTime = true,
    onTranscript,
  } = options;

  const [isListening, setIsListening] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [transcript, setTranscript] = useState('');
  const [segments, setSegments] = useState<ConversationSegment[]>([]);

  const deepgramRef = useRef<DeepgramService | null>(null);
  const whisperRef = useRef<WhisperService | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);

  const startListening = useCallback(async () => {
    setError(null);
    audioChunksRef.current = [];

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      if (realTime && provider === 'deepgram') {
        // 实时模式：使用 Deepgram
        deepgramRef.current = new DeepgramService({
          apiKey: import.meta.env.VITE_DEEPGRAM_API_KEY || '',
        });

        await deepgramRef.current.startStreaming(
          (text, isFinal) => {
            setTranscript((prev) => prev + ' ' + text);
            onTranscript?.(text, isFinal);
          },
          (segment) => {
            setSegments((prev) => [...prev, segment]);
          }
        );
      } else {
        // 录音模式：使用 MediaRecorder
        mediaRecorderRef.current = new MediaRecorder(stream);
        mediaRecorderRef.current.ondataavailable = (event) => {
          if (event.data.size > 0) {
            audioChunksRef.current.push(event.data);
          }
        };
        mediaRecorderRef.current.start(250);
      }

      setIsListening(true);
    } catch (err: any) {
      setError(err.message);
    }
  }, [provider, realTime, onTranscript]);

  const stopListening = useCallback(async () => {
    if (realTime && provider === 'deepgram') {
      deepgramRef.current?.stopStreaming();
    } else {
      mediaRecorderRef.current?.stop();
      mediaRecorderRef.current?.stream.getTracks().forEach(track => track.stop());
    }

    setIsListening(false);
    setIsProcessing(true);

    // 如果不是实时转写，录音结束后处理
    if (!realTime || provider === 'whisper') {
      const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
      whisperRef.current = new WhisperService();
      
      try {
        const result = await whisperRef.current.transcribeWithSegments(audioBlob);
        setTranscript(result.transcript);
        setSegments(result.segments);
      } catch (err: any) {
        setError(err.message);
      }
    }

    setIsProcessing(false);
  }, [provider, realTime]);

  const reset = useCallback(() => {
    setTranscript('');
    setSegments([]);
    setError(null);
  }, []);

  return {
    isListening,
    isProcessing,
    error,
    transcript,
    segments,
    startListening,
    stopListening,
    reset,
  };
}
```

- [ ] **Step 2: 提交**

```bash
git add src/hooks/useMedicalSpeechRecognition.ts
git commit -m "feat(hook): add medical speech recognition hook"
```

---

## Chunk 5: UI 组件

### Task 9: 语音录入组件

**Files:**
- Create: `src/components/VoiceConsultation/VoiceInput.tsx`

- [ ] **Step 1: 创建组件**

```typescript
// src/components/VoiceConsultation/VoiceInput.tsx
import React from 'react';

interface VoiceInputProps {
  isListening: boolean;
  isProcessing: boolean;
  onStart: () => void;
  onStop: () => void;
  disabled?: boolean;
}

export const VoiceInput: React.FC<VoiceInputProps> = ({
  isListening,
  isProcessing,
  onStart,
  onStop,
  disabled = false,
}) => {
  const handleClick = () => {
    if (isListening) {
      onStop();
    } else {
      onStart();
    }
  };

  return (
    <div className="voice-input">
      <button
        onClick={handleClick}
        disabled={disabled || isProcessing}
        className={`voice-input__button ${isListening ? 'voice-input__button--recording' : ''}`}
      >
        {isProcessing ? (
          <span className="spinner">⏳</span>
        ) : isListening ? (
          <span>⏹ 结束问诊</span>
        ) : (
          <span>🎤 开始问诊</span>
        )}
      </button>
      
      {isListening && (
        <div className="voice-input__indicator">
          <span className="pulse">🔴</span>
          <span>正在录音...</span>
        </div>
      )}
    </div>
  );
};
```

- [ ] **Step 2: 添加样式**

```css
/* src/components/VoiceConsultation/VoiceInput.css */
.voice-input {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.voice-input__button {
  padding: 16px 32px;
  font-size: 18px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  background: #007bff;
  color: white;
  transition: background 0.2s;
}

.voice-input__button:hover:not(:disabled) {
  background: #0056b3;
}

.voice-input__button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.voice-input__button--recording {
  background: #dc3545;
  animation: pulse 1.5s infinite;
}

.voice-input__indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #dc3545;
  font-weight: 500;
}

.pulse {
  animation: pulse-dot 1s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/components/VoiceConsultation/VoiceInput.tsx src/components/VoiceConsultation/VoiceInput.css
git commit -m "feat(component): add VoiceInput component"
```

---

### Task 10: 转写文本展示组件

**Files:**
- Create: `src/components/VoiceConsultation/TranscriptView.tsx`

- [ ] **Step 1: 创建组件**

```typescript
// src/components/VoiceConsultation/TranscriptView.tsx
import React, { useEffect, useRef } from 'react';
import { ConversationSegment } from '../../types/medical';

interface TranscriptViewProps {
  segments: ConversationSegment[];
  fullTranscript: string;
}

export const TranscriptView: React.FC<TranscriptViewProps> = ({
  segments,
  fullTranscript,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [segments]);

  if (segments.length === 0 && !fullTranscript) {
    return (
      <div className="transcript-view transcript-view--empty">
        <p>点击"开始问诊"后，对话内容将实时显示在这里...</p>
      </div>
    );
  }

  return (
    <div className="transcript-view" ref={containerRef}>
      {segments.length > 0 ? (
        <div className="transcript-view__segments">
          {segments.map((segment) => (
            <div
              key={segment.id}
              className={`transcript-view__segment transcript-view__segment--${segment.speaker}`}
            >
              <span className="transcript-view__speaker">
                {segment.speaker === 'doctor' ? '👨‍⚕️ 医生' : '👤 患者'}:
              </span>
              <span className="transcript-view__text">{segment.text}</span>
            </div>
          ))}
        </div>
      ) : (
        <div className="transcript-view__text">{fullTranscript}</div>
      )}
    </div>
  );
};
```

- [ ] **Step 2: 添加样式**

```css
/* src/components/VoiceConsultation/TranscriptView.css */
.transcript-view {
  max-height: 300px;
  overflow-y: auto;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.transcript-view--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c757d;
  min-height: 100px;
}

.transcript-view__segments {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.transcript-view__segment {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.transcript-view__speaker {
  font-weight: 600;
  color: #495057;
  font-size: 14px;
}

.transcript-view__text {
  color: #212529;
  line-height: 1.6;
}
```

- [ ] **Step 3: 提交**

```bash
git add src/components/VoiceConsultation/TranscriptView.tsx src/components/VoiceConsultation/TranscriptView.css
git commit -m "feat(component): add TranscriptView component"
```

---

## Chunk 6: 主容器组件

### Task 11: 智能问诊主组件

**Files:**
- Create: `src/components/VoiceConsultation/VoiceConsultation.tsx`

- [ ] **Step 1: 创建主组件**

```typescript
// src/components/VoiceConsultation/VoiceConsultation.tsx
import React from 'react';
import { useConsultationStore } from '../../stores/consultationStore';
import { useMedicalSpeechRecognition } from '../../hooks/useMedicalSpeechRecognition';
import { VoiceInput } from './VoiceInput';
import { TranscriptView } from './TranscriptView';
import { PatientInfoBanner } from './PatientInfoBanner';
import { SOAPNoteEditor } from './SOAPNoteEditor';
import { DiagnosisPanel } from './DiagnosisPanel';

interface VoiceConsultationProps {
  patientId?: string;
  onSessionComplete?: (sessionId: string) => void;
}

export const VoiceConsultation: React.FC<VoiceConsultationProps> = ({
  patientId,
  onSessionComplete,
}) => {
  const { session, startSession, setSoapNote, setStatus } = useConsultationStore();
  
  const {
    isListening,
    isProcessing,
    error,
    transcript,
    segments,
    startListening,
    stopListening,
  } = useMedicalSpeechRecognition({
    provider: 'deepgram',
    realTime: true,
  });

  const handleStart = () => {
    if (patientId) {
      startSession(patientId);
    }
    startListening();
  };

  const handleStop = async () => {
    stopListening();
    // TODO: 调用 LLM 生成病历
    setStatus('processing');
  };

  return (
    <div className="voice-consultation">
      <PatientInfoBanner patientId={patientId} />
      
      <div className="voice-consultation__content">
        <TranscriptView segments={segments} fullTranscript={transcript} />
        
        <VoiceInput
          isListening={isListening}
          isProcessing={isProcessing}
          onStart={handleStart}
          onStop={handleStop}
          disabled={!patientId}
        />
        
        {error && (
          <div className="voice-consultation__error">
            ⚠️ {error}
          </div>
        )}
      </div>
    </div>
  );
};
```

- [ ] **Step 2: 提交**

```bash
git add src/components/VoiceConsultation/VoiceConsultation.tsx
git commit -m "feat(component): add VoiceConsultation main container"
```

---

## 测试策略

### 单元测试

- [ ] `useMedicalSpeechRecognition` hook 测试
- [ ] `DeepgramService` mocking 测试
- [ ] `SOAPNoteGenerator` schema 验证测试

### 集成测试

- [ ] 端到端问诊流程测试
- [ ] HIS postMessage 通信测试

### 手动测试清单

- [ ] Chrome 浏览器录音权限
- [ ] 实时转写延迟 < 500ms
- [ ] 医学术语识别准确率
- [ ] 病历生成时间 < 10 秒

---

## 环境变量配置

创建 `.env.local` 文件：

```bash
# Deepgram API Key
VITE_DEEPGRAM_API_KEY=your_deepgram_api_key

# LLM API Keys (可选，根据使用的 provider)
ANTHROPIC_API_KEY=your_anthropic_key
OPENAI_API_KEY=your_openai_key
DASHSCOPE_API_KEY=your_dashscope_key
```

---

## 执行顺序

1. **Chunk 1**: 基础架构（依赖、类型、存储）
2. **Chunk 2**: 语音识别服务（Deepgram + Whisper）
3. **Chunk 3**: LLM 服务（SOAP 生成 + 注册表）
4. **Chunk 4**: React Hooks
5. **Chunk 5**: UI 组件
6. **Chunk 6**: 主容器组件

每个 Chunk 完成后进行 review，确认无误后继续下一个。
