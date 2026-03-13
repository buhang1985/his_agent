# Mock 策略规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：外部服务 Mock 配置

所有外部付费服务在开发和测试环境必须使用 Mock，禁止调用真实 API。

#### 场景：开发环境 Mock
- **当** 在开发环境运行时
- **那么** 必须：
  - 语音识别服务：使用预录制音频的 Mock 响应
  - LLM 服务：使用固定响应模板
  - HIS 集成：使用本地 Mock 服务器
  - 配置项：`external-services.mock=true`

#### 场景：测试环境 Mock
- **当** 运行自动化测试时
- **那么** 必须：
  - 使用 WireMock 模拟所有外部 HTTP 服务
  - 使用 TestContainers 模拟数据库和缓存
  - 录制真实响应作为基础 Mock 数据
  - 模拟各种响应场景（成功、失败、超时、限流）

#### 场景：Mock 数据管理
- **当** 创建 Mock 数据时
- **那么** 必须：
  - Mock 数据存放在 `src/test/resources/mock-data/`
  - 使用 JSON 格式存储响应模板
  - 每个外部服务有独立的 Mock 配置
  - Mock 数据包含正常和异常场景

### 需求：WireMock 集成规范

必须使用 WireMock 模拟外部 HTTP 服务，支持场景化响应。

#### 场景：语音识别 Mock
- **当** 模拟语音识别服务时
- **那么** 必须配置：
```java
// WireMock 配置
stubFor(post(urlEqualTo("/api/v1/asr/transcribe"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBodyJson("""
        {
          "transcript": "患者主诉头痛，持续三天",
          "confidence": 0.95,
          "duration": 5.2
        }
        """)));
```

#### 场景：LLM 服务 Mock
- **当** 模拟 LLM 服务时
- **那么** 必须配置：
```java
stubFor(post(urlEqualTo("/api/v1/llm/generate-soap"))
    .willReturn(aResponse()
        .withStatus(200)
        .withBodyJson("""
        {
          "soapNote": {
            "subjective": {"chiefComplaint": "头痛"},
            "objective": {},
            "assessment": {"primaryDiagnosis": "紧张性头痛"},
            "plan": {"treatment": "休息"}
          }
        }
        """)));
```

#### 场景：错误场景 Mock
- **当** 测试错误处理时
- **那么** 必须模拟：
  - HTTP 429 Too Many Requests（限流）
  - HTTP 503 Service Unavailable（服务不可用）
  - HTTP 504 Gateway Timeout（超时）
  - 延迟响应（模拟网络延迟）

### 需求：环境隔离配置

必须通过环境变量严格隔离各环境的外部服务配置。

#### 场景：环境配置
- **当** 配置外部服务时
- **那么** 必须使用：
```yaml
# application-dev.yml
external-services:
  speech-recognition:
    enabled: false
    mock: true
    mock-data-path: classpath:mock-data/asr/
  
  llm:
    enabled: false
    mock: true
    mock-data-path: classpath:mock-data/llm/

# application-prod.yml
external-services:
  speech-recognition:
    enabled: true
    mock: false
    api-key: ${XUNFEI_API_KEY}
  
  llm:
    enabled: true
    mock: false
    api-key: ${DASHSCOPE_API_KEY}
```

#### 场景：启动检查
- **当** 应用启动时
- **那么** 必须检查：
  - 生产环境禁止开启 Mock
  - 开发环境禁止配置真实 API Key
  - 配置冲突时启动失败并报错

### 需求：Mock 数据生成器

必须提供 Mock 数据生成工具，支持动态生成测试数据。

#### 场景：动态 Mock 生成
- **当** 需要大量测试数据时
- **那么** 必须支持：
  - 基于 Faker 库生成随机患者数据
  - 基于模板生成随机病历
  - 支持批量生成（1000+ 条记录）
  - 生成的数据符合医疗数据规范
