# 技术栈规格说明

**版本**: 1.0  
**日期**: 2026-03-10  
**状态**: 已批准

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        HIS 宿主系统                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    iframe 嵌入区域                       │    │
│  │  ┌───────────────────────────────────────────────────┐  │    │
│  │  │           his_agent Web 应用                       │  │    │
│  │  │  ┌─────────┐ ┌─────────┐ ┌─────────────────────┐  │  │    │
│  │  │  │ 语音录入 │ │ 诊断建议 │ │   病历生成/展示     │  │  │    │
│  │  │  └─────────┘ └─────────┘ └─────────────────────┘  │  │    │
│  │  │                       │                            │  │    │
│  │  │  ┌────────────────────┴────────────────────────┐  │  │    │
│  │  │  │           LLM 抽象层                         │  │  │    │
│  │  │  └────────────────────┬────────────────────────┘  │  │    │
│  │  └───────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                    postMessage 通信                              │
└──────────────────────────────┼──────────────────────────────────┘
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
           ┌────────────────┐   ┌────────────────┐
           │   云端 LLM API  │   │  本地 LLM 服务  │
           │  (Claude/GPT)  │   │ (Ollama/vLLM)  │
           └────────────────┘   └────────────────┘
```

---

## 技术选型

### 核心栈

| 层级 | 技术 | 版本 | 理由 |
|------|------|------|------|
| 运行时 | Node.js | 20+ LTS | 企业级支持，生态成熟 |
| 语言 | TypeScript | 5+ | 类型安全，减少医疗应用运行时错误 |
| 前端框架 | React | 18+ | 组件化开发，生态丰富 |
| 构建工具 | Vite | 5+ | 快速开发体验，生产优化 |

### 语音识别方案对比

| 方案 | 优势 | 劣势 | 推荐场景 |
|------|------|------|----------|
| **Web Speech API** | 浏览器原生，无需额外依赖 | 医学术语识别率低，依赖浏览器 | 快速原型 |
| **Whisper (本地)** | 开源，可自定义医学词表，数据不出院 | 需要 GPU/较好 CPU | 私有化部署 |
| **Azure Speech** | 中文支持好，可定制语音模型 | 数据出云，成本较高 | 云端部署 |
| **讯飞/百度语音** | 中文医学术语优化 | API 成本，数据合规 | 国内云端部署 |

**推荐方案**: 支持双模式
- **开发/云端模式**: Azure Speech / 讯飞 (快速上线)
- **私有化模式**: Whisper.cpp + 自定义医学词表 (数据合规)

### LLM 集成方案

| 组件 | 推荐库 | 理由 |
|------|--------|------|
| **LLM 抽象层** | Vercel AI SDK | 统一接口，支持多 provider，TypeScript 友好 |
| **本地部署** | Ollama | 一键部署，支持 Qwen/ChatGLM 等国产模型 |
| **RAG 框架** | LangChain.js / LlamaIndex | 医学知识库检索，引用溯源 |
| **向量数据库** | Chroma / Faiss | 本地运行，医学文献存储 |

### 支持的大模型

| 类型 | 模型 | 接入方式 |
|------|------|----------|
| **云端** | Claude API | Vercel AI SDK |
| **云端** | GPT-4 API | Vercel AI SDK |
| **云端** | 通义千问 API | 阿里云百炼平台 |
| **云端** | ChatGLM API | 智谱 AI 开放平台 |
| **本地** | Qwen-72B | Ollama / vLLM |
| **本地** | Llama-3-70B | Ollama / vLLM |
| **本地** | ChatGLM3-6B | Ollama |

---

## HIS 集成方案

### iframe 嵌入规范

```html
<!-- HIS 系统侧嵌入代码示例 -->
<iframe 
  src="https://his-agent.internal/" 
  sandbox="allow-scripts allow-same-origin allow-microphone"
  style="width: 400px; height: 100%; border: none;"
  title="医疗 AI 助手"
></iframe>
```

### postMessage 通信协议

```typescript
// his_agent → HIS
interface HISMessage {
  type: 'PATIENT_INFO' | 'SAVE_RECORD' | 'OPEN_RECORD';
  payload: {
    patientId?: string;
    recordData?: MedicalRecord;
    recordId?: string;
  };
}

// HIS → his_agent
interface AgentMessage {
  type: 'PATIENT_UPDATED' | 'RECORD_SAVED' | 'ERROR';
  payload: unknown;
}
```

---

## 项目结构

```
his_agent/
├── src/
│   ├── components/          # React 组件
│   │   ├── VoiceInput/      # 语音录入组件
│   │   ├── DiagnosisPanel/  # 诊断建议面板
│   │   ├── RecordEditor/    # 病历编辑器
│   │   └── KnowledgeBase/   # 医学知识查询
│   ├── hooks/               # React Hooks
│   ├── services/            # 外部服务集成
│   │   ├── llm/             # LLM 抽象层
│   │   ├── speech/          # 语音识别服务
│   │   └── his/             # HIS 通信服务
│   ├── stores/              # 状态管理 (Zustand)
│   ├── types/               # TypeScript 类型定义
│   └── utils/               # 工具函数
├── openspec/
│   ├── config.yaml          # OpenSpec 配置
│   ├── specs/               # 能力规格说明
│   └── changes/             # 变更工件
├── package.json
└── tsconfig.json
```

---

## 下一步

1. **确认技术选型** - 语音识别和 LLM 供应商选择
2. **创建项目脚手架** - 初始化 React + TypeScript + Vite
3. **定义第一个 Spec** - 智能问诊功能详细规格
4. **开发环境搭建** - 本地 LLM (Ollama) 配置
