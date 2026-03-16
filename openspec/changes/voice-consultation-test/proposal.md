## 为什么

his_agent 需要通过实际的语音录入测试来验证讯飞 ASR 集成是否可用，以及 LLM 病历生成功能是否正常工作。当前项目已配置好技术栈和讯飞 API 凭证，但缺少端到端的测试用例来验证整个语音问诊流程。

## 变更内容

创建一个完整的语音录入测试页面，实现从语音采集 → ASR 转写 → LLM 病历生成 → 编辑展示的完整流程。

**新增功能**:
- 前端语音录入测试页面（TestPage.vue）
- 前端讯飞 ASR 语音识别服务（IFlytekService.ts）
- 后端语音转写和病历生成 REST API
- 后端 LLM 病历生成服务（基于 Spring AI）
- 测试控制器和接口

**修改功能**:
- 无（纯新增功能，不影响现有代码）

## 功能 (Capabilities)

### 新增功能
- `voice-test-page`: 前端语音录入测试页面，包含录音控制、转写展示、病历编辑功能
- `iflytek-asr-service`: 前端讯飞 ASR WebSocket 服务，实现实时语音识别
- `voice-consultation-api`: 后端语音转写和病历生成 REST API 接口
- `llm-soap-generator`: 后端基于 Spring AI 的 SOAP 病历生成服务

### 修改功能
<!-- 无 -->

## 影响

**前端**:
- 新增 `his_agent-frontend/src/views/TestPage.vue`
- 新增 `his_agent-frontend/src/services/speech/IFlytekService.ts`
- 新增 `his_agent-frontend/src/composables/useMedicalSpeechRecognition.ts`
- 新增路由配置 `/test/voice`

**后端**:
- 新增 `his_agent-backend/src/main/java/com/hisagent/controller/TestController.java` (已存在，扩展功能)
- 新增 `his_agent-backend/src/main/java/com/hisagent/service/voice/VoiceRecognitionService.java`
- 新增 `his_agent-backend/src/main/java/com/hisagent/service/llm/SoapNoteGeneratorService.java`
- 新增 `his_agent-backend/src/main/java/com/hisagent/dto/voice/` 数据传输对象

**配置**:
- 更新 `application.yml` 添加讯飞 ASR 配置
- 更新 `.env` 添加前端讯飞 API 配置

**依赖**:
- 前端：`crypto-js` (签名生成)
- 后端：`spring-boot-starter-websocket` (已存在)

**非目标 (Non-Goals)**:
- ❌ 不包含角色分离（医生/患者区分）
- ❌ 不包含说话人识别
- ❌ 不包含 HIS 系统集成（postMessage）
- ❌ 不包含诊断建议生成
- ❌ 不包含病历保存到数据库

**技术选型**:
- **ASR**: 讯飞医疗 ASR（主方案）→ 阿里云 ASR（备用）→ Whisper（降级）
- **LLM**: Spring AI 统一接口，支持 Qwen/Claude/Ollama 切换
- **通信**: WebSocket 实时音频流 + REST API 病历生成
