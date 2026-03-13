# 前后端通信契约规范

**版本**: 1.0  
**日期**: 2026-03-12  
**状态**: 新增

---

## 新增需求

### 需求：跨域 CORS 配置

系统必须实现严格的 CORS 策略，平衡安全性和可用性。

#### 场景：后端 CORS 配置
- **当** Spring Boot 应用接收跨域请求时
- **那么** 必须配置：
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://his-agent.hospital.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
        config.setExposedHeaders(List.of("X-Request-ID", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

#### 场景：生产环境 CORS
- **当** 部署到生产环境时
- **那么** 必须：
  - 禁止使用 `setAllowedOrigins(List.of("*"))`
  - 明确指定允许的域名列表
  - 通过环境变量配置允许的原点

#### 场景：开发环境 CORS
- **当** 开发环境运行时
- **那么** 可以：
  - 允许 `http://localhost:3000`
  - 允许 `http://127.0.0.1:3000`
  - 通过 Vite 代理绕过 CORS

---

### 需求：前端 HTTP 客户端配置

前端必须使用 Axios 作为统一的 HTTP 客户端，并实现拦截器。

#### 场景：Axios 实例创建
- **当** 创建 HTTP 客户端时
- **那么** 必须配置：
```typescript
// src/services/http.ts
import axios from 'axios';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});
```

#### 场景：请求拦截器
- **当** 发起请求时
- **那么** 必须：
  - 自动添加 JWT Token（如果存在）
  - 生成并添加 traceId
  - 添加请求时间戳
```typescript
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  
  config.headers['X-Request-ID'] = crypto.randomUUID();
  config.headers['X-Request-Timestamp'] = Date.now().toString();
  
  return config;
});
```

#### 场景：响应拦截器
- **当** 接收响应时
- **那么** 必须：
  - 统一处理业务错误码
  - 处理 401 未授权（触发 token 刷新）
  - 处理 403 禁止访问
  - 处理 429 请求限流
  - 处理 500 服务器错误
```typescript
http.interceptors.response.use(
  (response) => {
    const { code, data, message } = response.data;
    
    if (code !== 200) {
      handleError(code, message);
      return Promise.reject(new Error(message));
    }
    
    return data;
  },
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          handleUnauthorized(error);
          break;
        case 403:
          ElMessage.error('无权访问');
          break;
        case 429:
          ElMessage.error('请求过于频繁，请稍后重试');
          break;
        case 500:
          ElMessage.error('服务器错误，请稍后重试');
          break;
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接');
    } else {
      ElMessage.error('网络错误，请检查网络连接');
    }
    
    return Promise.reject(error);
  }
);
```

---

### 需求：JWT Token 刷新机制

系统必须实现 JWT Token 自动刷新，避免用户频繁重新登录。

#### 场景：Token 存储
- **当** 用户登录成功后
- **那么** 必须：
  - Access Token 存储在内存（Pinia Store）
  - Refresh Token 存储在 HttpOnly Cookie
  - 禁止将 Token 存储在 localStorage（XSS 风险）

#### 场景：Token 刷新触发
- **当** API 返回 401 错误时
- **那么** 必须：
  - 检查是否有 Refresh Token
  - 使用 Refresh Token 请求新的 Access Token
  - 刷新成功后重试原请求
  - 刷新失败则跳转到登录页

#### 场景：刷新 Token 实现
```typescript
// src/services/auth.ts
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(token: string) {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
}

async function refreshToken(): Promise<string> {
  const response = await axios.post('/api/v1/auth/refresh', null, {
    withCredentials: true, // 发送 Refresh Token Cookie
  });
  return response.data.access_token;
}

http.interceptors.response.use(
  async (error) => {
    const { config, response } = error;
    
    if (response?.status !== 401) {
      return Promise.reject(error);
    }
    
    if (!isRefreshing) {
      isRefreshing = true;
      
      try {
        const newToken = await refreshToken();
        onRefreshed(newToken);
        return http(config);
      } catch (refreshError) {
        // 刷新失败，跳转登录
        router.push('/login');
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    } else {
      // 等待刷新完成
      return new Promise((resolve) => {
        subscribeTokenRefresh((token) => {
          config.headers.Authorization = `Bearer ${token}`;
          resolve(http(config));
        });
      });
    }
  }
);
```

#### 场景：Token 有效期
- **当** 配置 JWT 时
- **那么** 必须：
  - Access Token 有效期：2 小时
  - Refresh Token 有效期：7 天
  - Refresh Token 使用一次后轮换（防止重放攻击）

---

### 需求：异常追踪（TraceId）

系统必须实现全链路异常追踪，便于问题定位。

#### 场景：TraceId 生成
- **当** 请求进入系统时
- **那么** 必须：
  - 前端生成 UUID 作为 traceId
  - 通过 `X-Request-ID` 请求头传递
  - 如果前端未提供，后端生成 traceId

#### 场景：TraceId 传递
- **当** 后端处理请求时
- **那么** 必须：
  - 从 `X-Request-ID` 读取 traceId
  - 存入 MDC（Mapped Diagnostic Context）
  - 所有日志自动包含 traceId
  - 调用外部服务时传递 traceId
```java
@Component
public class TraceIdFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String traceId = httpRequest.getHeader("X-Request-ID");
        
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        
        MDC.put("traceId", traceId);
        ((HttpServletResponse) response).setHeader("X-Request-ID", traceId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

#### 场景：日志输出
- **当** 记录日志时
- **那么** 必须：
  - 使用 SLF4J 记录
  - 日志自动包含 traceId
  - 错误日志包含完整堆栈
```xml
<!-- logback-spring.xml -->
<encoder>
  <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
</encoder>
```

#### 场景：异常响应
- **当** 发生异常时
- **那么** 响应必须包含：
```json
{
  "code": 50000,
  "message": "服务器内部错误",
  "data": null,
  "timestamp": 1710144000,
  "traceId": "abc123-def456",
  "path": "/api/v1/consultations"
}
```

---

### 需求：前端容错处理

前端必须实现完善的容错机制，提升用户体验。

#### 场景：网络异常处理
- **当** 网络请求失败时
- **那么** 必须：
  - 显示友好的错误提示（不显示技术细节）
  - 提供重试按钮
  - 保持页面可交互（不阻塞整个应用）

#### 场景：降级 UI 展示
- **当** 数据加载失败时
- **那么** 必须：
  - 显示空状态页面（Empty State）
  - 提供刷新按钮
  - 保留上次成功加载的数据（如适用）

#### 场景：加载状态
- **当** 请求进行中时
- **那么** 必须：
  - 按钮显示 loading 状态
  - 防止重复提交
  - 超过 5 秒显示加载提示

#### 场景：错误边界
- **当** Vue 组件渲染失败时
- **那么** 必须：
  - 使用 `<error-boundary>` 组件捕获错误
  - 显示降级 UI
  - 记录错误到监控系统

---

### 需求：请求幂等性

写操作必须实现幂等性，防止重复提交导致数据问题。

#### 场景：幂等令牌
- **当** 发起写请求（POST/PUT/DELETE）时
- **那么** 前端必须：
  - 生成幂等令牌（UUID）
  - 通过 `Idempotency-Key` 请求头发送
  - 按钮禁用防止重复点击

#### 场景：幂等性保证
- **当** 后端收到重复请求时
- **那么** 必须：
  - 检查幂等令牌是否已处理
  - 已处理则返回缓存的响应
  - 不重复执行业务逻辑

---

## 修改需求

（无现有需求修改）

---

## 移除需求

（无需求移除）

---

## 验收标准

- [ ] CORS 配置正确（生产环境禁止通配符）
- [ ] Axios 拦截器实现（请求 + 响应）
- [ ] JWT Token 自动刷新正常工作
- [ ] Token 存储安全（HttpOnly Cookie + 内存）
- [ ] TraceId 全链路传递
- [ ] 日志包含 traceId
- [ ] 前端错误提示友好
- [ ] 网络异常有重试机制
- [ ] 请求幂等性实现
- [ ] 401/403/429/500 错误正确处理
