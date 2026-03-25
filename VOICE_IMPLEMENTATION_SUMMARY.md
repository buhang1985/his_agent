# 讯飞语音转写双方案实施总结

## 📋 实施内容

### 方案 A：修复前端直连讯飞 RTASR

**问题诊断**:
- ScriptProcessorNode 缓冲大小过小（1024）导致回调不稳定
- ScriptProcessorNode 未连接 destination，可能被垃圾回收
- 调试日志不足，难以定位问题

**修复内容**:
1. 增大 ScriptProcessorNode 缓冲：`1024 → 4096`
2. 连接 destination 防止被垃圾回收（关键修复）
3. 增强调试日志输出

**修改文件**:
- `his_agent-frontend/src/services/speech/IFlytekService.ts` (第 334-336 行)

**测试页面**: `http://localhost:5173/test/simple-voice`

---

### 方案 B：前后端分离架构（后端代理模式）

**架构优势**:
- ✅ 复用后端已验证的 Java SDK
- ✅ 避免前端暴露 API Key
- ✅ 更稳定的音频处理和网络连接
- ✅ 支持降级策略（讯飞 → 阿里云 → Whisper → 手动录入）

**后端实现**:
- 文件：`his_agent-backend/src/main/java/com/hisagent/controller/VoiceTranscriptionWebSocket.java`
- WebSocket 端点：`ws://localhost:8080/ws/voice`
- 使用讯飞官方 Java SDK (`websdk-java-speech v3.0.9`)

**前端实现**:
- 文件：`his_agent-frontend/src/views/BackendProxyVoiceTest.vue`
- 路由：`/test/backend-voice`
- 通过 WebSocket 发送 Base64 编码的 PCM 音频到后端

**通信协议**:

```typescript
// 客户端 → 服务端
{ type: "audio", data: "<Base64 PCM>" }
{ type: "end" }

// 服务端 → 客户端
{ type: "ready", sessionId: "..." }
{ type: "result", text: "...", isFinal: false }
{ type: "error", message: "..." }
```

---

## 🚀 测试步骤

### 1. 启动后端

```bash
cd /Users/yzh/opencode_workspace/his_agent/his_agent-backend
mvn spring-boot:run
```

后端会在 `ws://localhost:8080/ws/voice` 启动 WebSocket 服务。

### 2. 启动前端

```bash
cd /Users/yzh/opencode_workspace/his_agent/his_agent-frontend
npm run dev
```

### 3. 测试方案 A（前端直连）

访问：`http://localhost:5173/test/simple-voice`

**预期结果**:
- 点击"开始录音"后，状态变为"录音中"
- 对着麦克风说话，看到实时转写结果
- 绿色边框 = 最终结果，蓝色边框 = 中间结果

### 4. 测试方案 B（后端代理）

访问：`http://localhost:5173/test/backend-voice`

**预期结果**:
- 点击"开始录音"后，自动连接后端 WebSocket
- 显示"已连接"状态和 Session ID
- 对着麦克风说话，看到实时转写结果

---

## 🔍 故障排查

### 方案 A 常见问题

**问题 1**: 录音马上停止
- 检查浏览器控制台是否有错误日志
- 确认麦克风权限已授予
- 检查 `.env` 文件中的 API Key 是否正确

**问题 2**: 鉴权失败 (错误码 10105/10110)
- 检查 AppID 和 API Key 是否匹配
- 确认实时语音转写服务已开通
- 检查 IP 白名单配置（如已启用）

**问题 3**: 没有转写结果
- 检查浏览器控制台日志
- 确认音频数据正在发送（查看 `onaudioprocess` 日志）
- 检查网络连接是否正常

### 方案 B 常见问题

**问题 1**: WebSocket 连接失败
- 确认后端已启动
- 检查端口 8080 是否被占用
- 查看后端日志是否有错误

**问题 2**: 后端初始化失败
- 检查 `application.yml` 中的讯飞配置
- 确认 Maven 依赖已下载
- 查看后端启动日志

---

## 📊 方案对比

| 特性 | 方案 A（前端直连） | 方案 B（后端代理） |
|------|-------------------|-------------------|
| **延迟** | 低（直接连接） | 中（经过后端） |
| **安全性** | 中（API Key 暴露） | 高（后端保护） |
| **稳定性** | 中（依赖浏览器） | 高（Java SDK） |
| **降级支持** | ❌ 无 | ✅ 支持 |
| **适用场景** | 开发/测试 | 生产环境 |

---

## 🎯 推荐方案

**开发/测试阶段**: 使用方案 A，快速调试
**生产环境**: 使用方案 B，稳定可靠

---

## 📝 下一步优化建议

1. **方案 A**: 迁移到 AudioWorklet API（更现代，但兼容性较差）
2. **方案 B**: 
   - 添加音频压缩（减少带宽）
   - 实现连接池（提高并发能力）
   - 添加录音文件存储功能

---

## 📚 相关文档

- [讯飞 RTASR 官方文档](https://www.xfyun.cn/doc/asr/rtasr/API.html)
- [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API)
- [Spring WebSocket](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
