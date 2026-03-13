# 基础架构技术设计

## 上下文

当前项目需要统一开发框架和生产级约束规范，整合原有 `dev-integration` 和 `enhance-openspec-constraints` 两个变更的需求。

**干系人**:
- 开发团队：需要统一的开发工具和自动化流程
- 运维团队：需要可视化的运维平台和监控告警
- 安全团队：需要数据安全和合规保障
- 测试团队：需要自动化测试框架和 Mock 策略

## 目标 / 非目标

**目标：**
- 建立完整的开发框架（CI/CD、Git Hooks、DevPortal）
- 建立生产级约束体系（API、安全、测试、监控）
- 实现 HIS 集成适配器架构
- 提供可视化运维能力

**非目标：**
- 自动化代码 Review（由人工执行）
- 完整 FHIR/HL7 支持（首期 REST/SOAP）

## 决策

### 0. 技术选型

**决策**: 前端 Vue 3 + TypeScript，后端 Java 17 + Spring Boot，数据库 MySQL 8+

```
┌─────────────────────────────────────────────────────────────┐
│  技术栈架构                                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  前端 (Vue 3 Ecosystem)                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Vue 3.4+ (Composition API)                          │   │
│  │  ├── TypeScript 5+ (类型安全)                        │   │
│  │  ├── Vite 6+ (构建工具)                              │   │
│  │  ├── Element Plus 2.8+ (UI 组件)                     │   │
│  │  ├── Pinia 2.2+ (状态管理)                           │   │
│  │  └── Vue Router 4.4+ (路由)                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  后端 (Java Ecosystem)                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Java 17+ (LTS)                                      │   │
│  │  ├── Spring Boot 3.x (应用框架)                      │   │
│  │  ├── Maven 3.9+ (构建工具)                           │   │
│  │  ├── Spring Data Redis (缓存)                        │   │
│  │  ├── Spring AI (LLM 集成)                             │   │
│  │  └── Lombok (代码简化)                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  数据库与缓存                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  MySQL 8+      (业务数据持久化)                       │   │
│  │  ├── Flyway    (数据库迁移)                          │   │
│  │  └── HikariCP  (连接池)                              │   │
│  │                                                      │   │
│  │  Redis 7+      (缓存/会话)                            │   │
│  │  ├── String 结构 (简单数据)                          │   │
│  │  ├── Hash 结构 (对象数据)                            │   │
│  │  └── ZSet 结构 (排行榜/延迟队列)                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**理由**:
- **Vue 3**: 渐进式框架，学习曲线低，适合医疗场景快速迭代
- **Java 17**: LTS 版本，Spring Boot 3.x 强制要求，企业级稳定性
- **MySQL 8+**: 医院最常用数据库，运维团队熟悉，JSON 字段支持好
- **Redis 7+**: 高性能缓存，支持丰富数据结构，医疗场景成熟应用

**替代方案**:
- 前端：React（❌ 学习曲线陡，医疗团队 Vue 经验更多）
- 后端：Node.js（❌ 类型安全弱，医疗场景 Java 更成熟）
- 数据库：PostgreSQL（❌ 医院运维团队不熟悉）

---

### 1. 开发框架设计

**决策**: 统一启动脚本 + Git Hooks + CI/CD

```
┌─────────────────────────────────────────────────────────────┐
│  开发工作流                                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  开发者执行 ./dev.sh                                        │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  1. Docker Compose 启动 MySQL + Redis                │   │
│  │  2. 启动 Spring Boot 后端 (:8080)                     │   │
│  │  3. 启动 Vite 前端 (:3000)                            │   │
│  │  4. 健康检查                                         │   │
│  │  5. 显示访问地址                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Git Commit 触发 Git Hooks                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  pre-commit:                                        │   │
│  │  - 前端：ESLint + Prettier + TypeCheck              │   │
│  │  - 后端：Checkstyle + 编译                          │   │
│  │  - OpenSpec：规格验证                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Git Push 触发 GitHub Actions                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  CI Pipeline:                                       │   │
│  │  Stage 1: Code Quality (ESLint, Checkstyle)         │   │
│  │  Stage 2: Testing (Vitest, JUnit)                   │   │
│  │  Stage 3: Build (Docker images)                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**理由**: 
- 简单直接，2-3 人团队最佳实践
- 自动化程度高，减少人工操作
- 开发体验好，一键启动

**替代方案**:
- 方案 A: DevContainer 完整容器化（❌ 学习曲线陡）
- 方案 B: Makefile 统一管理（❌ Windows 兼容性差）

---

### 2. DevPortal 架构

**决策**: 前后端分离 + WebSocket 实时日志

```
┌─────────────────────────────────────────────────────────────┐
│  DevPortal 架构                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  前端 (Vue 3 + TypeScript)                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  /__dev                                              │   │
│  │  ├── Dashboard (服务器状态)                          │   │
│  │  ├── HealthCheck (服务健康)                          │   │
│  │  ├── DeployManager (部署管理)                        │   │
│  │  ├── ConfigEditor (配置编辑)                         │   │
│  │  └── ApiLogs (API 日志 - WebSocket)                   │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         │ REST API + WebSocket                             │
│         │                                                   │
│         ▼                                                   │
│  后端 (Spring Boot)                                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  /api/dev/*                                          │   │
│  │  ├── GET  /server-status (服务器状态)                │   │
│  │  ├── GET  /health (健康检查)                         │   │
│  │  ├── POST /deploy (一键部署)                         │   │
│  │  ├── POST /services/{name}/restart (重启服务)        │   │
│  │  ├── GET  /configs (配置列表)                         │   │
│  │  ├── PUT  /configs/{name} (更新配置)                 │   │
│  │  └── WS   /ws/api-logs (API 日志推送)                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**实现状态**: ✅ 基础功能已完成

---

### 3. HIS 适配器架构

**决策**: 适配器模式 + 工厂模式

```java
// 统一适配器接口
public interface HisAdapter {
    String getName();
    boolean supports(String hisType);
    AuthResult authenticate(AuthRequest request);
    PatientInfo getPatientInfo(String patientId);
    void writeBackSoapNote(SOAPNote note);
}

// 适配器工厂
@Component
public class HisAdapterFactory {
    private final List<HisAdapter> adapters;
    
    public HisAdapter getAdapter(String hisType) {
        return adapters.stream()
            .filter(a -> a.supports(hisType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unsupported HIS: " + hisType));
    }
}

// 具体适配器
@Component
public class RestHisAdapter implements HisAdapter {
    // REST API 适配
}

@Component
public class SoapHisAdapter implements HisAdapter {
    // SOAP Web Service 适配
}

@Component
public class IihHisAdapter implements HisAdapter {
    // IIH 专属适配
}
```

**理由**:
- 符合开闭原则，新增 HIS 无需修改现有代码
- 支持多 HIS 厂商并行
- 易于测试和 Mock

---

### 4. 数据脱敏策略

**决策**: 注解驱动 + AOP 自动脱敏

```java
// 脱敏注解
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataMasking {
    MaskType value(); // NAME, ID_CARD, PHONE
}

// AOP 切面
@Aspect
@Component
public class DataMaskingAspect {
    
    @Around("@annotation(DataMasking)")
    public Object mask(ProceedingJoinPoint pjp) {
        // 自动脱敏处理
    }
}

// 使用示例
public class PatientDTO {
    @DataMasking(MaskType.NAME)
    private String name; // 张*三
    
    @DataMasking(MaskType.ID_CARD)
    private String idCard; // 110101********1234
    
    @DataMasking(MaskType.PHONE)
    private String phone; // 138****1234
}
```

**状态**: 📋 待实施

---

### 5. 监控告警架构

**决策**: Spring Boot Actuator + Prometheus + Grafana

```
┌─────────────────────────────────────────────────────────────┐
│  监控告警架构                                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  his_agent Backend                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Spring Boot Actuator                                │   │
│  │  - /actuator/health (健康检查)                       │   │
│  │  - /actuator/metrics (指标暴露)                      │   │
│  │  - /actuator/prometheus (Prometheus 格式)             │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  Prometheus (指标收集)                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  业务指标：                                          │   │
│  │  - consultations_total (问诊总量)                    │   │
│  │  - asr_success_rate (识别成功率)                     │   │
│  │  - llm_latency_seconds (LLM 延迟)                     │   │
│  │                                                      │   │
│  │  技术指标：                                          │   │
│  │  - jvm_memory_used_bytes (JVM 内存)                   │   │
│  │  - http_server_requests_seconds (HTTP 延迟)           │   │
│  │  - hikaricp_connections (连接池)                     │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  Grafana (可视化)                                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Dashboard:                                         │   │
│  │  - 应用概览                                          │   │
│  │  - 数据库监控                                        │   │
│  │  - Redis 监控                                        │   │
│  │  - 业务指标                                          │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  Alertmanager (告警)                                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  P0: 服务不可用 → 电话 + 短信                        │   │
│  │  P1: 错误率>5% → Slack + 邮件                        │   │
│  │  P2: 延迟过高 → Slack                                │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**状态**: 📋 待实施

---

### 6. 缓存策略架构

**决策**: Redis 统一缓存 + Cache-Aside 模式 + 多层防护

```
┌─────────────────────────────────────────────────────────────┐
│  缓存策略架构                                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  应用层 (Spring Boot)                                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Service Layer                                       │   │
│  │       │                                               │   │
│  │       ▼                                               │   │
│  │  ┌─────────────────────────────────────────────┐     │   │
│  │  │  CacheManager (Spring Cache Abstraction)    │     │   │
│  │  │  - @Cacheable      (查询缓存)               │     │   │
│  │  │  - @CachePut       (更新缓存)               │     │   │
│  │  │  - @CacheEvict     (删除缓存)               │     │   │
│  │  └─────────────────────────────────────────────┘     │   │
│  │       │                                               │   │
│  │       ▼                                               │   │
│  │  RedisTemplate / StringRedisTemplate                  │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  Redis 7+ (主从复制 + RDB/AOF 持久化)                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Database 0: 业务缓存                                 │   │
│  │  ├── his_agent:patient:info:*      (患者信息)        │   │
│  │  ├── his_agent:consultation:*      (问诊记录)        │   │
│  │  └── his_agent:user:session:*      (用户会话)        │   │
│  │                                                      │   │
│  │  Database 1: 配置缓存                                 │   │
│  │  ├── his_agent:config:*            (系统配置)        │   │
│  │  └── his_agent:medical:term:*      (医学词库)        │   │
│  │                                                      │   │
│  │  Database 2: 临时缓存                                 │   │
│  │  ├── his_agent:api:response:*      (API 响应)         │   │
│  │  └── his_agent:temp:*              (临时数据)        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  缓存防护机制                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  穿透防护：                                          │   │
│  │  - 布隆过滤器 (大数据量场景)                         │   │
│  │  - 空值缓存 (5 分钟 TTL)                               │   │
│  │                                                      │   │
│  │  雪崩防护：                                          │   │
│  │  - 过期时间 + 随机偏移 (±20%)                        │   │
│  │  - 热点数据后台刷新                                   │   │
│  │                                                      │   │
│  │  击穿防护：                                          │   │
│  │  - 互斥锁 (分布式锁)                                 │   │
│  │  - 逻辑过期（不设置物理过期时间）                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**缓存键设计规范**:
```
格式：his_agent:{模块}:{数据类型}:{ID}

示例:
- his_agent:patient:info:12345        # 患者 ID 为 12345 的信息
- his_agent:user:session:abc123       # 用户会话
- his_agent:medical:term:drug         # 药品词库
- his_agent:consultation:list:page1   # 问诊列表第 1 页
```

**过期时间策略**:
| 数据类型 | 过期时间 | 说明 |
|---------|---------|------|
| 患者数据 | 30 分钟 | 频繁访问，隐私敏感 |
| 用户会话 | 2 小时 | 与 Token 有效期一致 |
| 医学词库 | 24 小时 | 极少变化，访问频繁 |
| 配置数据 | 1 小时 | 可能动态调整 |
| API 响应 | 10 分钟 | 短期缓存 |
| 空值缓存 | 5 分钟 | 穿透防护 |

**缓存更新流程**:
```
1. 接收更新请求
2. 开启事务
3. 更新数据库（事务内）
4. 提交事务
5. 删除缓存（事务外）
6. 删除失败 → 重试 3 次 → 记录告警

注意：删除缓存而非更新缓存，避免并发问题
```

**实现状态**: 📋 待实施

---

### 7. 并发控制架构

**决策**: 多层并发控制 + 线程池隔离 + 分布式锁

```
┌─────────────────────────────────────────────────────────────┐
│  并发控制架构                                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  客户端请求                                                  │
│       │                                                      │
│       ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Layer 1: 网关限流 (Nginx / Spring Cloud Gateway)   │   │
│  │  - 全局限流：100 req/s                               │   │
│  │  - IP 限流：10 req/s                                  │   │
│  │  - 黑名单：自动封禁恶意 IP                            │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Layer 2: 应用限流 (Resilience4j RateLimiter)       │   │
│  │  - 用户限流：100 req/min                             │   │
│  │  - 接口限流：/voice 5 req/s, /generate 2 req/s       │   │
│  │  - 令牌桶算法                                        │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Layer 3: 线程池隔离 (ThreadPoolExecutor)           │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌─────────────┐ │   │
│  │  │ 核心业务池   │ │ 语音处理池   │ │ LLM 调用池   │ │   │
│  │  │ 20-50/100    │ │ 10-20/50     │ │ 5-10/20     │ │   │
│  │  └──────────────┘ └──────────────┘ └─────────────┘ │   │
│  │  ┌──────────────┐ ┌──────────────┐                 │   │
│  │  │ HIS 集成池    │ │ 异步任务池   │                 │   │
│  │  │ 10-20/30     │ │ 5-10/50      │                 │   │
│  │  └──────────────┘ └──────────────┘                 │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Layer 4: 分布式锁 (Redisson)                       │   │
│  │  - 定时任务互斥执行                                   │   │
│  │  - 会话并发控制                                       │   │
│  │  - 幂等性保证                                         │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Layer 5: 数据库并发控制 (MySQL)                    │   │
│  │  - 乐观锁 (@Version)                                 │   │
│  │  - 悲观锁 (SELECT ... FOR UPDATE)                   │   │
│  │  - 唯一约束 (UNIQUE INDEX)                           │   │
│  │  - 事务隔离 (READ COMMITTED)                         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**线程池配置**:
```yaml
spring:
  task:
    execution:
      pool:
        core-size: 20
        max-size: 50
        queue-capacity: 100
        keep-alive: 60s
        rejection-policy: caller-runs

# 自定义线程池
thread-pool:
  speech-processing:
    core-size: 10
    max-size: 20
    queue-capacity: 50
  llm-calls:
    core-size: 5
    max-size: 10
    queue-capacity: 20
  his-integration:
    core-size: 10
    max-size: 20
    queue-capacity: 30
```

**分布式锁实现**:
```java
@Service
public class ScheduledTaskExecutor {
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Scheduled(cron = "0 */5 * * * *")
    public void executeTask() {
        RLock lock = redissonClient.getLock("his_agent:lock:task-name");
        
        // 尝试获取锁，等待 0 秒，锁自动过期 10 分钟
        boolean locked = lock.tryLock(0, 10, TimeUnit.MINUTES);
        
        if (!locked) {
            log.warn("Task skipped, lock held by another instance");
            return;
        }
        
        try {
            // 执行任务逻辑
            processTask();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

**限流器配置**:
```java
@Configuration
public class RateLimiterConfig {
    
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.of(RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(0))
            .build());
    }
    
    @Bean
    public RateLimiterAspect rateLimiterAspect() {
        return new RateLimiterAspect();
    }
}

// 使用示例
@Service
public class VoiceService {
    
    @RateLimiter(name = "voiceEndpoint", fallbackMethod = "fallback")
    public String recognizeVoice(AudioStream audio) {
        // 限流保护
    }
}
```

**会话并发控制**:
```java
@Service
public class SessionManager {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean createSession(String userId, String sessionId) {
        String key = "his_agent:session:user:" + userId;
        
        // 检查并发会话数
        Long count = redisTemplate.opsForSet().size(key);
        if (count >= 3) {
            return false; // 已达上限
        }
        
        // 添加新会话
        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        return true;
    }
}
```

**实现状态**: 📋 待实施

---

### 8. 熔断降级策略

**决策**: Resilience4j 熔断器 + 多级降级链

```java
// 语音识别降级链
@Service
public class SpeechRecognitionServiceImpl {
    
    @CircuitBreaker(name = "asrService", fallbackMethod = "fallbackAsr")
    public String recognize(AudioStream audio) {
        // 主方案：讯飞 ASR
        return iflytekClient.recognize(audio);
    }
    
    // 降级方案 1: 腾讯云 ASR
    @CircuitBreaker(name = "tencentAsr", fallbackMethod = "fallbackTencent")
    public String fallbackAsr(AudioStream audio, Exception e) {
        return tencentClient.recognize(audio);
    }
    
    // 降级方案 2: Whisper 本地
    @CircuitBreaker(name = "whisper", fallbackMethod = "fallbackManual")
    public String fallbackTencent(Exception e) {
        return whisperClient.recognize(audio);
    }
    
    // 降级方案 3: 手动输入
    public String fallbackManual(Exception e) {
        throw new BusinessException("语音识别不可用，请手动输入");
    }
}
```

**配置**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      asrService:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        slidingWindowSize: 10
```

**状态**: 📋 待实施

---

## 风险 / 权衡

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| **IIH 接口不开放** | 高 | 准备通用 REST/SOAP 适配器，通过集成平台对接 |
| **DevPortal 安全问题** | 高 | 仅限内网访问，管理员认证，操作审计 |
| **熔断延迟故障发现** | 中 | 快速失败 + 监控告警配合 |
| **约束过多影响效率** | 中 | 提供代码模板和脚手架，渐进式采用 |
| **Redis 单点故障** | 高 | 配置主从复制 + RDB/AOF 持久化，生产环境使用哨兵模式 |
| **缓存数据不一致** | 中 | 严格遵循 Cache-Aside 模式，先更新 DB 再删除缓存 |
| **缓存穿透风险** | 中 | 实现布隆过滤器和空值缓存双重防护 |
| **技术栈单一** | 低 | MySQL 唯一数据库，简化运维但失去多数据库灵活性 |
| **分布式锁死锁** | 高 | 锁自动过期，禁止手动延长；监控锁等待时间 |
| **线程池耗尽** | 高 | 线程池隔离 + 监控告警；拒绝策略保护系统 |
| **限流过严** | 中 | 动态调整限流阈值；灰度发布验证 |
| **并发性能下降** | 中 | 压测验证；线程池参数调优 |

## 开放问题

1. **IIH 具体接口规范**: 需要联系厂商获取文档
2. **GitLab 部署网络**: 医院内网如何访问？
3. **DevPortal 权限**: 是否需要多级权限？
4. **日志保留策略**: 保留多久？是否归档？
