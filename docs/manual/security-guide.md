# 安全规范指南

本文档详细说明 his_agent 的安全规范和实现要求。

---

## 目录

1. [网关安全](#网关安全)
2. [外部 HIS 系统接口安全](#外部 his 系统接口安全)
3. [缓存安全](#缓存安全)
4. [异常处理规范](#异常处理规范)
5. [代码风格规范](#代码风格规范)

---

## 网关安全

### 架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Spring Security Filter Chain                │    │
│  │                                                           │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │    │
│  │  │  认证    │  │  授权    │  │  限流    │  │  审计    │ │    │
│  │  │  Filter  │─▶│  Filter  │─▶│  Filter  │─▶│  Filter  │ │    │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   业务 Controller │
                    └─────────────────┘
```

### JWT 认证

**Token 结构**:
```java
{
  "alg": "HS256",
  "typ": "JWT"
}
{
  "sub": "user123",
  "iat": 1678886400,
  "exp": 1678893600,  // 2 小时后过期
  "roles": ["DOCTOR"],
  "permissions": ["consultation:create", "consultation:read"]
}
```

**实现要求**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws SecurityException {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 白名单接口
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                // 需要认证的接口
                .requestMatchers("/api/v1/consultations/**").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            );
        return http.build();
    }
}
```

### 频率限制

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(
            RedisRateLimiter.builder()
                .requestsPerSecond(10)  // 每秒 10 请求
                .burstCapacity(20)       // 突发容量
                .build()
        ));
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
```

### IP 白名单

```yaml
# application.yml
hisagent:
  security:
    ip-whitelist:
      enabled: true
      addresses:
        - 192.168.1.0/24
        - 10.0.0.0/8
      # HIS 系统 IP
      his-systems:
        - name: "HIS-PROD-01"
          ip: "192.168.1.100"
        - name: "HIS-TEST-01"
          ip: "192.168.2.100"
```

---

## 外部 HIS 系统接口安全

### 认证流程

```
┌─────────────────┐                           ┌─────────────────┐
│   HIS 系统       │                           │   his_agent     │
│                 │                           │                 │
│  1. 生成签名     │                           │                 │
│  signature =    │────── 请求 ──────────────▶│  2. 验证签名    │
│  HMAC(secret,   │                           │  - 时间戳       │
│    timestamp +  │                           │  - IP 白名单     │
│    body)        │                           │  - 签名         │
│                 │                           │                 │
│  Headers:       │◀───── 响应 ───────────────│  3. 执行业务    │
│  X-API-Key: xxx │                           │                 │
│  X-Timestamp:   │                           │                 │
│  X-Signature:   │                           │                 │
└─────────────────┘                           └─────────────────┘
```

### API Key 管理

**环境变量配置**:
```bash
# .env
HIS_API_KEYS=his-system-1:secret1,his-system-2:secret2
```

**Java 配置**:
```java
@Configuration
public class HisApiConfig {
    
    @Value("${hisagent.security.api-keys}")
    private Map<String, String> apiKeys;
    
    @Bean
    public HisApiAuthService hisApiAuthService() {
        return new HisApiAuthService(apiKeys);
    }
}
```

### 签名实现

**HIS 系统侧 (请求方)**:
```java
public class HisApiClient {
    
    public HttpResponse sendRequest(String apiUrl, String body, String apiKey, String secret) {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        
        // 生成签名
        String signature = generateSignature(secret, timestamp, nonce, body);
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.set("X-Timestamp", String.valueOf(timestamp));
        headers.set("X-Nonce", nonce);
        headers.set("X-Signature", signature);
        headers.set("Content-Type", "application/json");
        
        return httpClient.post(apiUrl, body, headers);
    }
    
    private String generateSignature(String secret, long timestamp, String nonce, String body) {
        String message = timestamp + nonce + body;
        return HmacUtils.hmacSha256Hex(secret, message);
    }
}
```

**his_agent 侧 (服务端)**:
```java
@Component
public class HisApiAuthFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 1. 获取请求头
        String apiKey = httpRequest.getHeader("X-API-Key");
        String timestamp = httpRequest.getHeader("X-Timestamp");
        String nonce = httpRequest.getHeader("X-Nonce");
        String signature = httpRequest.getHeader("X-Signature");
        
        // 2. 验证 API Key
        String secret = apiKeys.get(apiKey);
        if (secret == null) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
            return;
        }
        
        // 3. 验证时间戳 (5 分钟内)
        long requestTime = Long.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - requestTime) > 300000) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request expired");
            return;
        }
        
        // 4. 验证签名
        String body = getRequestBody(httpRequest);
        String expectedSignature = generateSignature(secret, timestamp, nonce, body);
        if (!expectedSignature.equals(signature)) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }
        
        // 5. 验证 IP 白名单
        String clientIp = httpRequest.getRemoteAddr();
        if (!ipWhitelist.contains(clientIp)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "IP not allowed");
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

### API Key 轮换

```java
@Service
public class ApiKeyRotationService {
    
    @Scheduled(cron = "0 0 0 * * 0")  // 每周执行
    public void rotateApiKeys() {
        List<HisSystem> systems = hisSystemRepository.findAll();
        
        for (HisSystem system : systems) {
            // 检查是否需要轮换 (90 天)
            if (system.shouldRotateKey()) {
                // 生成新密钥
                String newSecret = generateSecureRandomKey();
                
                // 通知 HIS 系统 (通过安全渠道)
                hisNotificationService.sendNewApiKey(system, newSecret);
                
                // 更新密钥 (保留旧密钥 24 小时过渡期)
                system.setNewApiKey(newSecret);
                system.setRotationTime(LocalDateTime.now());
                hisSystemRepository.save(system);
            }
        }
    }
    
    private String generateSecureRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32];
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
```

---

## 缓存安全

### 缓存键规范

```java
public class CacheKeyBuilder {
    
    private static final String PREFIX = "his_agent";
    
    // 患者数据缓存 (30 分钟)
    public static String patient(String patientId) {
        return String.format("%s:patient:%s", PREFIX, patientId);
    }
    
    // 问诊会话缓存 (2 小时)
    public static String consultation(String consultationId) {
        return String.format("%s:consultation:%s", PREFIX, consultationId);
    }
    
    // 医学词库缓存 (24 小时)
    public static String medicalTerm(String category) {
        return String.format("%s:medical-term:%s", PREFIX, category);
    }
    
    // 会话数据缓存 (2 小时)
    public static String session(String sessionId) {
        return String.format("%s:session:%s", PREFIX, sessionId);
    }
}
```

### 缓存过期时间

```java
@Configuration
public class CacheConfig {
    
    // 患者数据：30 分钟
    public static final long PATIENT_TTL = 30 * 60;
    
    // 问诊会话：2 小时
    public static final long CONSULTATION_TTL = 2 * 60 * 60;
    
    // 医学词库：24 小时
    public static final long MEDICAL_TERM_TTL = 24 * 60 * 60;
    
    // 会话数据：2 小时
    public static final long SESSION_TTL = 2 * 60 * 60;
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheManager.RedisCacheManagerBuilder builder = 
            RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(factory)
                .transactionAware();
        
        // 配置默认 TTL
        builder.defaults((cacheName) -> 
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(1800))  // 30 分钟
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
        );
        
        return builder.build();
    }
}
```

### 缓存穿透防护

```java
@Service
public class PatientService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private BloomFilter bloomFilter;
    
    public Patient getPatient(String patientId) {
        // 1. 布隆过滤器检查
        if (!bloomFilter.mightContain(patientId)) {
            return null;  // 肯定不存在
        }
        
        // 2. 查询缓存
        String key = CacheKeyBuilder.patient(patientId);
        Patient patient = (Patient) redisTemplate.opsForValue().get(key);
        if (patient != null) {
            return patient;
        }
        
        // 3. 查询数据库
        patient = patientRepository.findById(patientId).orElse(null);
        
        if (patient == null) {
            // 缓存空值，防止穿透
            redisTemplate.opsForValue().set(key, new Object(), 5, TimeUnit.MINUTES);
        } else {
            // 缓存实际数据
            redisTemplate.opsForValue().set(key, patient, 30, TimeUnit.MINUTES);
        }
        
        return patient;
    }
}
```

### 缓存雪崩防护

```java
public class CacheUtil {
    
    /**
     * 添加随机偏移的过期时间
     */
    public static long getExpiryWithJitter(long baseSeconds, double jitterPercent) {
        Random random = new Random();
        long jitter = (long) (baseSeconds * jitterPercent * random.nextDouble());
        return baseSeconds + jitter;
    }
    
    // 使用示例
    long ttl = getExpiryWithJitter(1800, 0.1);  // 30 分钟 ± 10%
}
```

---

## 异常处理规范

### 统一异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // 业务异常
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage(), ex);
        return ErrorResponse.builder()
            .code(400)
            .message(ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    // 权限异常
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ErrorResponse.builder()
            .code(403)
            .message("没有访问权限")
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    // 资源不存在
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ErrorResponse.builder()
            .code(404)
            .message(ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    // 参数验证异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());
        
        return ErrorResponse.builder()
            .code(400)
            .message("参数验证失败")
            .errors(errors)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    // 未知异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception ex) {
        log.error("Unexpected exception", ex);
        return ErrorResponse.builder()
            .code(500)
            .message("系统内部错误")
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

### 异常日志格式

```java
@Slf4j
@Service
public class ConsultationService {
    
    public Consultation createConsultation(String patientId) {
        try {
            // 业务逻辑
        } catch (DataAccessException ex) {
            log.error("Database error creating consultation for patient: {}", 
                      patientId, ex);
            throw new BusinessException("创建问诊失败，请稍后重试");
        } catch (Exception ex) {
            log.error("Unexpected error creating consultation. " +
                      "User: {}, Patient: {}", 
                      getCurrentUser(), patientId, ex);
            throw new BusinessException("系统错误，请联系管理员");
        }
    }
}
```

---

## 代码风格规范

### Java 命名规范

```java
// ✅ 正确
public class ConsultationService { }
public interface PatientRepository { }
public void createConsultation() { }
private String patientId;
private static final int MAX_RETRY = 3;

// ❌ 错误
public class consultationService { }  // 类名应大驼峰
public void CreateConsultation() { }  // 方法名应小驼峰
private String PATIENT_ID;  // 非 final 不应大写
```

### 代码行数限制

```java
// ✅ 正确：方法简短，职责单一
public Patient getPatient(String patientId) {
    validatePatientId(patientId);
    return patientRepository.findById(patientId)
        .orElseThrow(() -> new NotFoundException("患者不存在"));
}

// ❌ 错误：方法过长，应拆分
public void processConsultation() {
    // 50 行代码...
    // 应该拆分为多个小方法
}
```

### 禁止魔法数字

```java
// ✅ 正确
private static final int SESSION_TIMEOUT_MINUTES = 30;
private static final int MAX_UPLOAD_SIZE_MB = 10;

if (age > SESSION_TIMEOUT_MINUTES) { }

// ❌ 错误
if (age > 30) { }  // 30 的含义不明确
```

---

## 参考资料

- [Spring Security 文档](https://docs.spring.io/spring-security/reference/)
- [JWT.io](https://jwt.io/)
- [Redis 最佳实践](https://redis.io/docs/manual/)
- [OWASP API Security](https://owasp.org/www-project-api-security/)
