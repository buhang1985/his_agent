## 1. 前端基础设置

- [x] 1.1 安装 crypto-js 依赖（npm install crypto-js）
- [x] 1.2 配置前端环境变量（.env 添加讯飞 API 凭证）
- [x] 1.3 更新路由配置添加测试页面路由

## 2. 前端语音识别服务

- [x] 2.1 创建 IFlytekService.ts（讯飞 ASR WebSocket 客户端）✅ 已验证可用
- [x] 2.2 创建 useMedicalSpeechRecognition.ts（语音识别 Composable）
- [x] 2.3 创建 SOAP 病历类型定义（types/voice.ts）

## 3. 前端测试页面

- [x] 3.1 创建 SimpleVoiceTest.vue（语音录入测试页面）✅ 成功 demo，已备份
- [x] 3.2 创建 XunfeiVoiceTest.vue（讯飞官方 demo 参考实现）
- [x] 3.3 创建 BackendProxyVoiceTest.vue（后端代理模式测试页面）
- [ ] 3.4 创建端到端测试页面（语音→转写→病历生成）

## 4. 后端基础设置

- [x] 4.1 更新 application.yml 添加讯飞 ASR 配置
- [x] 4.2 添加 WebSocket 依赖（spring-boot-starter-websocket）
- [x] 4.3 创建语音相关 DTO 类（GenerateSoapRequest, GenerateSoapResponse, SoapNoteDTO）

## 5. 后端语音识别服务

- [x] 5.1 创建 VoiceTranscriptionWebSocket.java（WebSocket 端点，前端直连讯飞模式）
- [x] 5.2 创建 VoiceRecognitionFallback.java（降级策略）
- [x] 5.3 创建 SoapNoteGeneratorService.java（病历生成服务）

## 6. 后端测试接口

- [ ] 6.1 创建 TestController.java 或 VoiceController.java
- [ ] 6.2 创建 POST /api/v1/voice/generate 接口（LLM 病历生成）
- [ ] 6.3 添加 Swagger 文档注解

## 7. LLM 配置与病历生成

- [ ] 7.1 配置 Spring AI（Qwen/Claude/Ollama）
- [ ] 7.2 编写 SOAP 病历生成提示词模板
- [ ] 7.3 测试 LLM 病历生成功能
- [ ] 7.4 验证端到端流程（语音→转写→病历）

## 8. 前端病历展示与编辑

- [ ] 8.1 创建病历展示界面
- [ ] 8.2 创建病历编辑表单
- [ ] 8.3 实现保存回 HIS（postMessage）

## 9. 测试验证

- [ ] 9.1 前端单元测试（Vitest）
- [ ] 9.2 后端单元测试（JUnit 5）
- [x] 9.3 端到端手动测试（录音→转写）✅ SimpleVoiceTest.vue 已验证
- [ ] 9.4 端到端手动测试（录音→转写→病历生成）
- [ ] 9.5 编写测试报告文档
