# API 接口文档

本文档说明 his_agent 的所有 API 接口。

## 后端 API (Spring Boot)

### 接口列表

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 创建问诊 | POST | `/api/v1/consultations` | 创建新的问诊会话 |
| 获取问诊 | GET | `/api/v1/consultations/{id}` | 获取问诊详情 |
| 语音转写 | POST | `/api/v1/consultations/{id}/transcribe` | 上传音频并转写 |
| 生成病历 | POST | `/api/v1/consultations/{id}/generate-soap` | 生成 SOAP 病历 |
| 获取诊断 | GET | `/api/v1/consultations/{id}/diagnosis` | 获取诊断建议 |
| 保存病历 | PUT | `/api/v1/consultations/{id}/soap-note` | 保存病历到 HIS |

### Swagger 文档

启动后端后访问：
```
http://localhost:8080/swagger-ui.html
```

OpenAPI 规范：
```
http://localhost:8080/v3/api-docs
```

### 接口详细说明

详见 [backend/](backend/) 目录下的分接口文档。

---

## 前端 API 调用

### 服务层

前端通过 `src/services/` 目录下的服务层封装 API 调用。

```typescript
// src/services/api.ts
export const api = {
  // 创建问诊
  createConsultation(patientId: string): Promise<ConsultationResponse>
  
  // 语音转写
  transcribeAudio(id: string, audio: Blob): Promise<TranscriptionResponse>
  
  // 生成病历
  generateSoapNote(id: string, transcript: string): Promise<SOAPNoteResponse>
}
```

### API 客户端

使用 Axios 封装的 API 客户端：

```typescript
// src/services/api-client.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use((config) => {
  // 添加认证 token
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 统一错误处理
    return Promise.reject(error);
  }
);
```

---

## WebSocket 接口

### 连接配置

```typescript
const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws';
const socket = new WebSocket(wsUrl);
```

### 消息格式

**客户端 → 服务端**:
```json
{
  "type": "START_TRANSCRIPTION",
  "payload": {
    "consultationId": "uuid"
  }
}
```

**服务端 → 客户端**:
```json
{
  "type": "TRANSCRIPTION_UPDATE",
  "payload": {
    "text": "转写文本",
    "isFinal": false,
    "confidence": 0.95
  }
}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用 |

## 认证

使用 JWT Token 认证：

```
Authorization: Bearer <token>
```

## 速率限制

| 接口 | 限制 |
|------|------|
| 创建问诊 | 100 次/分钟 |
| 语音转写 | 10 次/分钟 |
| 生成病历 | 20 次/分钟 |
