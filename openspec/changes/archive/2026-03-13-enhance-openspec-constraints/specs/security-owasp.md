# 安全规范（OWASP TOP 10 防护措施）

**版本**: 1.0  
**日期**: 2026-03-12  
**状态**: 新增

---

## 新增需求

### 需求：Spring Security 配置

系统必须实现完整的 Spring Security 认证授权框架。

#### 场景：安全依赖
- **当** 配置项目依赖时
- **那么** 必须添加：
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

#### 场景：SecurityConfig 配置
- **当** 配置 Spring Security 时
- **那么** 必须：
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（使用 JWT + CORS）
            .csrf(csrf -> csrf.disable())
            
            // 会话管理（无状态）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/__dev/**").permitAll()
                // 静态资源
                .requestMatchers("/", "/favicon.ico", "/assets/**").permitAll()
                // 其他接口需要认证
                .anyRequest().authenticated()
            )
            
            // JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class)
            
            // 异常处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

#### 场景：CORS 配置
- **当** 配置跨域时
- **那么** 必须：
```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "https://his-agent.hospital.com",
        "http://localhost:3000"  // 仅开发环境
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of(
        "Authorization", 
        "Content-Type", 
        "X-Request-ID",
        "X-Requested-With"
    ));
    config.setExposedHeaders(List.of("X-Request-ID", "X-Total-Count"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

---

### 需求：SQL 注入防护（A03:2021）

系统必须实现多层 SQL 注入防护。

#### 场景：JPA 参数绑定
- **当** 编写 Repository 查询时
- **那么** 必须使用参数绑定：
```java
// ✅ 正确：使用参数绑定
@Query("SELECT c FROM Consultation c WHERE c.patientId = :patientId")
List<Consultation> findByPatientId(@Param("patientId") String patientId);

// ❌ 错误：字符串拼接（禁止）
@Query("SELECT c FROM Consultation c WHERE c.patientId = '" + patientId + "'")
```

#### 场景：原生 SQL 防护
- **当** 使用原生 SQL 时
- **那么** 必须：
```java
// ✅ 正确：使用命名参数
@Query(value = "SELECT * FROM consultations WHERE patient_id = :patientId", 
       nativeQuery = true)
List<Consultation> findByPatientIdNative(@Param("patientId") String patientId);

// ❌ 错误：字符串拼接（禁止）
```

#### 场景：LIKE 查询防护
- **当** 使用 LIKE 查询时
- **那么** 必须：
```java
// ✅ 正确：参数化 LIKE
@Query("SELECT c FROM Consultation c WHERE c.patientName LIKE %:keyword%")
List<Consultation> searchByKeyword(@Param("keyword") String keyword);
```

#### 场景：排序字段防护
- **当** 动态排序时
- **那么** 必须使用白名单：
```java
public Sort createSort(String sortField, String sortDirection) {
    List<String> allowedFields = List.of("created_at", "updated_at", "patient_name");
    
    if (!allowedFields.contains(sortField)) {
        throw new IllegalArgumentException("Invalid sort field");
    }
    
    Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) 
        ? Sort.Direction.ASC : Sort.Direction.DESC;
    
    return Sort.by(direction, sortField);
}
```

---

### 需求：XSS 防护（A03:2021）

系统必须实现 XSS（跨站脚本）攻击防护。

#### 场景：输入验证
- **当** 接收用户输入时
- **那么** 必须验证：
```java
public record ConsultationRequest(
    @NotBlank(message = "患者 ID 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,36}$", message = "患者 ID 格式不正确")
    String patientId,
    
    @Size(max = 100, message = "患者姓名不能超过 100 字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9*]+$", 
             message = "患者姓名包含非法字符")
    String patientName,
    
    @Pattern(regexp = "^(male|female|other)$", message = "性别无效")
    String gender
) {}
```

#### 场景：输出编码
- **当** 返回 HTML 内容时
- **那么** 必须编码：
```java
@Component
public class XssEncoder {
    
    public String encode(String input) {
        if (input == null) return null;
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }
}
```

#### 场景：XSS 过滤器
- **当** 配置过滤器时
- **那么** 必须：
```java
@WebFilter(urlPatterns = "/*", filterName = "xssFilter")
public class XssFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }
}

public class XssRequestWrapper extends HttpServletRequestWrapper {
    
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return XssEncoder.encode(value);
    }
    
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        
        return Arrays.stream(values)
                     .map(XssEncoder::encode)
                     .toArray(String[]::new);
    }
}
```

#### 场景：HTTP 头防护
- **当** 配置响应头时
- **那么** 必须添加：
```java
@Bean
public FilterRegistration<?> securityHeadersFilter() {
    FilterRegistration<Filter> registration = new FilterRegistration<>();
    
    registration.setFilter((request, response, chain) -> {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // XSS 防护
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        chain.doFilter(request, response);
    });
    
    registration.addUrlPatterns("/*");
    return registration;
}
```

---

### 需求：CSRF 防护（A01:2021）

系统必须实现 CSRF（跨站请求伪造）防护。

#### 场景：CSRF Token（传统方式）
- **当** 使用 Session 认证时
- **那么** 必须：
```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .requireCsrfProtectionMatcher(new AntPathRequestMatcher("/api/**"))
);
```

#### 场景：JWT 双令牌（推荐）
- **当** 使用 JWT 认证时
- **那么** 必须：
```typescript
// 前端：从 Cookie 读取 CSRF Token
const csrfToken = getCookie('XSRF-TOKEN');

// 添加所有写请求头
http.interceptors.request.use((config) => {
  if (['POST', 'PUT', 'DELETE'].includes(config.method)) {
    config.headers['X-XSRF-TOKEN'] = csrfToken;
  }
  return config;
});
```

#### 场景：SameSite Cookie 属性
- **当** 设置 Cookie 时
- **那么** 必须：
```java
@Bean
public Cookie csrfCookie() {
    ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", csrfToken)
        .httpOnly(true)
        .secure(true)  // 仅 HTTPS
        .sameSite("Strict")
        .path("/")
        .maxAge(3600)
        .build();
    
    return cookie;
}
```

#### 场景：来源验证
- **当** 接收请求时
- **那么** 必须验证来源：
```java
@Component
public class OriginCheckFilter implements Filter {
    
    private final List<String> allowedOrigins = List.of(
        "https://his-agent.hospital.com"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String origin = httpRequest.getHeader("Origin");
        String referer = httpRequest.getHeader("Referer");
        
        if (origin != null && !allowedOrigins.contains(origin)) {
            ((HttpServletResponse) response).sendError(403, "Invalid origin");
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

---

### 需求：认证失效防护（A01:2021）

系统必须实现健壮的认证和会话管理。

#### 场景：密码加密
- **当** 存储密码时
- **那么** 必须：
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // cost factor ≥ 12
}

// 使用
String encodedPassword = passwordEncoder.encode(rawPassword);
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

#### 场景：密码策略
- **当** 用户设置密码时
- **那么** 必须验证：
```java
public record RegisterRequest(
    @NotBlank String username,
    
    @NotBlank
    @Size(min = 12, message = "密码长度至少 12 位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
             message = "密码必须包含大小写字母、数字和特殊字符")
    String password
) {}
```

#### 场景：登录失败限制
- **当** 用户登录失败时
- **那么** 必须：
```java
@Service
public class LoginAttemptService {
    
    private final Cache<String, Integer> attemptCache;
    private final Cache<String, Boolean> lockCache;
    
    public LoginAttemptService() {
        attemptCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();
        
        lockCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    }
    
    public void loginFailed(String username) {
        int attempts = Optional.ofNullable(attemptCache.getIfPresent(username))
            .orElse(0);
        attemptCache.put(username, attempts + 1);
        
        if (attempts >= 5) {
            lockCache.put(username, true);
        }
    }
    
    public boolean isBlocked(String username) {
        return Optional.ofNullable(lockCache.getIfPresent(username))
            .orElse(false);
    }
}
```

#### 场景：Token 过期处理
- **当** JWT Token 过期时
- **那么** 必须：
```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.expiration.access:7200000}")  // 2 小时
    private long accessExpiration;
    
    @Value("${jwt.expiration.refresh:604800000}")  // 7 天
    private long refreshExpiration;
    
    public String generateAccessToken(Authentication auth) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);
        
        return Jwts.builder()
            .setSubject(auth.getName())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Token 过期
            return false;
        } catch (JwtException e) {
            // Token 无效
            return false;
        }
    }
}
```

---

### 需求：敏感配置管理

系统必须安全地管理敏感配置，禁止硬编码密码。

#### 场景：环境变量配置
- **当** 配置敏感信息时
- **那么** 必须：
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}  # 从环境变量读取

jwt:
  secret: ${JWT_SECRET}  # 禁止硬编码
  expiration:
    access: ${JWT_ACCESS_EXPIRATION:7200000}
    refresh: ${JWT_REFRESH_EXPIRATION:604800000}
```

#### 场景：Docker Compose 配置
- **当** 配置 docker-compose 时
- **那么** 必须：
```yaml
# docker-compose.yml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}  # 从.env 读取
      MYSQL_PASSWORD: ${DB_PASSWORD}
  
  redis:
    command: redis-server --requirepass ${REDIS_PASSWORD}

# .env 文件（不提交到 Git）
MYSQL_ROOT_PASSWORD=<强密码>
DB_PASSWORD=<强密码>
REDIS_PASSWORD=<强密码>
JWT_SECRET=<强随机字符串>
```

#### 场景：Git 忽略
- **当** 配置 Git 时
- **那么** 必须：
```gitignore
# .gitignore
.env
.env.local
.env.production
**/.env
config/.env
deploy/config/.env
```

#### 场景：Secrets 管理（生产环境）
- **当** 部署到生产环境时
- **那么** 必须：
```bash
# 使用 HashiCorp Vault
vault kv put secret/his_agent/database \
  username=hisagent \
  password=<强密码>

# 使用 AWS Secrets Manager
aws secretsmanager create-secret \
  --name his_agent/prod/database \
  --secret-string '{"username":"hisagent","password":"<强密码>"}'
```

#### 场景：密码强度要求
- **当** 生成密码时
- **那么** 必须：
```yaml
# 密码策略
password_policy:
  min_length: 16
  require_uppercase: true
  require_lowercase: true
  require_digit: true
  require_special: true
  exclude_common: true  # 排除常见密码
```

---

### 需求：安全审计日志

系统必须实现完整的安全审计日志。

#### 场景：审计事件记录
- **当** 发生安全事件时
- **那么** 必须记录：
```java
@Component
public class SecurityAuditLogger {
    
    private static final Logger auditLogger = 
        LoggerFactory.getLogger("SECURITY_AUDIT");
    
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        auditLogger.info("AUTH_SUCCESS | user={} | ip={} | timestamp={}",
            event.getAuthentication().getName(),
            getClientIp(event),
            Instant.now()
        );
    }
    
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        auditLogger.warn("AUTH_FAILURE | user={} | ip={} | timestamp={}",
            event.getAuthentication().getName(),
            getClientIp(event),
            Instant.now()
        );
    }
}
```

#### 场景：审计日志内容
- **当** 记录审计日志时
- **那么** 必须包含：
  - 事件类型（登录、登出、权限变更）
  - 用户标识（用户名、用户 ID）
  - 时间戳
  - IP 地址
  - User-Agent
  - 操作结果（成功/失败）
  - traceId（链路追踪）

---

## 验收标准

### Spring Security
- [ ] spring-boot-starter-security 依赖已添加
- [ ] SecurityConfig 配置正确
- [ ] JWT 认证过滤器正常工作
- [ ] CORS 配置正确（生产环境禁止通配符）
- [ ] CSRF 防护生效

### SQL 注入防护
- [ ] 所有查询使用参数绑定
- [ ] 无字符串拼接 SQL
- [ ] 动态排序使用白名单

### XSS 防护
- [ ] 输入验证实现
- [ ] 输出编码实现
- [ ] 安全响应头配置（X-Frame-Options, CSP）

### CSRF 防护
- [ ] 双令牌机制或 SameSite Cookie
- [ ] 来源验证实现

### 认证安全
- [ ] BCrypt 密码加密（cost ≥ 12）
- [ ] 密码策略强制执行
- [ ] 登录失败限制（5 次失败锁定 30 分钟）
- [ ] JWT Token 过期处理正确

### 敏感配置
- [ ] 无硬编码密码
- [ ] .env 文件不提交到 Git
- [ ] Docker Compose 使用环境变量
- [ ] 生产环境使用 Secrets 管理工具

### 审计日志
- [ ] 安全事件完整记录
- [ ] 审计日志独立存储
- [ ] 日志包含必要字段（用户、IP、时间、结果）
