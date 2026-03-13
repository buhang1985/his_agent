# 监控告警规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：关键业务指标监控

必须监控核心业务指标，反映系统健康状况。

#### 场景：问诊业务指标
- **当** 监控问诊业务时
- **那么** 必须监控：
```yaml
metrics:
  business:
    consultations:
      - name: consultations_per_minute
        description: 每分钟问诊会话数
        type: counter
        
      - name: speech_recognition_success_rate
        description: 语音识别成功率
        type: gauge
        target: "> 95%"
        
      - name: llm_generation_latency_p99
        description: LLM 病历生成 P99 延迟
        type: histogram
        target: "< 10s"
        
      - name: soap_note_generation_count
        description: SOAP 病历生成数量
        type: counter
```

#### 场景：HIS 集成指标
- **当** 监控 HIS 集成时
- **那么** 必须监控：
  - his_integration_success_rate: HIS 同步成功率
  - his_integration_latency_p95: HIS 调用 P95 延迟
  - his_integration_error_count: HIS 集成错误数
  - postmessage_send_count: postMessage 发送次数

#### 场景：语音识别指标
- **当** 监控语音识别时
- **那么** 必须监控：
  - asr_request_count: 语音识别请求数
  - asr_success_rate: 识别成功率
  - asr_latency_p95: 识别延迟 P95
  - asr_provider_fallback_count: 降级次数

### 需求：技术指标监控

必须监控基础设施和技术组件指标。

#### 场景：应用指标
- **当** 监控应用时
- **那么** 必须监控：
```yaml
metrics:
  application:
    jvm:
      - jvm_memory_used: JVM 内存使用率
      - jvm_gc_count: GC 次数
      - jvm_thread_count: 线程数
      
    http:
      - http_requests_total: 总请求数
      - http_requests_active: 活跃请求数
      - http_request_duration_seconds: 请求延迟
      
    database:
      - db_connection_pool_active: 活跃连接数
      - db_connection_pool_idle: 空闲连接数
      - db_query_duration_seconds: 查询延迟
      - db_slow_query_count: 慢查询数
```

#### 场景：缓存指标
- **当** 监控缓存时
- **那么** 必须监控：
  - redis_connection_count: Redis 连接数
  - redis_command_duration: Redis 命令延迟
  - cache_hit_rate: 缓存命中率（目标 > 80%）
  - cache_eviction_count: 缓存淘汰数

### 需求：告警阈值定义

必须为每个关键指标定义告警阈值。

#### 场景：P0 级告警（严重）
- **当** 发生严重问题时
- **那么** 必须触发 P0 告警：
```yaml
alerts:
  p0:
    - name: 系统不可用
      metric: http_requests_active
      condition: "== 0"
      window: 2m
      notify: [sms, phone, slack]
      
    - name: 语音识别完全失败
      metric: speech_recognition_success_rate
      condition: "< 50%"
      window: 5m
      notify: [sms, phone, slack]
      
    - name: 数据库连接池耗尽
      metric: db_connection_pool_active
      condition: "> 95%"
      window: 2m
      notify: [sms, phone, slack]
```

#### 场景：P1 级告警（高优先级）
- **当** 发生高优先级问题时
- **那么** 必须触发 P1 告警：
```yaml
alerts:
  p1:
    - name: 语音识别失败率过高
      metric: speech_recognition_success_rate
      condition: "< 90%"
      window: 5m
      notify: [slack, email]
      
    - name: LLM 延迟过高
      metric: llm_generation_latency_p99
      condition: "> 15s"
      window: 10m
      notify: [slack, email]
      
    - name: HIS 集成失败率高
      metric: his_integration_success_rate
      condition: "< 95%"
      window: 10m
      notify: [slack, email]
```

#### 场景：P2 级告警（中优先级）
- **当** 发生中优先级问题时
- **那么** 必须触发 P2 告警：
```yaml
alerts:
  p2:
    - name: 缓存命中率低
      metric: cache_hit_rate
      condition: "< 70%"
      window: 30m
      notify: [slack]
      
    - name: JVM 内存使用率高
      metric: jvm_memory_used
      condition: "> 85%"
      window: 10m
      notify: [slack]
      
    - name: 慢查询增多
      metric: db_slow_query_count
      condition: "> 10/min"
      window: 15m
      notify: [slack]
```

### 需求：告警通知策略

必须根据告警级别配置不同的通知渠道。

#### 场景：通知渠道
- **当** 发送告警通知时
- **那么** 必须：
```yaml
notifications:
  channels:
    sms:
      providers: [阿里云 SMS, 腾讯云 SMS]
      rate_limit: 10/hour  # 防止短信轰炸
      
    phone:
      providers: [阿里云语音，Twilio]
      rate_limit: 5/hour
      
    slack:
      webhook: ${SLACK_WEBHOOK_URL}
      channel: "#his-agent-alerts"
      
    email:
      smtp: ${SMTP_SERVER}
      recipients: [dev-team@hospital.com]
```

#### 场景：告警升级
- **当** 告警未处理时
- **那么** 必须升级：
  - P0 告警：15 分钟未响应 → 升级给主管
  - P1 告警：1 小时未响应 → 升级给主管
  - P2 告警：4 小时未响应 → 升级给主管

#### 场景：告警抑制
- **当** 发生告警风暴时
- **那么** 必须：
  - 相同告警 5 分钟内只发送 1 次
  - 系统维护期间暂停告警
  - 依赖服务不可用时抑制下游告警

### 需求：告警仪表板

必须提供可视化仪表板展示监控指标。

#### 场景：仪表板内容
- **当** 展示监控数据时
- **那么** 必须包含：
  - 系统概览（请求量、错误率、延迟）
  - 业务指标（问诊量、识别成功率）
  - 资源使用（CPU、内存、磁盘）
  - 告警列表（当前活跃告警）
  - 趋势图表（24 小时/7 天/30 天）

#### 场景：仪表板访问
- **当** 访问仪表板时
- **那么** 必须：
  - 支持移动端查看
  - 支持自动刷新（30 秒）
  - 支持时间范围选择
  - 支持仪表板分享
