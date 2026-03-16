## 1. 前端基础设置

- [ ] 1.1 安装 crypto-js 依赖（npm install crypto-js）
- [ ] 1.2 配置前端环境变量（.env 添加讯飞 API 凭证）
- [ ] 1.3 更新路由配置添加测试页面路由

## 2. 前端语音识别服务

- [ ] 2.1 创建 IFlytekService.ts（讯飞 ASR WebSocket 客户端）
- [ ] 2.2 创建 useMedicalSpeechRecognition.ts（语音识别 Composable）
- [ ] 2.3 创建 SOAP 病历类型定义

## 3. 前端测试页面

- [ ] 3.1 创建 TestPage.vue（语音录入测试页面）
- [ ] 3.2 创建 VoiceInput.vue（录音控制组件）
- [ ] 3.3 创建 TranscriptView.vue（转写文本展示组件）
- [ ] 3.4 创建 SOAPNoteEditor.vue（病历编辑组件）

## 4. 后端基础设置

- [ ] 4.1 更新 application.yml 添加讯飞 ASR 配置
- [ ] 4.2 添加 WebSocket 依赖（如未安装）
- [ ] 4.3 创建语音相关 DTO 类

## 5. 后端语音识别服务

- [ ] 5.1 创建 IFlytekAsrClient.java（讯飞 ASR WebSocket 客户端）
- [ ] 5.2 创建 VoiceRecognitionService.java（语音识别服务）
- [ ] 5.3 创建 SoapNoteGeneratorService.java（病历生成服务）

## 6. 后端测试接口

- [ ] 6.1 扩展 TestController.java 添加语音相关接口
- [ ] 6.2 创建 POST /api/v1/voice/generate 接口
- [ ] 6.3 添加 Swagger 文档注解

## 7. 测试验证

- [ ] 7.1 前端单元测试（Vitest）
- [ ] 7.2 后端单元测试（JUnit 5）
- [ ] 7.3 端到端手动测试（录音→转写→病历生成）
- [ ] 7.4 编写测试报告文档
