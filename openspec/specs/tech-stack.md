# 技术栈规格说明

**版本**: 2.0  
**日期**: 2026-03-10  
**状态**: 已批准  
**变更**: 调整为 Java + 前后端分离架构

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        HIS 宿主系统                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    iframe 嵌入区域                       │    │
│  │  ┌───────────────────────────────────────────────────┐  │    │
│  │  │     his_agent Frontend (React + TypeScript)       │  │    │
│  │  │  ┌─────────┐ ┌─────────┐ ┌─────────────────────┐  │  │    │
│  │  │  │ 语音录入 │ │ 诊断建议 │ │   病历生成/展示     │  │  │    │
│  │  │  └─────────┘ └─────────┘ └─────────────────────┘  │  │    │
│  │  └───────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                    REST API / WebSocket                          │
└──────────────────────────────┼──────────────────────────────────┘
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
           ┌────────────────┐   ┌────────────────┐
           │  Spring Boot   │   │  本地 LLM 服务  │
           │  后端服务       │   │ (Ollama/vLLM)  │
           └────────────────┘   └────────────────┘
```

---

## 核心栈

### 后端

| 层级 | 技术 | 版本 | 理由 |
|------|------|------|------|
| 语言 | Java | 17+ | LTS 版本，企业级支持 |
| 框架 | Spring Boot | 3.4.x | 约定优于配置，生态完善 |
| LLM 集成 | Spring AI | 1.0.0-M5+ | 统一 LLM 接口，支持多 provider |
| 构建工具 | Maven | 3.9+ | 依赖管理成熟 |
| 数据库 | MySQL | 8+ | 关系型数据存储 |
| 迁移工具 | Flyway | 10+ | 版本化数据库迁移 |
| API 文档 | SpringDoc OpenAPI | 2.7+ | Swagger UI 自动生成 |

### 前端

| 层级 | 技术 | 版本 | 理由 |
|------|------|------|------|
| 语言 | TypeScript | 5+ | 类型安全 |
| 框架 | React | 18+ | 组件化开发 |
| 构建工具 | Vite | 6+ | 快速开发体验 |
| 状态管理 | Zustand | 5+ | 轻量级状态管理 |

### 外部服务

| 服务 | 技术 | 部署方式 |
|------|------|----------|
| 语音识别 | Deepgram Nova-3 Medical | 云端 API |
| 语音识别 | Whisper.cpp | 本地部署 |
| LLM | Qwen (通义千问) | 阿里云 API |
| LLM | Claude | Anthropic API |
| LLM | Qwen2.5/Llama3 | Ollama 本地部署 |
| 向量检索 | Milvus | 可选，用于 RAG |

---

## 后端架构

### 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      Controller Layer                            │
│  - REST API 端点                                                 │
│  - WebSocket 处理器                                              │
│  - 请求验证                                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                              │
│  - 业务逻辑                                                      │
│  - 事务管理                                                      │
│  - AI 服务编排                                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Repository Layer                            │
│  - 数据访问 (JPA)                                                │
│  - 数据库操作                                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 核心模块

```
com.hisagent/
├── controller/          # REST 控制器
│   ├── ConsultationController.java
│   ├── SpeechController.java
│   └── HisIntegrationController.java
├── service/             # 业务服务
│   ├── ConsultationService.java
│   ├── SoapNoteGenerationService.java
│   └── DiagnosisSuggestionService.java
├── repository/          # 数据访问
│   ├── ConsultationRepository.java
│   └── PatientRepository.java
├── model/               # JPA 实体
│   ├── Consultation.java
│   └── SoapNote.java
├── dto/                 # 数据传输对象
│   ├── request/
│   └── response/
├── ai/                  # AI 服务集成
│   ├── LlmProviderRegistry.java
│   ├── SoapNoteGenerator.java
│   └── DiagnosisSuggester.java
├── config/              # 配置类
│   ├── SecurityConfig.java
│   └── WebConfig.java
└── exception/           # 异常处理
    ├── GlobalExceptionHandler.java
    └── BusinessException.java
```

---

## API 设计

### RESTful API 规范

```yaml
openapi: 3.0.3
info:
  title: his_agent API
  version: 0.1.0

paths:
  /api/v1/consultations:
    post:
      summary: 创建问诊会话
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateConsultationRequest'
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ConsultationResponse'
    
    get:
      summary: 获取问诊列表
      parameters:
        - name: patientId
          in: query
          schema:
            type: string
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/ConsultationResponse'

  /api/v1/consultations/{id}/transcribe:
    post:
      summary: 语音转写
      requestBody:
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                audio:
                  type: string
                  format: binary
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TranscriptionResponse'

  /api/v1/consultations/{id}/generate-soap:
    post:
      summary: 生成 SOAP 病历
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SOAPNoteResponse'
```

### WebSocket 通信

```java
// WebSocket 配置示例
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

---

## 前端架构

### 组件结构

```
his_agent-frontend/
├── src/
│   ├── components/
│   │   └── VoiceConsultation/
│   │       ├── VoiceConsultation.tsx    # 主容器
│   │       ├── VoiceInput.tsx           # 语音录入
│   │       ├── TranscriptView.tsx       # 转写展示
│   │       ├── SOAPNoteEditor.tsx       # 病历编辑
│   │       ├── DiagnosisPanel.tsx       # 诊断建议
│   │       └── PatientInfoBanner.tsx    # 患者信息
│   ├── hooks/
│   │   ├── useVoiceConsultation.ts
│   │   ├── useMedicalSpeechRecognition.ts
│   │   └── useSOAPNoteGenerator.ts
│   ├── services/
│   │   ├── api.ts              # API 客户端
│   │   ├── speech.ts           # 语音服务
│   │   └── websocket.ts        # WebSocket 服务
│   ├── stores/
│   │   └── consultationStore.ts # Zustand 状态
│   └── types/
│       └── medical.ts          # 医疗类型定义
```

### API 客户端

```typescript
// src/services/api.ts
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const api = {
  // 创建问诊会话
  async createConsultation(patientId: string) {
    const response = await fetch(`${API_BASE_URL}/v1/consultations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ patientId }),
    });
    return response.json();
  },
  
  // 语音转写
  async transcribeAudio(consultationId: string, audioBlob: Blob) {
    const formData = new FormData();
    formData.append('audio', audioBlob);
    
    const response = await fetch(
      `${API_BASE_URL}/v1/consultations/${consultationId}/transcribe`,
      { method: 'POST', body: formData }
    );
    return response.json();
  },
  
  // 生成 SOAP 病历
  async generateSoapNote(consultationId: string, transcript: string) {
    const response = await fetch(
      `${API_BASE_URL}/v1/consultations/${consultationId}/generate-soap`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ transcript }),
      }
    );
    return response.json();
  },
};
```

---

## 数据模型

### 核心实体

```java
// Consultation.java - 问诊会话实体
@Entity
@Table(name = "consultations")
public class Consultation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String patientId;
    
    @Enumerated(EnumType.STRING)
    private ConsultationStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String transcript;
    
    @OneToOne(cascade = CascadeType.ALL)
    private SoapNote soapNote;
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<DiagnosisSuggestion> diagnosisSuggestions;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
// SoapNote.java - SOAP 病历实体
@Entity
@Table(name = "soap_notes")
public class SoapNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(columnDefinition = "JSON")
    private SubjectiveData subjective;
    
    @Column(columnDefinition = "JSON")
    private ObjectiveData objective;
    
    @Column(columnDefinition = "JSON")
    private AssessmentData assessment;
    
    @Column(columnDefinition = "JSON")
    private PlanData plan;
    
    private Boolean reviewed = false;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
}
```

---

## 安全与合规

### Spring Security 配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws SecurityException {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/ws/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

### 数据安全

| 措施 | 实现 |
|------|------|
| **传输加密** | HTTPS/TLS |
| **认证授权** | JWT + OAuth2 |
| **审计日志** | Spring AOP 拦截 |
| **PII 脱敏** | 数据脱敏过滤器 |
| **紧急重定向** | 关键词检测拦截器 |

---

## 部署架构

### 开发环境

```
┌──────────────┐     ┌──────────────┐
│   Frontend   │     │   Backend    │
│   (Vite)     │────▶│ (Spring Boot)│
│ localhost:3k │     │ localhost:8k │
└──────────────┘     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │    MySQL     │
                     │  localhost   │
                     └──────────────┘
```

### 生产环境

```
┌──────────────┐     ┌──────────────┐
│   Nginx      │     │   Frontend   │
│   (Reverse   │────▶│   (Static)   │
│    Proxy)    │     │              │
└──────┬───────┘     └──────────────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐
│  Spring Boot │────▶│    MySQL     │
│  (Cluster)   │     │  (Primary)   │
└──────┬───────┘     └──────────────┘
       │
       ▼
┌──────────────┐
│    Ollama    │
│  (Local LLM) │
└──────────────┘
```

---

## 开发环境搭建

### 后端

```bash
cd his_agent-backend

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件

# 启动 MySQL (Docker)
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=his_agent \
  -p 3306:3306 \
  mysql:8

# 构建并启动
mvn clean install
mvn spring-boot:run
```

### 前端

```bash
cd his_agent-frontend

# 安装依赖
npm install

# 配置环境变量
cp .env.example .env

# 启动开发服务器
npm run dev
```

---

## 参考资料

- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot 最佳实践](https://spring.io/projects/spring-boot)
- [Vercel AI SDK](https://sdk.vercel.ai/docs)
- [Deepgram Medical](https://deepgram.com/solutions/medical-transcription)
