# Qwen LLM 配置完成指南

## ✅ 配置已更新

### 1. application.yml 配置

```yaml
spring:
  ai:
    openai:
      api-key: sk-sp-758abbba5db646c79da0572e8e694b5e
      base-url: https://coding.dashscope.aliyuncs.com/v1
      chat:
        enabled: true
        options:
          model: qwen3.5-plus
          temperature: 0.7
```

### 2. SoapNoteGeneratorService 重写

**新功能**:
- ✅ 调用 Qwen3.5-plus LLM API 生成 SOAP 病历
- ✅ 使用专业医疗提示词模板
- ✅ 解析 JSON 格式的病历结果
- ✅ 降级策略（LLM 失败时切换到关键词匹配）

**提示词模板**:
```
你是一位经验丰富的临床医生，擅长根据医患对话生成结构化病历（SOAP 格式）。

【要求】
1. 严格按照 SOAP 格式组织内容
2. 使用专业医学术语
3. 保持客观、准确
4. 不确定的信息标注"待确认"
5. 识别医学术语和症状
6. 提供合理的鉴别诊断建议
```

---

## 🚀 测试步骤

### 步骤 1: 重启后端

```bash
cd /Users/yzh/opencode_workspace/his_agent/his_agent-backend
mvn spring-boot:run
```

**验证后端启动**:
```bash
curl http://localhost:8080/actuator/health
# 应返回：{"status":"UP",...}
```

---

### 步骤 2: 访问端到端测试页面

```
http://localhost:5173/test/voice-to-soap
```

---

### 步骤 3: 完整测试流程

#### ① 点击"开始录音"
- 允许麦克风权限
- 状态变为"录音中"

#### ② 对着麦克风说话
**测试台词示例**:
> "医生，我头痛，发烧 3 天了，体温最高到 39 度，还有点咳嗽。"

或

> "医生您好，我这两天肚子痛，拉肚子，一天跑好几次厕所。"

#### ③ 点击"停止录音"
- 查看转写文本是否正确

#### ④ 点击"生成 SOAP 病历"
- 等待 2-5 秒（LLM 处理时间）
- 查看生成的结构化病历

---

## 📊 预期结果

### 简化版（之前）
```json
{
  "subjective": {
    "chiefComplaint": "待补充",
    "historyOfPresentIllness": "待补充"
  },
  "objective": {
    "vitalSigns": "待测量",
    "physicalExamFindings": "待检查"
  }
}
```

### LLM 版（现在）✨
```json
{
  "subjective": {
    "chiefComplaint": "头痛、发热、咳嗽",
    "historyOfPresentIllness": "患者 3 天前无明显诱因出现头痛，伴发热，体温最高 39°C，伴咳嗽，咳少量白痰。"
  },
  "objective": {
    "vitalSigns": "体温：39°C，脉搏：90 次/分，呼吸：20 次/分，血压：120/80 mmHg",
    "physicalExamFindings": "咽部充血，扁桃体 I 度肿大，双肺呼吸音粗，未闻及干湿啰音"
  },
  "assessment": {
    "primaryDiagnosis": "急性上呼吸道感染",
    "differentialDiagnoses": ["流行性感冒", "急性支气管炎", "新冠肺炎"]
  },
  "plan": {
    "diagnosticTests": ["血常规", "C 反应蛋白", "流感病毒检测", "胸部 X 线"],
    "treatment": "1. 退热治疗：布洛芬 200mg 口服 每 6 小时一次（体温>38.5°C）\n2. 止咳化痰：氨溴索 30mg 口服 每日 3 次\n3. 抗病毒治疗：奥司他韦 75mg 口服 每日 2 次（如确诊流感）",
    "advice": "1. 多饮水，充分休息\n2. 清淡饮食，避免辛辣刺激\n3. 如有呼吸困难、持续高热不退，及时就医\n4. 居家隔离，佩戴口罩"
  }
}
```

---

## 🔍 调试技巧

### 查看 LLM 调用日志

后端日志应显示:
```
INFO  - 开始生成 SOAP 病历，转写长度：XX，使用模型：qwen3.5-plus
DEBUG - 调用 LLM API: https://coding.dashscope.aliyuncs.com/v1/chat/completions
DEBUG - LLM 原始响应：{...}
✅ SOAP 病历生成成功
```

### 常见问题排查

**问题 1**: 病历生成超时
- 检查网络连接
- 确认 API Key 是否正确
- 检查 Qwen 服务状态

**问题 2**: 返回"待确认"内容
- 这是正常的，LLM 会标注不确定的信息
- 医生可以在编辑功能中修改（待实现）

**问题 3**: 降级到关键词匹配
- 查看日志中的错误信息
- 可能是 API Key 无效或网络问题

---

## 📝 测试用例

### 测试用例 1: 上呼吸道感染

**输入**:
> "医生，我头痛，发烧 3 天了，体温最高到 39 度，还有点咳嗽，咳白痰。"

**预期**:
- 主诉：头痛、发热、咳嗽
- 诊断：急性上呼吸道感染
- 检查：血常规、C 反应蛋白

---

### 测试用例 2: 急性胃肠炎

**输入**:
> "医生，我肚子痛，拉肚子，一天跑好几次厕所，昨天吃了海鲜。"

**预期**:
- 主诉：腹痛、腹泻
- 诊断：急性胃肠炎
- 检查：大便常规、血常规

---

### 测试用例 3: 心绞痛

**输入**:
> "医生，我胸口痛，特别是爬楼梯的时候，休息一会儿就好转。"

**预期**:
- 主诉：胸痛、胸闷
- 诊断：冠心病？心绞痛？
- 检查：心电图、心肌酶谱

---

## 🎯 质量评估标准

| 指标 | 目标 | 评估方法 |
|------|------|---------|
| **SOAP 格式正确性** | 100% | 检查 JSON 结构 |
| **医学术语准确性** | ≥90% | 医生评估 |
| **鉴别诊断合理性** | ≥80% | 医生评估 |
| **响应时间** | <10 秒 | 计时测试 |
| **API 调用成功率** | ≥95% | 日志统计 |

---

## 📚 下一步优化

### 高优先级
1. **添加病历编辑功能** - 允许医生修改 LLM 生成的内容
2. **优化提示词** - 根据测试结果调整提示词模板
3. **添加 few-shot 示例** - 提供高质量病历示例给 LLM

### 中优先级
1. **实现 postMessage** - 回传 HIS 系统
2. **添加诊断建议生成** - 独立的鉴别诊断面板
3. **支持多轮对话** - 问诊过程中的追问

### 低优先级
1. **组件化重构** - 拆分独立组件
2. **单元测试** - Vitest + JUnit
3. **角色分离** - 医生/患者区分

---

## 🔗 相关资源

- [Qwen API 文档](https://help.aliyun.com/zh/dashscope/developer-reference/api-details)
- [讯飞 ASR 文档](https://www.xfyun.cn/doc/asr/rtasr/API.html)
- [SOAP 病历规范](https://en.wikipedia.org/wiki/SOAP_note)

---

## ✅ 完成检查清单

- [ ] 后端编译成功
- [ ] 后端启动成功
- [ ] 访问测试页面成功
- [ ] 语音转写成功
- [ ] LLM 病历生成成功
- [ ] 病历质量评估通过

---

**准备就绪！现在可以开始测试了！** 🎉
