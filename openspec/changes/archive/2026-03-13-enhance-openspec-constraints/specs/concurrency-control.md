# 并发控制规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：分布式锁机制

系统必须实现分布式锁，防止多实例并发执行同一任务。

#### 场景：Redis 分布式锁
- **当** 多实例部署时执行定时任务
- **那么** 必须使用 Redis 分布式锁
- **那么** 锁键命名：`his_agent:lock:{task_name}`
- **那么** 锁超时时间 = 任务超时时间 + 5 分钟
- **那么** 禁止使用本地锁（synchronized、ReentrantLock）

#### 场景：锁获取失败
- **当** 无法获取分布式锁时
- **那么** 必须立即放弃执行
- **那么** 记录日志：`Task {task_name} skipped, lock held by another instance`
- **那么** 不重试、不等待

#### 场景：锁释放
- **当** 任务执行完成时
- **那么** 必须主动释放锁
- **当** 任务超时或异常时
- **那么** 锁必须自动过期（基于 Redis TTL）
- **那么** 禁止手动延长锁时间

---

### 需求：线程池隔离

系统必须实现线程池隔离，避免资源竞争和级联故障。

#### 场景：业务线程池
- **当** 处理核心业务逻辑时
- **那么** 必须使用独立线程池：
```yaml
thread-pool:
  name: core-business
  core-size: 20
  max-size: 50
  queue-capacity: 100
  keep-alive-seconds: 60
  rejection-policy: CallerRuns
```

#### 场景：语音处理线程池
- **当** 处理语音转写任务时
- **那么** 必须使用独立线程池：
```yaml
thread-pool:
  name: speech-processing
  core-size: 10
  max-size: 20
  queue-capacity: 50
  keep-alive-seconds: 30
  rejection-policy: Reject
```

#### 场景：LLM 调用线程池
- **当** 调用 LLM 服务时
- **那么** 必须使用独立线程池：
```yaml
thread-pool:
  name: llm-calls
  core-size: 5
  max-size: 10
  queue-capacity: 20
  keep-alive-seconds: 30
  rejection-policy: CallerRuns
```

#### 场景：HIS 集成线程池
- **当** 调用 HIS 系统接口时
- **那么** 必须使用独立线程池：
```yaml
thread-pool:
  name: his-integration
  core-size: 10
  max-size: 20
  queue-capacity: 30
  keep-alive-seconds: 30
  rejection-policy: Reject
```

#### 场景：线程池监控
- **当** 线程池运行时
- **那么** 必须暴露监控指标：
  - 活跃线程数
  - 队列长度
  - 拒绝任务数
  - 平均等待时间
  - 平均执行时间

---

### 需求：限流器实现

系统必须实现多层限流，保护系统免受过载影响。

#### 场景：全局限流（令牌桶）
- **当** 请求进入系统时
- **那么** 必须通过全局令牌桶限流：
```yaml
rate-limiter:
  type: token-bucket
  capacity: 100        # 桶容量
  refill-rate: 50      # 每秒补充令牌数
  timeout: 0           # 不等待，立即拒绝
```

#### 场景：用户限流（滑动窗口）
- **当** 用户发起请求时
- **那么** 必须限制：
```yaml
rate-limiter:
  type: sliding-window
  per-user: true
  requests-per-second: 10
  requests-per-minute: 100
  requests-per-hour: 1000
```

#### 场景：接口限流
- **当** 访问特定接口时
- **那么** 必须限制：
```yaml
rate-limiter:
  endpoints:
    /api/v1/consultations/voice:
      requests-per-second: 5
    /api/v1/consultations/generate:
      requests-per-second: 2
    /api/v1/his/sync:
      requests-per-second: 10
```

#### 场景：限流响应
- **当** 请求被限流时
- **那么** 必须返回：
  - HTTP 状态码：429 Too Many Requests
  - 响应头：`Retry-After: {秒数}`
  - 响应体：`{"code": 429, "message": "请求过于频繁，请稍后重试"}`

---

### 需求：并发会话管理

系统必须管理用户并发会话，防止资源滥用。

#### 场景：会话并发限制
- **当** 用户创建新会话时
- **那么** 必须检查：
  - 单用户最大并发会话数：3
  - 超出限制时拒绝创建新会话
  - 返回错误：`"您当前会话数已达上限，请先结束旧会话"`

#### 场景：会话超时
- **当** 会话空闲时
- **那么** 必须：
  - 空闲超时：30 分钟
  - 超时后自动清理会话资源
  - 释放语音缓存、LLM 上下文

#### 场景：会话互斥
- **当** 同一用户同时发起多个语音录入时
- **那么** 必须：
  - 仅允许一路语音录入进行
  - 新的语音请求必须等待或拒绝
  - 推荐：拒绝并提示 `"正在录制中，请先结束当前录制"`

---

### 需求：数据库并发控制

系统必须实现数据库层面的并发控制，保证数据一致性。

#### 场景：乐观锁
- **当** 更新数据时
- **那么** 必须使用版本号：
```java
public class Consultation {
    @Version
    private Long version;
    // ...
}
```
- **那么** 更新失败时抛出 `OptimisticLockException`
- **那么** 前端提示 `"数据已被修改，请刷新后重试"`

#### 场景：悲观锁
- **当** 高并发扣减资源时
- **那么** 必须使用 `SELECT ... FOR UPDATE`
- **例如** 号源扣减、库存管理
- **那么** 锁超时时间：≤ 5 秒

#### 场景：唯一约束
- **当** 插入数据时
- **那么** 必须依赖数据库唯一约束防止重复：
```sql
CREATE UNIQUE INDEX uk_patient_id ON patient(id_card);
CREATE UNIQUE INDEX uk_consultation ON consultation(order_no);
```

---

### 需求：幂等性保证

系统必须实现接口幂等性，防止重复提交导致数据问题。

#### 场景：幂等令牌
- **当** 客户端发起写请求时
- **那么** 必须携带幂等令牌：
```http
POST /api/v1/consultations
Idempotency-Key: abc123-def456
```
- **那么** 服务端缓存令牌：24 小时
- **那么** 相同令牌返回相同响应

#### 场景：幂等性实现
- **当** 收到重复请求时
- **那么** 必须：
  - 检查幂等令牌是否已存在
  - 存在则返回缓存的响应
  - 不重复执行业务逻辑

#### 场景：防重复提交
- **当** 用户快速点击提交按钮时
- **那么** 前端必须：
  - 提交后禁用按钮：3 秒
  - 显示 loading 状态
  - 禁止表单重复提交

---

### 需求：异步任务管理

系统必须实现异步任务管理，避免阻塞主线程。

#### 场景：异步注解
- **当** 执行异步任务时
- **那么** 必须使用 `@Async`：
```java
@Async("async-tasks")
public CompletableFuture<Void> processAsync() {
    // 异步处理
}
```

#### 场景：异步超时
- **当** 异步任务执行时
- **那么** 必须设置超时：
```java
@Async("async-tasks")
public CompletableFuture<Void> processAsync() {
    // 必须在 5 分钟内完成
}
```

#### 场景：异步异常处理
- **当** 异步任务失败时
- **那么** 必须：
  - 记录完整异常日志
  - 发送告警通知
  - 不阻塞调用方

---

### 需求：并发安全数据结构

系统必须使用并发安全的数据结构，避免并发修改异常。

#### 场景：共享缓存
- **当** 多线程访问缓存时
- **那么** 必须使用：
  - `ConcurrentHashMap`（本地缓存）
  - `RedisTemplate`（分布式缓存）
  - 禁止使用 `HashMap`、`ArrayList`

#### 场景：计数器
- **当** 多线程计数时
- **那么** 必须使用：
  - `AtomicLong`、`AtomicInteger`
  - `LongAdder`（高并发场景）
  - Redis `INCR` 命令（分布式计数）

#### 场景：集合遍历
- **当** 遍历共享集合时
- **那么** 必须：
  - 使用迭代器安全遍历
  - 或先复制再遍历
  - 禁止直接遍历修改

---

### 需求：技术选型明确

系统必须明确并发相关的技术选型。

#### 场景：前端并发
- **当** 前端处理并发请求时
- **那么** 必须使用：
  - Axios 请求取消（CancelToken）
  - 请求队列管理
  - 防抖（debounce）和节流（throttle）

#### 场景：后端并发
- **当** 后端处理并发时
- **那么** 必须使用：
  - Java `java.util.concurrent` 包
  - Spring `@Async` 异步支持
  - Redis 分布式锁
  - Resilience4j 限流器

#### 场景：数据库并发
- **当** 数据库并发访问时
- **那么** 必须使用：
  - MySQL 行级锁
  - 乐观锁（@Version）
  - 事务隔离级别：READ COMMITTED

---

## 修改需求

（无现有需求修改）

---

## 移除需求

（无需求移除）

---

## 验收标准

- [ ] 分布式锁正常工作（多实例部署验证）
- [ ] 线程池隔离实施（各业务使用独立线程池）
- [ ] 限流器生效（超出限制返回 429）
- [ ] 会话并发限制生效
- [ ] 数据库乐观锁实施
- [ ] 接口幂等性实现
- [ ] 异步任务正常执行
- [ ] 并发安全数据结构使用
- [ ] 线程池监控指标可观察
- [ ] 压测达到性能指标要求
