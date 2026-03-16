## 上下文

his_agent 项目需要验证讯飞 ASR 语音识别集成和 LLM 病历生成功能是否可用。当前项目已配置好基础技术栈（Spring Boot 3.x + Vue 3 + TypeScript），并获得了讯飞 ASR 的 API 凭证（AppID: 8bc8b3ca），但缺少端到端的测试用例。

**当前状态**:
- ✅ 项目脚手架完成
- ✅ 技术栈配置完成（Spring Boot 3.4.x, Vue 3.4+, TypeScript 5+）
- ✅ 讯飞 ASR API 凭证已提供
- ✅ 监控系统部署完成
- ❌ 缺少语音识别功能实现
- ❌ 缺少 LLM 病历生成实现
- ❌ 缺少端到端测试页面

**约束**:
- 前端需要使用 Vue 3 Composition API + TypeScript
- 后端需要使用 Spring Boot 3.x + Spring AI
- 语音识别使用讯飞 ASR WebSocket API
- 病历生成使用 LLM（Qwen/Claude/Ollama）
- 录音数据不持久化，生成后自动删除

## 目标 / 非目标

**目标：**
1. 创建前端语音录入测试页面，支持开始/暂停/继续/结束录音
2. 实现讯飞 ASR WebSocket 实时语音识别
3. 创建后端 REST API 接收转写文本并生成 SOAP 病历
4. 实现 LLM 病历生成服务（基于 Spring AI）
5. 展示生成的病历并支持编辑
6. 完整的错误处理和降级策略

**非目标：**
- ❌ 角色分离（医生/患者区分）
- ❌ 说话人识别
- ❌ HIS 系统集成（postMessage）
- ❌ 诊断建议生成
- ❌ 病历保存到数据库
- ❌ 用户认证和授权（测试页面无需认证）

## 决策

### 1. 语音识别方案

**决策**: 使用讯飞 ASR WebSocket API 作为主方案

**理由**:
- 医疗专业化：97% 中文医学识别准确率
- 低延迟：400ms 响应时间
- 成本效益：约 ¥0.01-0.03/次
- 已提供 API 凭证

**替代方案**:
- 腾讯云 ASR：免费额度多，但医学词库不如讯飞
- 阿里云 ASR：方言支持好，但医学专业性稍弱
- Whisper.cpp：本地部署，但延迟 1-2s

### 2. 前端架构

**决策**: 使用 Vue 3 Composition API + TypeScript + Web Audio API

**理由**:
- 符合项目技术栈
- TypeScript 提供类型安全
- Web Audio API 原生支持音频处理
- Composition API 便于逻辑复用

**音频处理**:
- 使用 `navigator.mediaDevices.getUserMedia` 获取麦克风
- 使用 `AudioContext` 和 `ScriptProcessorNode` 处理音频流
- 每 40ms 发送 1280 字节 PCM 数据到讯飞 WebSocket

### 3. 后端架构

**决策**: Spring Boot 3.x + Spring WebSocket + Spring AI

**理由**:
- 符合项目技术栈
- Spring WebSocket 简化 WebSocket 客户端实现
- Spring AI 提供统一 LLM 接口

**接口设计**:
```
POST /api/v1/voice/transcribe    - 创建语音转写会话（返回 WebSocket URL）
POST /api/v1/voice/generate      - 基于转写文本生成 SOAP 病历
GET  /api/v1/voice/session/{id}  - 查询会话状态
DELETE /api/v1/voice/session/{id}- 清理会话（删除音频数据）
```

### 4. 数据安全

**决策**: 录音数据仅内存缓存，生成病历后自动删除

**理由**:
- 符合医疗数据隐私要求
- 减少法律风险
- 简化系统设计（无需持久化存储）

**实现**:
- 前端：不保存录音文件
- 后端：使用 `ConcurrentHashMap` 存储临时会话数据
- 会话过期时间：2 小时
- 生成病历后立即删除音频数据

### 5. LLM 选型

**决策**: Spring AI 统一接口，支持多 LLM Provider 切换

**理由**:
- 避免供应商锁定
- 支持云端和本地双模式
- 便于降级和 A/B 测试

**默认配置**:
- 主方案：Qwen（通义千问）- 中文医疗理解好
- 备用：Claude - 英文医学文献理解好
- 本地：Ollama + Llama3 - 数据不出院

## 风险 / 权衡

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 讯飞 ASR 服务不可用 | 高 | 实现降级策略：讯飞 → 阿里云 → Whisper |
| 医学词库识别率低 | 中 | 支持自定义科室词库，收集反馈持续优化 |
| LLM 生成病历质量不稳定 | 中 | 提供 few-shot 示例，设置医生审核环节 |
| 浏览器兼容性问题 | 低 | 仅支持 Chrome 90+、Edge 90+（医院标准配置） |
| WebSocket 连接不稳定 | 中 | 实现自动重连机制，指数退避策略 |
| 音频格式转换开销 | 低 | 使用 Web Audio API 原生转换，减少拷贝 |
| 内存泄漏（长时间录音） | 中 | 设置最大录音时长（30 分钟），超时自动停止 |

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3 + TS)                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  TestPage.vue                                            │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │    │
│  │  │ VoiceInput   │  │TranscriptView│  │ SOAPNote     │  │    │
│  │  │ Component    │  │ Component    │  │ Editor       │  │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  │    │
│  └─────────────────────────────────────────────────────────┘    │
│         │                        │                      │        │
│         ▼                        ▼                      ▼        │
│  ┌──────────────┐        ┌──────────────┐       ┌──────────────┐│
│  │IFlytekService│        │useSpeech     │       │SOAPNote      ││
│  │(WebSocket)   │        │Recognition   │       │Types         ││
│  └──────────────┘        └──────────────┘       └──────────────┘│
└─────────────────────────────────────────────────────────────────┘
         │                        │
         │ WebSocket (音频流)      │ REST API (转写文本)
         ▼                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                        后端 (Spring Boot)                        │
│  ┌──────────────┐        ┌──────────────┐       ┌──────────────┐│
│  │TestController│───────▶│VoiceRecognition│      │SoapNote      ││
│  │(REST API)    │        │Service       │      │Generator     ││
│  └──────────────┘        └──────────────┘       └──────────────┘│
│                                │                      │          │
│                                ▼                      ▼          │
│                         ┌──────────────┐       ┌──────────────┐  │
│                         │iFlytek ASR   │       │Spring AI     │  │
│                         │WebSocket     │       │(Qwen/Claude) │  │
│                         └──────────────┘       └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 数据流

```
1. 用户点击"开始录音"
   ↓
2. 前端请求麦克风权限
   ↓
3. 建立讯飞 ASR WebSocket 连接
   ↓
4. 实时发送音频流（40ms/次）
   ↓
5. 接收转写结果（中间结果 + 最终结果）
   ↓
6. 用户点击"结束录音"
   ↓
7. 发送结束标识到讯飞 ASR
   ↓
8. 合并所有转写文本
   ↓
9. 调用后端 POST /api/v1/voice/generate
   ↓
10. 后端调用 LLM 生成 SOAP 病历
   ↓
11. 返回病历 JSON
   ↓
12. 前端展示并允许编辑
```

## 模块边界

### 前端模块

| 模块 | 职责 | 接口 |
|------|------|------|
| `TestPage.vue` | 测试页面容器 | 无 |
| `VoiceInput.vue` | 录音控制（开始/暂停/结束） | `@start`, `@stop`, `@statusChange` |
| `TranscriptView.vue` | 转写文本展示 | `transcript: string`, `segments: Segment[]` |
| `SOAPNoteEditor.vue` | 病历编辑表单 | `soap: SOAPNote`, `@update` |
| `IFlytekService` | 讯飞 ASR WebSocket 客户端 | `start()`, `stop()`, `onResult` |
| `useMedicalSpeechRecognition` | 语音识别 Composable | `start()`, `stop()`, `transcript`, `status` |

### 后端模块

| 模块 | 职责 | 接口 |
|------|------|------|
| `TestController` | REST API 入口 | `/api/v1/voice/*` |
| `VoiceRecognitionService` | 语音识别服务 | `recognize(AudioStream)` |
| `IFlytekAsrClient` | 讯飞 ASR WebSocket 客户端 | `connect()`, `sendAudio()`, `close()` |
| `SoapNoteGeneratorService` | SOAP 病历生成 | `generate(transcript)` |
| `LlmService` | LLM 统一接口 | `generate(prompt)` |

## 接口定义

### 前端 → 后端

```typescript
// POST /api/v1/voice/generate
interface GenerateSoapRequest {
  transcript: string;        // 完整转写文本
  patientId?: string;        // 患者 ID（可选）
  department?: string;       // 科室（可选）
}

interface GenerateSoapResponse {
  recordId: string;
  soap: SOAPNote;
  confidence: number;
  lowConfidenceFields: string[];
  generatedAt: string;
}

interface SOAPNote {
  subjective: {
    chiefComplaint: string;
    historyOfPresentIllness: string;
  };
  objective: {
    vitalSigns: Record<string, any>;
    physicalExamFindings: string;
  };
  assessment: {
    primaryDiagnosis: string;
    differentialDiagnoses: string[];
  };
  plan: {
    diagnosticTests: string[];
    treatment: string;
    advice: string;
  };
}
```

### 前端 → 讯飞 ASR

```
WebSocket URL: wss://rtasr.xfyun.cn/v1/ws?appid={appid}&ts={ts}&signa={signa}&pd=medical&lang=cn

发送：音频数据（Binary Message, PCM 16k 16bit 单声道）
接收：转写结果（Text Message, JSON）
```

## 错误处理

### 前端错误

| 错误 | 用户提示 | 处理方案 |
|------|----------|----------|
| 麦克风权限拒绝 | "请允许麦克风访问" | 显示权限设置指引 |
| WebSocket 连接失败 | "连接服务失败，请重试" | 自动重试 3 次，指数退避 |
| 讯飞 ASR 超时 | "转写超时，请重试" | 切换到备用 ASR 方案 |
| 后端 API 失败 | "生成病历失败，请重试" | 显示错误详情，支持手动输入 |

### 后端错误

| 错误 | 日志级别 | 处理方案 |
|------|----------|----------|
| 讯飞 ASR 连接失败 | ERROR | 记录 SID，切换到备用方案 |
| LLM 超时 | WARN | 重试 1 次，返回降级结果 |
| 参数验证失败 | WARN | 返回 400 Bad Request |
| 会话不存在 | INFO | 返回 404 Not Found |

## 测试策略

### 单元测试

- `IFlytekService`: Mock WebSocket 测试签名生成、消息处理
- `SoapNoteGeneratorService`: Mock LLM 测试提示词模板
- `useMedicalSpeechRecognition`: 测试状态管理逻辑

### 集成测试

- 端到端录音 → 转写 → 病历生成流程
- 讯飞 ASR 实际连接测试
- LLM 实际调用测试

### 手动测试

- 实际对着麦克风说话验证识别准确率
- 测试医学专业术语识别效果
- 验证病历生成质量

## 部署说明

### 前端部署

```bash
cd his_agent-frontend
npm install crypto-js  # 签名生成依赖
npm run dev            # 开发模式
```

### 后端部署

```bash
cd his_agent-backend
# 配置 application.yml 添加讯飞凭证
mvn spring-boot:run
```

### 环境变量

```bash
# 前端 .env
VITE_IFLYTEK_APP_ID=8bc8b3ca
VITE_IFLYTEK_API_KEY=ef71686251f3f7b42bbb56c3d737f938

# 后端 application.yml
iflytek:
  app-id: 8bc8b3ca
  api-key: ef71686251f3f7b42bbb56c3d737f938
```
