# 技术栈规范

**版本**: 1.0  
**日期**: 2026-03-12  
**状态**: 已批准  
**变更**: enhance-openspec-constraints

---

## 1. 前端技术栈

### 1.1 核心框架

| 技术 | 版本 | 说明 | 强制要求 |
|------|------|------|----------|
| **语言** | TypeScript 5+ | 严格模式 | ✅ 必须 |
| **框架** | Vue 3.4+ | Composition API | ✅ 必须 |
| **构建工具** | Vite 6+ | 快速开发和构建 | ✅ 必须 |
| **UI 组件库** | Element Plus 2.8+ | 企业级 UI 组件 | ✅ 必须 |
| **状态管理** | Pinia 2.2+ | Vue 官方状态管理 | ✅ 必须 |
| **路由** | Vue Router 4.4+ | 前端路由 | ✅ 必须 |

### 1.2 开发工具

| 工具 | 版本 | 用途 |
|------|------|------|
| ESLint | 9.x | 代码linting |
| Prettier | 3.x | 代码格式化 |
| Vitest | 2.1+ | 单元测试框架 |
| Playwright | 1.40+ | E2E 测试 |

### 1.3 前端依赖约束

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.4.0",
    "pinia": "^2.2.0",
    "element-plus": "^2.8.0",
    "axios": "^1.6.0",
    "typescript": "^5.0.0"
  },
  "devDependencies": {
    "vite": "^6.0.0",
    "vitest": "^2.1.0",
    "eslint": "^9.0.0",
    "prettier": "^3.0.0"
  }
}
```

---

## 2. 后端技术栈

### 2.1 核心框架

| 技术 | 版本 | 说明 | 强制要求 |
|------|------|------|----------|
| **语言** | Java 17+ | LTS 版本 | ✅ 必须 |
| **框架** | Spring Boot 3.4.x | 最新稳定版 | ✅ 必须 |
| **构建工具** | Maven 3.9+ | 依赖管理 | ✅ 必须 |
| **AI 集成** | Spring AI 1.0.0-M5+ | 统一 LLM 接口 | ✅ 必须 |
| **代码简化** | Lombok 1.18.30+ | 减少样板代码 | ✅ 推荐 |
| **对象映射** | MapStruct 1.5.5+ | DTO 转换 | ✅ 推荐 |

### 2.2 数据存储

| 技术 | 版本 | 说明 | 强制要求 |
|------|------|------|----------|
| **关系数据库** | MySQL 8+ | 唯一支持的 RDBMS | ✅ 必须 |
| **连接池** | HikariCP 5.x | 高性能连接池 | ✅ 必须 |
| **数据库迁移** | Flyway 10.x | 版本化迁移 | ✅ 必须 |
| **缓存** | Redis 7+ | 分布式缓存 | ✅ 必须 |
| **本地缓存** | Caffeine 3.x | L1 缓存（可选） | ⚠️ 可选 |

### 2.3 后端依赖约束

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.4.2</spring-boot.version>
    <spring-ai.version>1.0.0-M5</spring-ai.version>
    <lombok.version>1.18.30</lombok.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <mysql.version>8.0.33</mysql.version>
</properties>

<dependencies>
    <!-- Spring Boot Core -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>${spring-boot.version}</version>
    </dependency>
    
    <!-- Spring Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
        <version>${spring-boot.version}</version>
    </dependency>
    
    <!-- MySQL Driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>${mysql.version}</version>
    </dependency>
</dependencies>
```

---

## 3. 数据库选型

### 3.1 唯一支持的数据库

**MySQL 8+** 是本项目唯一支持的关系型数据库。

| 特性 | 要求 |
|------|------|
| **版本** | 8.0+ |
| **字符集** | UTF8MB4 |
| **排序规则** | utf8mb4_unicode_ci |
| **存储引擎** | InnoDB |
| **事务隔离** | READ-COMMITTED |

### 3.2 禁止使用的数据库

以下数据库**不被支持**：

- ❌ PostgreSQL
- ❌ Oracle
- ❌ SQL Server
- ❌ MariaDB（除非明确测试通过）
- ❌ SQLite

### 3.3 数据库配置规范

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:his_agent}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 4. 技术选型理由

### 4.1 为什么选择 Vue 3？

- **Composition API**: 更好的代码组织和复用
- **TypeScript 支持**: 一流的类型支持
- **性能**: 比 Vue 2 快 1.3-2 倍
- **生态**: Element Plus、Pinia 等完善的生态

### 4.2 为什么选择 Java 17？

- **LTS 版本**: 长期支持，稳定可靠
- **性能提升**: 相比 Java 8 有显著性能提升
- **新特性**: Record、Pattern Matching、Switch 表达式等
- **Spring Boot 3 要求**: Spring Boot 3.x 最低要求 Java 17

### 4.3 为什么选择 MySQL 8？

- **医院标准**: 大多数医院 HIS 系统使用 MySQL
- **JSON 支持**: 原生 JSON 数据类型和函数
- **性能**: 相比 5.7 有 20%+ 性能提升
- **窗口函数**: 支持复杂分析查询

### 4.4 为什么选择 Redis 7？

- **性能**: 单线程模型，极高吞吐量
- **数据结构**: 丰富的数据结构支持
- **持久化**: RDB + AOF 双重保障
- **集群**: 原生集群支持

---

## 5. 版本兼容性矩阵

| 组件 | 最低版本 | 推荐版本 | 最高版本 |
|------|----------|----------|----------|
| Java | 17 | 17.0.18+ | 21 |
| Spring Boot | 3.4.0 | 3.4.2 | 3.4.x |
| Vue | 3.4.0 | 3.4.x | 3.x |
| TypeScript | 5.0 | 5.3+ | 5.x |
| MySQL | 8.0 | 8.0.33+ | 8.x |
| Redis | 7.0 | 7.0.15+ | 7.x |
| Maven | 3.9.0 | 3.9.6 | 3.x |

---

## 6. 开发环境要求

### 6.1 必需工具

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | Java 开发 |
| Node.js | 20+ | 前端开发 |
| Maven | 3.9+ | 后端构建 |
| Git | 2.40+ | 版本控制 |
| Docker | 24+ | 容器化部署（可选） |

### 6.2 推荐 IDE

| IDE | 用途 | 推荐插件 |
|-----|------|----------|
| IntelliJ IDEA | Java 开发 | Lombok, MapStruct, Spring Assistant |
| VS Code | 前端开发 | Volar, ESLint, Prettier |
| DataGrip | 数据库 | MySQL 支持 |

---

## 7. 技术债务管理

### 7.1 版本升级策略

- **小版本**: 自动升级（Patch）
- **中版本**: 评估后升级（Minor）
- **大版本**: 充分测试后升级（Major）

### 7.2 依赖审查

- 每月运行 `npm audit` 和 `mvn dependency-check:check`
- 及时修复高危漏洞
- 避免引入不必要的依赖

---

## 8. 变更历史

| 版本 | 日期 | 变更内容 | 批准人 |
|------|------|----------|--------|
| 1.0 | 2026-03-12 | 初始版本，明确技术栈选型 | - |

---

## 9. 参考文档

- [Vue 3 官方文档](https://vuejs.org/)
- [Spring Boot 3 官方文档](https://spring.io/projects/spring-boot)
- [MySQL 8 官方文档](https://dev.mysql.com/doc/refman/8.0/en/)
- [Redis 7 官方文档](https://redis.io/documentation)
