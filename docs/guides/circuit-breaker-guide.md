# 熔断降级配置文档

**版本**: 1.0  
**日期**: 2026-03-12  
**适用**: 后端开发人员、运维工程师

---

## 1. 概述

本系统基于 Resilience4j 实现熔断、降级、限流、重试功能，保障系统高可用性。

---

## 2. 熔断器配置

### 2.1 配置文件

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      # 语音识别服务熔断配置
      voiceRecognition:
        registerHealthIndicator: true
        slidingWindowSize: 10              # 滑动窗口大小（请求数）
        slidingWindowType: COUNT_BASED     # 基于请求数量
        failureRateThreshold: 50           # 失败率阈值（%）
        waitDurationInOpenState: 30000     # 打开状态等待时间（毫秒）
        permittedNumberOfCallsInHalfOpenState: 3  # 半开状态允许请求数
        automaticTransitionFromOpenToHalfOpenEnabled: true
      
      # LLM 服务熔断配置
      llmService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60000     # LLM 服务等待时间更长
        permittedNumberOfCallsInHalfOpenState: 3
      
      # HIS 集成服务熔断配置
      hisIntegration:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30000
        permittedNumberOfCallsInHalfOpenState: 3
```

### 2.2 熔断器状态说明

| 状态 | 说明 | 行为 |
|------|------|------|
| CLOSED | 关闭（正常） | 正常处理请求，统计失败率 |
| OPEN | 打开（熔断） | 直接拒绝请求，返回降级结果 |
| HALF_OPEN | 半开（试探） | 允许少量请求通过，测试服务是否恢复 |

### 2.3 使用示例

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
@RequiredArgsConstructor
public class VoiceRecognitionService {
    
    private final VoiceRecognitionFallback fallback;
    
    @CircuitBreaker(
        name = "voiceRecognition",
        fallbackMethod = "transcribeFallback"
    )
    public String transcribe(byte[] audioData) {
        // 调用语音识别服务
        return callVoiceApi(audioData);
    }
    
    // 降级方法
    public String transcribeFallback(byte[] audioData, Throwable ex) {
        return fallback.transcribeFallback(audioData, ex);
    }
}
```

---

## 3. 限流器配置

### 3.1 配置文件

```yaml
resilience4j:
  ratelimiter:
    instances:
      # 全局限流
      default:
        registerHealthIndicator: true
        limitForPeriod: 100          # 每次刷新周期允许的请求数
        limitRefreshPeriod: 60s      # 刷新周期
        timeoutDuration: 0s          # 超时立即拒绝
      
      # 用户级限流
      userLevel:
        limitForPeriod: 20           # 单用户每秒请求数
        limitRefreshPeriod: 60s
        timeoutDuration: 5s          # 超时等待 5 秒
```

### 3.2 使用示例

```java
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
public class PatientService {
    
    @RateLimiter(name = "userLevel", fallbackMethod = "searchPatientsFallback")
    public List<PatientDTO> searchPatients(String keyword) {
        return patientRepository.search(keyword);
    }
    
    public List<PatientDTO> searchPatientsFallback(String keyword, Throwable ex) {
        return Collections.emptyList();
    }
}
```

---

## 4. 重试机制配置

### 4.1 配置文件

```yaml
resilience4j:
  retry:
    instances:
      default:
        maxAttempts: 3                    # 最大重试次数
        waitDuration: 1s                  # 重试间隔
        enableExponentialBackoff: true    # 启用指数退避
        exponentialBackoffMultiplier: 2   # 退避倍数
        retryExceptions:
          - java.util.concurrent.TimeoutException
          - java.io.IOException
          - org.springframework.dao.TransientDataAccessException
```

### 4.2 使用示例

```java
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class HisIntegrationService {
    
    @Retry(
        name = "default",
        fallbackMethod = "getPatientFallback"
    )
    public HisPatientDTO getPatient(String patientId) {
        return hisAdapter.getPatient(patientId);
    }
    
    public HisPatientDTO getPatientFallback(String patientId, Throwable ex) {
        log.warn("Get patient failed, using fallback", ex);
        return null;
    }
}
```

---

## 5. 超时限制配置

### 5.1 配置文件

```yaml
resilience4j:
  timelimiter:
    instances:
      default:
        timeoutDuration: 30s              # 默认超时时间
        cancelRunningFuture: true         # 超时取消任务
      
      # 语音服务超时
      voiceService:
        timeoutDuration: 30s
      
      # LLM 服务超时（更长）
      llmService:
        timeoutDuration: 60s
      
      # HIS 服务超时
      hisService:
        timeoutDuration: 15s
```

### 5.2 使用示例

```java
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;

@Service
public class LlmService {
    
    @TimeLimiter(name = "llmService")
    @CircuitBreaker(name = "llmService", fallbackMethod = "chatFallback")
    public CompletableFuture<String> chat(String prompt) {
        return CompletableFuture.supplyAsync(() -> callLlmApi(prompt));
    }
}
```

---

## 6. 降级链实现

### 6.1 语音识别降级链

```java
@Service
public class VoiceRecognitionFallback {
    
    /**
     * 降级链：讯飞 → 阿里云 → Whisper → 手动录入
     */
    public String transcribeFallback(byte[] audioData, Throwable ex) {
        // 降级 1: 阿里云
        try {
            return transcribeWithAliyun(audioData);
        } catch (Exception e) {
            log.warn("Aliyun fallback failed", e);
        }
        
        // 降级 2: Whisper
        try {
            return transcribeWithWhisper(audioData);
        } catch (Exception e) {
            log.warn("Whisper fallback failed", e);
        }
        
        // 降级 3: 手动录入
        return null;
    }
}
```

### 6.2 LLM 服务降级链

```java
@Service
public class LlmServiceFallback {
    
    /**
     * 降级链：主 LLM → 备用 LLM → 规则引擎 → 模板回复
     */
    public String chatFallback(String prompt, Throwable ex) {
        // 降级 1: 备用 LLM
        try {
            return chatWithBackupLlm(prompt);
        } catch (Exception e) {
            log.warn("Backup LLM failed", e);
        }
        
        // 降级 2: 规则引擎
        try {
            return processWithRuleEngine(prompt);
        } catch (Exception e) {
            log.warn("Rule engine failed", e);
        }
        
        // 降级 3: 模板回复
        return "抱歉，系统暂时无法处理您的问题。";
    }
}
```

### 6.3 HIS 集成降级链

```java
@Service
public class HisIntegrationFallback {
    
    /**
     * 降级链：主 HIS → 备用 HIS → 本地缓存 → 空结果
     */
    public HisPatientDTO getPatientFallback(String patientId, Throwable ex) {
        // 降级 1: 备用 HIS
        try {
            return getPatientFromBackupHis(patientId);
        } catch (Exception e) {
            log.warn("Backup HIS failed", e);
        }
        
        // 降级 2: 本地缓存
        try {
            return getPatientFromCache(patientId);
        } catch (Exception e) {
            log.warn("Cache fallback failed", e);
        }
        
        // 降级 3: 空结果
        return null;
    }
}
```

---

## 7. 监控指标

### 7.1 熔断器指标

| 指标名称 | 说明 |
|----------|------|
| `resilience4j_circuitbreaker_state` | 熔断器状态（0=CLOSED, 1=OPEN, 2=HALF_OPEN） |
| `resilience4j_circuitbreaker_failure_rate` | 失败率 |
| `resilience4j_circuitbreaker_buffered_calls` | 缓冲请求数 |

### 7.2 限流器指标

| 指标名称 | 说明 |
|----------|------|
| `resilience4j_ratelimiter_available_permissions` | 可用许可数 |
| `resilience4j_ratelimiter_waiting_threads` | 等待线程数 |

---

## 8. 健康检查

### 8.1 熔断器健康状态

```bash
# 查看熔断器状态
GET http://localhost:8080/actuator/circuitbreakers

# 查看具体熔断器状态
GET http://localhost:8080/actuator/circuitbreakers/voiceRecognition
```

### 8.2 状态响应

```json
{
  "status": "UP",
  "details": {
    "voiceRecognition": {
      "status": "UP",
      "details": {
        "state": "CLOSED",
        "failureRate": "0.0%",
        "bufferedCalls": 10
      }
    }
  }
}
```

---

## 9. 最佳实践

### 9.1 熔断器配置建议

| 服务类型 | 滑动窗口 | 失败率阈值 | 打开等待时间 |
|----------|----------|------------|--------------|
| 语音识别 | 10 | 50% | 30 秒 |
| LLM 服务 | 10 | 50% | 60 秒 |
| HIS 集成 | 10 | 50% | 30 秒 |
| 数据库 | 20 | 30% | 10 秒 |

### 9.2 降级设计原则

1. **优雅降级**: 提供有限功能而非完全失败
2. **快速失败**: 超时立即返回降级结果
3. **日志记录**: 记录降级事件便于分析
4. **监控告警**: 降级触发时通知运维

---

## 10. 相关文档

- [API 约束使用文档](./api-constraints-guide.md)
- [监控告警配置文档](./monitoring-guide.md)
