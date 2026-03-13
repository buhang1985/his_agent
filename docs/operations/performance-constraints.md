# 性能约束配置规范

## 1. 并发限制配置

### 1.1 全局并发限制
通过 Resilience4j RateLimiter 实现：
```yaml
resilience4j.ratelimiter.instances.default:
  limitForPeriod: 100        # 全局每秒最大请求数
  limitRefreshPeriod: 60s    # 刷新周期
  timeoutDuration: 0s        # 超时立即拒绝
```

### 1.2 单用户并发限制
基于用户 ID 的限流（通过 Redis 实现）：
```yaml
resilience4j.ratelimiter.instances.userLevel:
  limitForPeriod: 20         # 单用户每秒最大请求数
  limitRefreshPeriod: 60s
  timeoutDuration: 5s        # 超时等待 5 秒
```

### 1.3 接口级并发限制
针对特定 API 路径的限流：
```yaml
resilience4j.ratelimiter.instances.apiLevel:
  limitForPeriod: 50         # 接口每秒最大请求数
  limitRefreshPeriod: 60s
```

## 2. 外部调用超时时间

### 2.1 语音识别服务
```yaml
# 语音服务超时配置
app.voice-service:
  connect-timeout: 5000      # 连接超时 5 秒
  read-timeout: 30000        # 读取超时 30 秒
  write-timeout: 30000       # 写入超时 30 秒
```

### 2.2 LLM 服务
```yaml
# LLM 服务超时配置
app.llm-service:
  connect-timeout: 10000     # 连接超时 10 秒
  read-timeout: 60000        # 读取超时 60 秒（LLM 响应较慢）
  write-timeout: 10000       # 写入超时 10 秒
```

### 2.3 HIS 集成服务
```yaml
# HIS 服务超时配置
app.his-service:
  connect-timeout: 5000      # 连接超时 5 秒
  read-timeout: 15000        # 读取超时 15 秒
  write-timeout: 5000        # 写入超时 5 秒
```

## 3. 线程池配额

### 3.1 核心业务线程池
```yaml
app.thread-pool.core-business:
  core-pool-size: 10
  max-pool-size: 50
  queue-capacity: 200
  keep-alive-seconds: 60
  rejection-policy: CallerRuns  # 调用者运行策略
```

### 3.2 语音处理线程池
```yaml
app.thread-pool.speech-processing:
  core-pool-size: 5
  max-pool-size: 20
  queue-capacity: 100
  keep-alive-seconds: 120
```

### 3.3 LLM 调用线程池
```yaml
app.thread-pool.llm-calls:
  core-pool-size: 5
  max-pool-size: 30
  queue-capacity: 150
  keep-alive-seconds: 60
```

### 3.4 HIS 集成线程池
```yaml
app.thread-pool.his-integration:
  core-pool-size: 5
  max-pool-size: 20
  queue-capacity: 100
  keep-alive-seconds: 60
```

## 4. 数据库连接池配置

### 4.1 HikariCP 配置
```yaml
spring.datasource.hikari:
  maximum-pool-size: 20      # 最大连接数
  minimum-idle: 5            # 最小空闲连接
  connection-timeout: 30000  # 连接超时 30 秒
  idle-timeout: 600000       # 空闲超时 10 分钟
  max-lifetime: 1800000      # 最大生命周期 30 分钟
  leak-detection-threshold: 60000  # 泄漏检测 60 秒（仅开发环境）
```

## 5. 性能监控指标

### 5.1 技术指标
- CPU 使用率
- 内存使用率
- 线程池活跃线程数
- 线程池队列长度
- 数据库连接池使用率
- 接口响应时间（P50/P90/P99）
- 接口 QPS

### 5.2 业务指标
- 问诊会话创建速率
- 语音转写成功率
- LLM 调用成功率
- HIS 集成成功率
- 缓存命中率

## 6. 告警阈值

### P0 级告警（立即响应）
- CPU 使用率 > 90% 持续 5 分钟
- 内存使用率 > 90% 持续 5 分钟
- 接口错误率 > 10% 持续 2 分钟
- 数据库连接池耗尽

### P1 级告警（30 分钟内响应）
- CPU 使用率 > 70% 持续 10 分钟
- 内存使用率 > 80% 持续 10 分钟
- 接口响应时间 P99 > 5 秒
- 缓存命中率 < 50%

### P2 级告警（24 小时内响应）
- CPU 使用率 > 50% 持续 30 分钟
- 内存使用率 > 70% 持续 30 分钟
- 接口响应时间 P90 > 2 秒
