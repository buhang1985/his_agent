# 压测策略规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：压测环境隔离

压测必须在独立环境执行，禁止调用真实外部付费服务。

#### 场景：外部服务 Mock
- **当** 执行压测时
- **那么** 必须：
  - 所有外部服务（语音识别、LLM、HIS）使用 Mock
  - Mock 服务响应延迟模拟真实服务（50-500ms）
  - Mock 服务按比例返回错误（模拟真实失败率）
  - 禁止配置任何真实 API Key

#### 场景：测试数据准备
- **当** 准备压测数据时
- **那么** 必须：
  - 使用 Faker 生成脱敏测试数据
  - 数据量级与生产相当（≥ 10 万患者）
  - 禁止使用真实患者数据
  - 压测后自动清理所有测试数据

#### 场景：环境配置
- **当** 配置压测环境时
- **那么** 必须：
```yaml
# application-loadtest.yml
load-testing:
  enabled: true
  external-services-mocked: true
  data-anonymized: true
  rate-limit-disabled: true  # 压测时禁用限流
  
external-services:
  speech-recognition:
    mock: true
    mock-latency-ms: 100
    mock-error-rate: 0.01
  
  llm:
    mock: true
    mock-latency-ms: 500
    mock-error-rate: 0.02
```

### 需求：压测场景设计

必须设计多种压测场景，覆盖不同业务情况。

#### 场景：日常门诊压测
- **当** 模拟日常门诊高峰时
- **那么** 必须：
  - 并发用户数：50
  - 持续时间：30 分钟
  - 场景分布：
    - 70% 语音录入 + 转写
    - 20% 病历生成
    - 10% 病历查询
  - 目标：P99 响应时间 < 2s

#### 场景：极端高峰压测
- **当** 模拟极端高峰（如疫情期间）时
- **那么** 必须：
  - 并发用户数：200
  - 持续时间：60 分钟
  - 场景分布：
    - 50% 语音录入 + 转写
    - 40% 病历生成
    - 10% 病历查询
  - 目标：系统不崩溃，P99 < 5s

#### 场景：压力极限压测
- **当** 测试系统极限时
- **那么** 必须：
  - 并发用户数：500（递增）
  - 持续时间：直到系统达到瓶颈
  - 监控指标：
    - CPU 使用率
    - 内存使用率
    - 数据库连接池
    - 请求队列长度
  - 目标：找到系统瓶颈和最大承载量

### 需求：压测工具规范

必须使用标准化压测工具，支持自动化执行。

#### 场景：压测脚本
- **当** 编写压测脚本时
- **那么** 必须使用 k6：
```javascript
// load-tests/consultation.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  stages: [
    { duration: '5m', target: 50 },   // 5 分钟预热到 50 用户
    { duration: '30m', target: 50 },  // 30 分钟稳定负载
    { duration: '5m', target: 100 },  // 5 分钟增加到 100 用户
    { duration: '10m', target: 0 },   // 10 分钟冷却
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% 请求 < 500ms
    http_req_failed: ['rate<0.01'],    // 错误率 < 1%
  },
};

export default function () {
  const patientId = uuidv4();
  
  // 创建问诊会话
  const createRes = http.post(
    'http://localhost:8080/api/v1/consultations',
    JSON.stringify({ patientId }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  check(createRes, {
    'create status is 201': (r) => r.status === 201,
  });
  
  sleep(1);
}
```

#### 场景：压测报告
- **当** 生成压测报告时
- **那么** 必须包含：
  - 执行摘要（通过/失败）
  - 性能指标（P50/P95/P99）
  - 资源使用率（CPU、内存、网络）
  - 瓶颈分析
  - 优化建议
  - 与上次压测对比

### 需求：压测执行规范

压测执行必须遵循标准流程，保证结果可靠性。

#### 场景：压测前检查
- **当** 开始压测前
- **那么** 必须检查：
  - 所有外部服务已 Mock
  - 测试数据已准备
  - 监控告警已配置
  - 压测环境隔离确认
  - 相关人员已通知

#### 场景：压测中监控
- **当** 压测执行中
- **那么** 必须监控：
  - 系统资源（CPU、内存、磁盘、网络）
  - 应用指标（QPS、响应时间、错误率）
  - 数据库指标（连接数、慢查询、锁等待）
  - 中间件指标（缓存命中率、消息队列堆积）

#### 场景：压测后清理
- **当** 压测完成后
- **那么** 必须：
  - 删除所有测试数据
  - 清理压测产生的日志
  - 恢复环境配置
  - 生成压测报告
  - 归档压测结果
