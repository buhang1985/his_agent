# 智能问诊功能规格说明

**版本**: 1.0  
**日期**: 2026-03-10  
**状态**: 已批准  
**关联变更**: voice-consultation

---

## 1. 概述

### 1.1 功能定位

智能问诊是 his_agent 的核心入口功能，通过语音录入医患对话，自动生成结构化病历（SOAP 格式）和诊断建议。

### 1.2 目标用户

- **主要用户**: 门诊医生
- **使用场景**: 门诊问诊过程中辅助记录病历

### 1.3 核心价值

- **提高效率**: 语音录入替代手工打字，问诊时间缩短 30-50%
- **规范化**: 自动生成符合医疗规范的 SOAP 病历
- **辅助诊断**: 基于症状提供鉴别诊断建议
- **减少负担**: 医生专注于患者，减少文书工作

---

## 2. 功能需求

### 2.1 核心功能

| 功能 | 描述 | 优先级 |
|------|------|--------|
| **语音录入** | 支持麦克风录音，实时显示录音状态 | P0 |
| **实时转写** | 边说边转写，支持中文医学术语 | P0 |
| **录音后转写** | 录音结束后统一转写（备用模式） | P1 |
| **病历生成** | 基于转写内容生成 SOAP 格式病历 | P0 |
| **诊断建议** | 生成鉴别诊断建议列表 | P0 |
| **病历编辑** | 支持医生修改生成的病历 | P0 |
| **保存回 HIS** | 通过 postMessage 将病历发送回 HIS | P0 |

### 2.2 非功能需求

| 需求 | 指标 |
|------|------|
| **语音识别准确率** | 医学术语识别率 ≥ 85% |
| **转写延迟** | 实时转写延迟 < 500ms |
| **病历生成时间** | < 10 秒（云端 LLM）|
| **数据隐私** | 录音数据不持久化，生成后自动删除 |
| **兼容性** | 支持 Chrome 90+、Edge 90+ |

---

## 3. 技术设计

### 3.1 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                    智能问诊界面                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  患者信息栏 (从 HIS 获取)                                │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │  ┌─────────────────────────────────────────────────┐    │    │
│  │  │              转写文本展示区                      │    │    │
│  │  │  (实时显示医患对话内容)                          │    │    │
│  │  └─────────────────────────────────────────────────┘    │    │
│  │                                                          │    │
│  │  ┌─────────────────────────────────────────────────┐    │    │
│  │  │              SOAP 病历编辑区                     │    │    │
│  │  │  S: 主观资料 (主诉、现病史)                      │    │    │
│  │  │  O: 客观资料 (体征、检查结果)                    │    │    │
│  │  │  A: 评估 (诊断)                                  │    │    │
│  │  │  P: 计划 (治疗方案)                              │    │    │
│  │  └─────────────────────────────────────────────────┘    │    │
│  │                                                          │    │
│  │  ┌─────────────────────────────────────────────────┐    │    │
│  │  │              诊断建议面板                        │    │    │
│  │  │  • 原发性高血压 (可能性：高)                     │    │    │
│  │  │  • 糖尿病 (可能性：中)                           │    │    │
│  │  └─────────────────────────────────────────────────┘    │    │
│  │                                                          │    │
│  │  [🎤 开始问诊] [⏹ 结束问诊] [💾 保存] [❌ 取消]           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 组件结构

```
src/components/VoiceConsultation/
├── VoiceConsultation.tsx        # 主容器组件
├── VoiceInput.tsx               # 语音录入控件（录音按钮、状态指示）
├── TranscriptView.tsx           # 转写文本展示（实时滚动）
├── SOAPNoteEditor.tsx           # SOAP 病历编辑器（可编辑表单）
├── DiagnosisPanel.tsx           # 诊断建议列表
└── PatientInfoBanner.tsx        # 患者基本信息栏

src/hooks/
├── useVoiceConsultation.ts      # 问诊会话状态管理
├── useMedicalSpeechRecognition.ts # 语音识别逻辑
└── useSOAPNoteGenerator.ts      # 病历生成逻辑

src/services/
├── speech/
│   ├── DeepgramService.ts       # Deepgram 云端识别
│   └── WhisperService.ts        # Whisper 本地识别
└── llm/
    ├── SOAPNoteGenerator.ts     # SOAP 病历生成
    └── DiagnosisSuggester.ts    # 诊断建议生成

src/stores/
└── consultationStore.ts         # 问诊状态存储 (Zustand)
```

### 3.3 数据流

```
┌─────────────────────────────────────────────────────────────────┐
│                        数据流                                    │
└─────────────────────────────────────────────────────────────────┘

  ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
  │  麦克风  │ ──▶ │ 语音识别 │ ──▶ │  转写文本 │ ──▶ │  状态存储 │
  └──────────┘     └──────────┘     └──────────┘     └──────────┘
                      │                                    │
                      ▼                                    ▼
               ┌──────────┐                         ┌──────────┐
               │  LLM 服务 │ ◀────────────────────── │ 完整文本 │
               └──────────┘                         └──────────┘
                      │
                      ▼
               ┌──────────┐
               │ SOAP 病历 │
               │ 诊断建议 │
               └──────────┘
                      │
                      ▼
               ┌──────────┐
               │  HIS 系统 │
               │ (postMsg)│
               └──────────┘
```

### 3.4 核心类型定义

```typescript
// 问诊会话状态
interface ConsultationSession {
  id: string;
  patientId: string;
  status: 'idle' | 'recording' | 'processing' | 'review' | 'completed';
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

// 对话片段
interface ConversationSegment {
  id: string;
  speaker: 'doctor' | 'patient';
  text: string;
  timestamp: number;
  confidence: number;
}

// 语音识别配置
interface SpeechRecognitionConfig {
  provider: 'deepgram' | 'whisper';
  language: 'zh-CN';
  medicalTerms: string[];
  realTime: boolean;
}

// LLM 配置
interface LLMConfig {
  provider: 'qwen' | 'claude' | 'ollama';
  model: string;
  temperature: number;
  maxTokens: number;
}
```

### 3.5 语音识别方案

**主方案 (云端优先): 讯飞医疗智能语音输入系统 (iFlytek Medical ASR)**
```typescript
// 实时流式识别，延迟 < 400ms
// 中文医学术语优化，97% 识别准确率
// 内置 100 万 + 医学词汇 (52 科室覆盖)
// 支持 27 种中文方言
// MLPS 合规，国内数据中心
// 成本：约 ¥0.01-0.03/次

// API 配置示例
interface IFlytekConfig {
  appId: string;        // 讯飞应用 ID
  apiKey: string;       // API Key
  apiSecret: string;    // API Secret
  hostUrl: string;      // wss://iat-api.xfyun.cn/v2/iat
  medicalDepartment?: string; // 科室词库 (如："cardiology", "pediatrics")
}
```

**备选方案 (云端): 阿里云智能语音 (Alibaba Cloud Intelligent Speech)**
```typescript
// 实时流式识别，延迟 < 600ms
// 自学习平台支持医学词库定制
// 98% 普通话准确率，27 方言支持
// MLPS、ISO27001 合规
// 成本：¥4.2/1000 次调用

// 适用场景：阿里云生态集成、自服务模型训练
```

**本地部署 (备用): Whisper.cpp**
```typescript
// 本地识别，数据不出院
// 延迟 1-2s，适合私有化部署
// 使用中文医学词库增强
// 需要本地 GPU/CPU 资源

// 适用场景：数据完全不出院要求、网络受限环境
```

**降级策略**:
```
讯飞医疗 ASR 失败 → 阿里云 ASR → Whisper 本地识别
```

### 3.6 LLM 提示词模板

```typescript
const SOAP_NOTE_PROMPT = `
你是一位经验丰富的临床医生，请根据以下医患对话生成结构化病历。

【要求】
1. 严格按照 SOAP 格式组织内容
2. 使用专业医学术语
3. 保持客观、准确
4. 不确定的信息标注"待确认"

【对话内容】
{transcript}

【输出格式】
请返回 JSON 格式：
{
  "subjective": {
    "chiefComplaint": { "symptom": "", "duration": "" },
    "historyOfPresentIllness": { "onset": "", "progression": "" }
  },
  "objective": {
    "vitalSigns": {},
    "physicalExamFindings": {}
  },
  "assessment": {
    "primaryDiagnosis": { "condition": "", "icd10Code": "" },
    "differentialDiagnoses": []
  },
  "plan": {
    "diagnosticTests": [],
    "treatment": { "medications": [] }
  }
}
`;

const DIAGNOSIS_PROMPT = `
根据以下病历信息，提供鉴别诊断建议。

【病历摘要】
{soapNoteSummary}

【要求】
1. 列出 3-5 个最可能的诊断
2. 每个诊断标注可能性（高/中/低）
3. 提供支持证据
4. 建议进一步检查

【输出格式】
JSON 数组：
[
  {
    "condition": "诊断名称",
    "likelihood": "high|medium|low",
    "icd10Code": "编码",
    "supportingEvidence": ["证据 1", "证据 2"],
    "recommendedTests": ["检查 1", "检查 2"]
  }
]
`;
```

---

## 4. 安全与合规

### 4.1 数据安全

| 措施 | 实现 |
|------|------|
| **录音不持久化** | 内存缓存，生成后自动删除 |
| **传输加密** | WebSocket (WSS) + HTTPS |
| **PII 保护** | 转写前不发送任何数据到云端 |
| **访问控制** | 与 HIS 共享会话认证 |

### 4.2 医疗安全

| 措施 | 实现 |
|------|------|
| **紧急症状检测** | 关键词触发紧急就医提示 |
| **免责声明** | 明确 AI 建议仅供参考 |
| **医生审核** | 必须经医生确认后才能保存 |
| **审计日志** | 记录操作时间、用户、动作 |

### 4.3 紧急重定向

```typescript
const EMERGENCY_KEYWORDS = [
  '胸痛', '呼吸困难', '意识丧失', '大出血',
  '剧烈头痛', '肢体无力', '言语不清'
];

const checkEmergency = (transcript: string): boolean => {
  return EMERGENCY_KEYWORDS.some(keyword => 
    transcript.toLowerCase().includes(keyword)
  );
};

// 检测到紧急症状时显示：
// ⚠️ 检测到可能的紧急症状，建议立即转诊急诊科！
```

---

## 5. 错误处理

### 5.1 错误类型

| 错误 | 用户提示 | 处理方案 |
|------|----------|----------|
| 麦克风权限拒绝 | "请允许麦克风访问" | 显示权限设置指引 |
| 语音识别失败 | "转写失败，请重试" | 切换到备用方案 |
| LLM 超时 | "生成超时，请重试" | 重试或降级处理 |
| HIS 通信失败 | "保存失败，请手动复制" | 提供导出功能 |

### 5.2 降级策略

```
Deepgram 失败 → Whisper 本地识别
                    ↓
              云端 LLM 失败 → 本地 Ollama
                    ↓
              HIS 保存失败 → 导出文本/JSON
```

---

## 6. 测试策略

### 6.1 单元测试

- [ ] 语音识别服务 mocking 测试
- [ ] SOAP 病历生成提示词测试
- [ ] 紧急症状检测测试
- [ ] 状态管理 hook 测试

### 6.2 集成测试

- [ ] 端到端问诊流程测试
- [ ] HIS postMessage 通信测试
- [ ] 多浏览器兼容性测试

### 6.3 医疗术语测试

使用 `xtea/chinese_medical_words` 词库验证：
- [ ] 疾病名称识别准确率
- [ ] 药品名称识别准确率
- [ ] 检查检验项目识别准确率

---

## 7. 验收标准

### 7.1 功能验收

- [ ] 能够成功录制语音并转写
- [ ] 医学术语识别准确率 ≥ 85%
- [ ] 生成的 SOAP 病历格式正确
- [ ] 诊断建议合理且有帮助
- [ ] 病历可以成功保存回 HIS

### 7.2 性能验收

- [ ] 实时转写延迟 < 500ms
- [ ] 病历生成时间 < 10 秒
- [ ] 页面加载时间 < 2 秒

### 7.3 安全验收

- [ ] 录音数据不持久化
- [ ] 紧急症状正确触发提示
- [ ] 无 PHI 数据泄露风险

---

## 8. 非目标 (Non-Goals)

以下功能**不在本次实现范围**内：

- ❌ 多科室病历模板（首期仅通用模板）
- ❌ 语音合成播报（仅语音识别）
- ❌ 离线运行能力（首期需网络）
- ❌ 多语言支持（首期仅中文）
- ❌ 医学影像分析

---

## 9. 依赖与前置条件

| 依赖 | 状态 |
|------|------|
| OpenSpec 配置完成 | ✅ 已完成 |
| 项目脚手架搭建 | ✅ 已完成 |
| 医疗类型定义 | ✅ 已完成 |
| LLM 服务配置 | ⏳ 待配置 |
| HIS 接口文档 | ⏳ 待提供 |

---

## 10. 参考资料

- [Deepgram Medical Transcription](https://deepgram.com/solutions/medical-transcription)
- [xtea/chinese_medical_words](https://github.com/xtea/chinese_medical_words)
- [Vercel AI SDK](https://sdk.vercel.ai/docs)
- [CREOLA Clinical Safety Framework](https://tortus.ai/creola-a-framework-for-evaluating-clinical-safety/)
