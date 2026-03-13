# 熔断降级规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：熔断器配置

所有外部服务调用必须配置熔断器，防止级联故障。

#### 场景：语音识别熔断
- **当** 调用语音识别服务时
- **那么** 必须配置：
```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker speechRecognitionCircuitBreaker() {
        return CircuitBreaker.of("speechRecognition",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)      // 失败率 50% 时打开
                .waitDurationInOpenState(Duration.ofSeconds(30))  // 打开状态等待 30 秒
                .slidingWindowSize(10)         // 滑动窗口大小 10
                .minimumNumberOfCalls(5)       // 最小调用数 5
                .permittedNumberOfCallsInHalfOpenState(3)  // 半开状态允许 3 次调用
                .build());
    }
}
```

#### 场景：LLM 服务熔断
- **当** 调用 LLM 服务时
- **那么** 必须配置：
```java
@Bean
public CircuitBreaker llmCircuitBreaker() {
    return CircuitBreaker.of("llmProvider",
        CircuitBreakerConfig.custom()
            .failureRateThreshold(60)      // 失败率 60% 时打开
            .waitDurationInOpenState(Duration.ofSeconds(60))  // 打开状态等待 60 秒
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .build());
}
```

#### 场景：HIS 集成熔断
- **当** 调用 HIS 系统时
- **那么** 必须配置：
```java
@Bean
public CircuitBreaker hisIntegrationCircuitBreaker() {
    return CircuitBreaker.of("hisIntegration",
        CircuitBreakerConfig.custom()
            .failureRateThreshold(40)      // 失败率 40% 时打开（更敏感）
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .build());
}
```

### 需求：降级策略

必须为每个外部服务定义降级方案。

#### 场景：语音识别降级
- **当** 语音识别服务不可用时
- **那么** 必须执行：
```
降级链：讯飞医疗 ASR → 阿里云 ASR → Whisper 本地 → 提示手动输入

// 代码实现
@Service
public class SpeechRecognitionService {
    
    @CircuitBreaker(name = "speechRecognition", fallbackMethod = "fallback")
    public RecognitionResult recognize(Audio audio) {
        return iflytekClient.recognize(audio);
    }
    
    public RecognitionResult fallback(Audio audio, Throwable t) {
        log.warn("讯飞 ASR 失败，尝试降级：{}", t.getMessage());
        
        // 尝试备选方案
        try {
            return aliyunClient.recognize(audio);
        } catch (Exception e) {
            // 最终降级：提示手动输入
            return RecognitionResult.manualInputRequired();
        }
    }
}
```

#### 场景：LLM 服务降级
- **当** LLM 服务不可用时
- **那么** 必须执行：
```
降级链：云端 LLM（Qwen） → 云端 LLM（Claude） → 本地 LLM（Ollama） → 返回模板病历

// 代码实现
@CircuitBreaker(name = "llmProvider", fallbackMethod = "fallback")
public SOAPNote generateSOAPNote(String transcript) {
    return qwenClient.generate(transcript);
}

public SOAPNote fallback(String transcript, Throwable t) {
    log.warn("Qwen LLM 失败，尝试降级：{}", t.getMessage());
    
    try {
        return claudeClient.generate(transcript);
    } catch (Exception e) {
        // 返回模板病历
        return SOAPNote.template(transcript);
    }
}
```

#### 场景：HIS 集成降级
- **当** HIS 系统不可用时
- **那么** 必须执行：
```
降级链：HIS 系统 → 本地缓存 → 提示稍后同步

// 代码实现
@CircuitBreaker(name = "hisIntegration", fallbackMethod = "fallback")
public HisPatient getPatientInfo(String patientId) {
    return hisClient.getPatientInfo(patientId);
}

public HisPatient fallback(String patientId, Throwable t) {
    log.warn("HIS 获取患者信息失败，使用缓存：{}", t.getMessage());
    
    // 从缓存获取（可能过期）
    HisPatient cached = cache.get(patientId);
    if (cached != null) {
        return cached.withWarning("数据可能过期");
    }
    
    // 返回空数据，提示稍后同步
    return HisPatient.notAvailable(patientId);
}
```

### 需求：限流器配置

必须配置限流器保护外部服务不被过度调用。

#### 场景：语音识别限流
- **当** 调用语音识别服务时
- **那么** 必须限流：
```java
@Bean
public RateLimiter speechRecognitionRateLimiter() {
    return RateLimiter.of("speechRecognition",
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(5)           // 每秒 5 次
            .timeoutDuration(Duration.ofSeconds(2))
            .build());
}
```

#### 场景：LLM 服务限流
- **当** 调用 LLM 服务时
- **那么** 必须限流：
```java
@Bean
public RateLimiter llmRateLimiter() {
    return RateLimiter.of("llmProvider",
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(2)           // 每秒 2 次（LLM 较慢）
            .timeoutDuration(Duration.ofSeconds(5))
            .build());
}
```

### 需求：重试机制

必须配置智能重试机制处理临时故障。

#### 场景：重试配置
- **当** 调用外部服务失败时
- **那么** 必须重试：
```java
@Configuration
public class RetryConfig {
    
    @Bean
    public Retry speechRecognitionRetry() {
        return Retry.of("speechRecognition",
            RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .exponentialBackoffMultiplier(2)  // 指数退避：1s, 2s, 4s
                .retryExceptions(
                    SocketTimeoutException.class,
                    ConnectTimeoutException.class,
                    SocketException.class
                )
                .ignoreExceptions(
                    BusinessException.class,      // 业务错误不重试
                    AccessDeniedException.class   // 权限错误不重试
                )
                .build());
    }
}
```

#### 场景：重试失败处理
- **当** 重试耗尽后
- **那么** 必须：
  - 记录完整错误日志
  - 触发降级策略
  - 发送告警通知（如果连续失败）
  - 返回友好错误提示给用户

### 需求：熔断状态监控

必须监控熔断器状态，及时处理异常。

#### 场景：状态监控
- **当** 监控熔断器时
- **那么** 必须监控：
```yaml
metrics:
  circuit-breaker:
    - name: circuit_breaker_state
      labels: [name, state]  # state: CLOSED, OPEN, HALF_OPEN
      type: gauge
      
    - name: circuit_breaker_failure_rate
      labels: [name]
      type: gauge
      
    - name: circuit_breaker_slow_call_rate
      labels: [name]
      type: gauge
```

#### 场景：状态告警
- **当** 熔断器打开时
- **那么** 必须：
  - 立即发送 P1 告警
  - 记录事件日志
  - 触发降级方案
  - 监控自动恢复（半开 → 关闭）
