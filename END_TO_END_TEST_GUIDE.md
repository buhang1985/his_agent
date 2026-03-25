# 端到端语音问诊测试指南

## ✅ 已完成任务

### 1. 更新 tasks.md
- ✅ 反映实际完成状态
- ✅ 添加 LLM 配置和测试任务
- ✅ 添加端到端测试任务

### 2. 修复 BackendProxyVoiceTest.vue
- ✅ 添加三个状态按钮：连接后端 → 开始录音 → 停止录音
- ✅ 完善错误处理和日志显示
- ⏳ 需要后端 WebSocket 服务运行在 `ws://localhost:8080/ws/voice`

### 3. 验证 LLM 功能
- ✅ `SoapNoteGeneratorService.java` 已存在（基于关键词的简化实现）
- ✅ `TestController.java` 提供 `POST /api/test/voice/generate` 接口
- ⏳ 需要配置 Qwen API Key 以实现真实 LLM 调用

### 4. 创建端到端测试页面
- ✅ `VoiceToSoapTest.vue` - 三步骤测试页面
  - 步骤 1: 语音录入（使用成功的 SimpleVoiceTest 方案）
  - 步骤 2: 实时转写（讯飞 ASR）
  - 步骤 3: 病历生成（调用后端 API）
- ✅ 路由：`/test/voice-to-soap`

### 5. LLM API 配置
- ⏳ 等待用户提供 Qwen API Key 和 Base URL
- 📝 当前使用简化版关键词匹配（可测试基本流程）

---

## 🚀 测试步骤

### 方案 A: 简化版（无需 LLM 配置）

**使用场景**: 快速验证端到端流程

1. **启动后端**
```bash
cd /Users/yzh/opencode_workspace/his_agent/his_agent-backend
mvn spring-boot:run
```

2. **启动前端**
```bash
cd /Users/yzh/opencode_workspace/his_agent/his_agent-frontend
npm run dev
```

3. **访问测试页面**
```
http://localhost:5173/test/voice-to-soap
```

4. **测试流程**
   - 点击"开始录音" → 对着麦克风说话
   - 点击"停止录音" → 查看转写文本
   - 点击"生成 SOAP 病历" → 查看简化版病历（基于关键词匹配）

**预期结果**:
- ✅ 语音转写正常（讯飞 ASR）
- ✅ 病历生成正常（简化版，基于关键词）
- ⚠️ 病历内容较简单（等待 LLM 配置后优化）

---

### 方案 B: 完整版（需要 LLM 配置）

**使用场景**: 生产环境，真实 LLM 病历生成

1. **提供 Qwen API 配置**
```bash
# 后端 application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:你的 Qwen API Key}
      base-url: ${OPENAI_BASE_URL:你的 Base URL}
      chat:
        options:
          model: ${LLM_MODEL:qwen-max}
```

2. **重启后端**
```bash
# 停止后端 (Ctrl+C)
mvn spring-boot:run
```

3. **测试完整流程**
- 访问：`http://localhost:5173/test/voice-to-soap`
- 录音 → 转写 → 生成真实 LLM 病历

**预期结果**:
- ✅ 高质量的 SOAP 病历生成
- ✅ 医学术语理解更准确
- ✅ 鉴别诊断建议更专业

---

## 📊 测试页面汇总

| 页面 | 路由 | 用途 | 状态 |
|------|------|------|------|
| **SimpleVoiceTest** | `/test/simple-voice` | 前端直连讯飞（成功 Demo） | ✅ 已验证 |
| **XunfeiVoiceTest** | `/test/xunfei-voice` | 讯飞官方 Demo 参考 | ✅ 可用 |
| **BackendProxyVoiceTest** | `/test/backend-voice` | 后端代理模式 | ⏳ 需要后端 WebSocket |
| **VoiceToSoapTest** | `/test/voice-to-soap` | 端到端测试（语音→病历） | ✅ 已创建 |

---

## 🔧 后端 API 接口

### 生成 SOAP 病历

**接口**: `POST /api/test/voice/generate`

**请求**:
```json
{
  "transcript": "患者主诉头痛、发热 3 天...",
  "department": "general"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": "uuid",
    "soap": {
      "subjective": {
        "chiefComplaint": "头痛、发热",
        "historyOfPresentIllness": "..."
      },
      "objective": {
        "vitalSigns": "体温：38.5°C",
        "physicalExamFindings": "咽部充血"
      },
      "assessment": {
        "primaryDiagnosis": "上呼吸道感染",
        "differentialDiagnoses": ["流感", "新冠肺炎"]
      },
      "plan": {
        "diagnosticTests": ["血常规", "C 反应蛋白"],
        "treatment": "退热治疗",
        "advice": "多饮水，休息"
      }
    },
    "confidence": 0.85,
    "generatedAt": "2026-03-24T14:30:00"
  }
}
```

---

## ⚠️ 注意事项

### 1. 后端 WebSocket 服务
- `VoiceTranscriptionWebSocket.java` 已创建
- 端点：`ws://localhost:8080/ws/voice`
- **需要手动启动后端才能使用**

### 2. LLM 配置
- 当前使用简化版（关键词匹配）
- 生产环境需要配置 Qwen/Claude API
- 提示词模板待优化

### 3. 数据安全
- 录音数据不持久化（内存缓存）
- 生成病历后自动删除
- 符合医疗数据隐私要求

---

## 📝 下一步优化

### 高优先级
1. **配置 Qwen API** - 实现真实 LLM 病历生成
2. **编写提示词模板** - 优化病历生成质量
3. **测试医学专业术语** - 验证识别准确率

### 中优先级
1. **添加病历编辑功能** - 允许医生修改
2. **实现 postMessage** - 回传 HIS 系统
3. **添加诊断建议生成** - 鉴别诊断列表

### 低优先级
1. **组件化重构** - 拆分独立组件
2. **单元测试** - Vitest + JUnit
3. **角色分离** - 医生/患者区分

---

## 🎯 成功标准

- [x] 语音录制成功（SimpleVoiceTest 已验证）
- [x] 实时转写成功（讯飞 ASR 工作正常）
- [x] 端到端流程打通（VoiceToSoapTest 创建）
- [ ] LLM 病历生成高质量（需要 API 配置）
- [ ] 医生可以实际使用（需要 HIS 集成）

---

## 📚 相关文档

- [需求文档](../docs/requirement_spec/01-voice-consultation.md)
- [OpenSpec 变更](../openspec/changes/voice-consultation-test/)
- [讯飞 API 文档](https://www.xfyun.cn/doc/asr/rtasr/API.html)
