# 基础架构与开发集成

**日期**: 2026-03-11  
**状态**: 实施中  
**变更名称**: `enhance-openspec-constraints`

---

## 1. 为什么 (Why)

### 现状问题

当前项目开发基础设施存在以下差距：

1. **无自动化 CI/CD**: 代码提交后无自动构建、测试、部署
2. **无代码质量检查**: 提交前无自动 Lint、格式化检查
3. **无统一开发环境**: 开发者需手动启动多个服务，新人上手成本高
4. **无可视化运维**: 部署、监控、日志查看依赖命令行
5. **缺少生产级约束**: API 设计、数据安全、测试策略、性能约束、可观测性、**缓存策略**等方面缺少规范
6. **HIS 集成无标准**: 缺少统一的适配器架构，难以支持多 HIS 厂商
7. **技术选型需明确**: 前后端技术栈需要在规范中明确定义（前端 Vue 3 + TypeScript，后端 Java 17 + Spring Boot）
8. **数据库规范缺失**: 需要使用 MySQL 8+ 作为统一数据库，缺少相关约束

### 业务价值

实施基础架构与开发集成后：

- **开发效率提升 50%**: 一键启动、自动化部署
- **代码质量保障**: 提交前自动检查，减少 review 负担
- **运维可视化**: 降低运维门槛，非技术人员也可部署
- **生产级标准**: 确保系统达到医疗级生产要求
- **HIS 快速对接**: 适配器模式支持快速接入新 HIS 厂商
- **性能提升**: 通过 Redis 缓存策略，减少数据库查询，提升响应速度
- **技术栈统一**: 明确前后端技术选型，降低维护成本

---

## 2. 目标 / 非目标

### 目标

#### 开发体验
- 统一启动脚本 (`dev.sh`)，一键启动所有服务
- Git Hooks 自动检查（ESLint、Checkstyle、TypeCheck）
- 开发环境容器化（DevContainer 可选）

#### CI/CD 自动化
- GitHub 开发（CI 流水线自动构建、测试）
- GitLab 部署（生产环境自动/手动部署）
- 一键部署到测试/生产服务器

#### 可视化运维 (DevPortal)
- 服务器状态监控（CPU、内存、磁盘）
- 服务健康检查（前端、后端、数据库、Redis）
- 一键部署、重启、停止
- 配置文件在线编辑
- API 调用日志查看（实时 WebSocket）

#### 生产级约束
- API 接口出入参结构标准化
- 患者数据脱敏规范（姓名、身份证、手机号）
- 集成测试和 E2E 测试规范
- Mock 策略（避免开发测试调用付费服务）
- 并发性能约束、压测策略
- **并发控制规范**（线程池隔离、分布式锁、限流器）
- 运维可观测性约束（JSON 结构化日志）
- 监控告警策略、熔断降级策略
- 前端 Pinia 状态管理、表单验证规范
- 数据生命周期管理（软删除、录音数据）
- **缓存策略规范**（Redis 缓存、过期时间、更新策略）

#### HIS 集成架构
- 适配器模式支持多 HIS 厂商
- 首家对接 IIH，后续扩展东软、卫宁等
- Mock 测试沙箱支持离线开发

### 非目标

- ❌ 自动化代码 Review 工具（由人工执行）
- ❌ 复杂的多环境管理（初期仅 Test/Prod）
- ❌ 完整的 FHIR/HL7 支持（首期仅 REST/SOAP）
- ❌ 分布式缓存集群（初期使用单机 Redis）
- ❌ 多数据库支持（仅支持 MySQL 8+）
- ❌ 复杂的分布式事务（初期使用本地事务）
- ❌ 自定义并发框架（使用成熟库：Resilience4j、Redisson）

---

## 3. 功能 (Capabilities)

### 3.1 开发环境集成

#### 功能：统一启动脚本

**用户故事**:
> 作为开发者，我希望一键启动所有服务，这样我不需要手动配置和启动多个进程。

**验收标准**:
- [x] 执行 `./dev.sh` 后自动启动 MySQL、Redis、后端、前端
- [x] 服务启动后显示访问地址
- [x] 按 Ctrl+C 后优雅停止所有服务
- [x] 支持 macOS、Linux、Windows (WSL)

**实现状态**: ✅ 已完成 (`dev.sh`, `docker-compose.dev.yml`)

---

#### 功能：Git Hooks 自动检查

**用户故事**:
> 作为开发者，我希望提交代码前自动检查代码质量，这样避免低级错误进入代码库。

**验收标准**:
- [x] pre-commit hook 自动运行 ESLint（前端）
- [x] pre-commit hook 自动运行 Checkstyle（后端）
- [x] pre-commit hook 自动验证 OpenSpec 规格
- [x] 检查失败时阻止提交

**实现状态**: ✅ 已完成 (`.husky/pre-commit`, `.lintstagedrc.json`)

---

#### 功能：GitHub Actions CI/CD

**用户故事**:
> 作为开发者，我希望 Push 代码后自动构建和测试，这样我能快速发现代码问题。

**验收标准**:
- [x] Push 到 develop 分支触发 CI
- [x] 自动运行代码质量检查（ESLint、Checkstyle）
- [x] 自动运行单元测试（Vitest、JUnit）
- [x] 构建成功后推送 Docker 镜像

**实现状态**: ✅ 已完成 (`.github/workflows/ci.yml`)

---

### 3.2 可视化运维 (DevPortal)

#### 功能：服务器状态监控

**用户故事**:
> 作为运维人员，我希望实时查看服务器状态，这样能快速发现资源瓶颈。

**验收标准**:
- [x] 显示 CPU 使用率（实时更新）
- [x] 显示内存使用率（实时更新）
- [x] 超过阈值时显示告警

**实现状态**: ✅ 已完成 (`DevPortalController.java`, `DevPortal.vue`)

---

#### 功能：服务健康检查

**用户故事**:
> 作为运维人员，我希望一键检查所有服务健康状态，这样快速定位故障服务。

**验收标准**:
- [x] 显示前端服务状态（✅/❌）
- [x] 显示后端服务状态（✅/❌）
- [x] 显示数据库连接状态（✅/❌）
- [x] 显示 Redis 连接状态（✅/❌）

**实现状态**: ✅ 已完成

---

### 3.3 生产级约束

#### 功能：API 设计规范

**需求**:
- 统一响应结构 `ApiResponse<T>` 和 `PageResponse<T>`
- 全局异常处理器，统一错误码规范
- 分页参数验证拦截器
- 所有 API 添加 traceId 支持

**状态**: 📋 待实施

---

#### 功能：数据安全与脱敏

**需求**:
- 数据脱敏工具类（姓名、身份证、手机号）
- @DataMasking 注解和切面
- 数据库加密字段配置
- PII 数据访问审计日志

**状态**: 📋 待实施

---

#### 功能：测试策略

**需求**:
- TestContainers 集成测试环境
- WireMock 模拟外部服务
- Playwright E2E 测试框架
- 覆盖率检查和报告

**状态**: 📋 待实施

---

#### 功能：监控告警

**需求**:
- JSON 结构化日志（Logback）
- OpenTelemetry 链路追踪
- Spring Boot Actuator + Micrometer
- Prometheus + Grafana 监控
- 告警规则（P0/P1/P2）

**状态**: 📋 待实施

---

#### 功能：熔断降级

**需求**:
- Resilience4j 熔断器
- 语音识别降级链（讯飞→阿里云→Whisper→手动）
- LLM 服务降级链（Qwen→Claude→Ollama→模板）
- 限流器、重试机制

**状态**: 📋 待实施

---

### 3.4 HIS 集成架构

#### 功能：HIS 适配器工厂

**用户故事**:
> 作为开发者，我希望快速接入新 HIS 厂商，这样不需要修改核心代码。

**验收标准**:
- [ ] 定义统一的 `HisAdapter` 接口
- [ ] 实现通用 REST 适配器
- [ ] 实现通用 SOAP 适配器
- [ ] 通过配置选择适配器类型

**状态**: 📋 待实施

---

### 3.5 技术选型规范

#### 功能：前后端技术栈明确

**需求**:
- 前端：Vue 3.4+ + TypeScript 5+ + Vite 6+
- 后端：Java 17+ + Spring Boot 3.x + Maven 3.9+
- 数据库：MySQL 8+（唯一支持的数据库）
- 缓存：Redis 7+（可选，用于会话和 API 缓存）

**状态**: 📋 待实施

---

### 3.6 缓存策略

#### 功能：统一缓存管理

**用户故事**:
> 作为开发者，我希望使用统一的缓存策略，这样可以减少数据库查询，提升系统性能。

**验收标准**:
- [ ] 使用 Redis 作为统一缓存存储
- [ ] 定义缓存键命名规范
- [ ] 实现缓存过期时间管理
- [ ] 实现缓存穿透、雪崩防护
- [ ] 实现缓存更新策略（先更新数据库，再删除缓存）

**状态**: 📋 待实施

---

### 3.7 并发控制

#### 功能：多层并发控制

**用户故事**:
> 作为系统架构师，我希望实施多层并发控制，这样保证系统在高并发下的稳定性和响应性能。

**验收标准**:
- [ ] 实现线程池隔离（核心业务、语音处理、LLM 调用、HIS 集成）
- [ ] 实现分布式锁（Redisson，定时任务互斥执行）
- [ ] 实现限流器（Resilience4j RateLimiter）
- [ ] 实现会话并发管理（单用户最大 3 个并发会话）
- [ ] 实现接口幂等性（防重复提交）
- [ ] 实现数据库乐观锁（@Version）
- [ ] 配置线程池监控指标
- [ ] 压测达到性能指标（P95 < 500ms，并发用户 ≥ 100）

**依赖**:
- Resilience4j（限流器、熔断器）
- Redisson（分布式锁）
- Redis 7+（分布式锁、会话管理）

**状态**: 📋 待实施

---

### 3.8 前后端通信契约

#### 功能：统一通信规范

**用户故事**:
> 作为前端开发者，我希望有统一的 HTTP 客户端和错误处理机制，这样能快速开发并保证用户体验一致性。

**验收标准**:
- [ ] 实现 Axios 统一 HTTP 客户端
- [ ] 实现请求/响应拦截器
- [ ] 实现 JWT Token 自动刷新
- [ ] 实现 CORS 配置（生产环境严格限制）
- [ ] 实现全链路 traceId 追踪
- [ ] 实现前端容错处理（错误边界、降级 UI）

**状态**: 📋 待实施

---

### 3.9 安全规范（OWASP TOP 10）

#### 功能：完整的安全防护体系

**用户故事**:
> 作为安全工程师，我希望系统实现 OWASP TOP 10 防护措施，这样保证患者数据安全合规。

**验收标准**:
- [ ] 实现 Spring Security 认证授权
- [ ] 实现 SQL 注入防护（参数化查询）
- [ ] 实现 XSS 防护（输入验证、输出编码）
- [ ] 实现 CSRF 防护（Token 验证）
- [ ] 实现敏感配置管理（无硬编码密码）
- [ ] 实现安全审计日志

**状态**: 📋 待实施

---

### 3.10 数据库规范

#### 功能：标准化数据库管理

**用户故事**:
> 作为 DBA，我希望有统一的数据库规范，这样便于维护和性能优化。

**验收标准**:
- [ ] 表/字段命名规范实施
- [ ] 索引设计规范实施
- [ ] 慢查询监控配置（阈值 2 秒）
- [ ] Flyway 迁移文件命名规范
- [ ] 连接池配置优化（HikariCP）

**状态**: 📋 待实施

---

### 3.11 前端性能规范

#### 功能：性能优化约束

**用户故事**:
> 作为前端用户，我希望页面加载快速流畅，这样提升工作效率。

**验收标准**:
- [ ] 首屏加载时间 ≤ 2.5s（LCP）
- [ ] 路由懒加载实施
- [ ] 静态资源缓存策略（Nginx）
- [ ] 图片优化（WebP、懒加载）
- [ ] Tree Shaking 实施
- [ ] 虚拟滚动实施（长列表）

**状态**: 📋 待实施

---

### 3.12 部署回滚策略

#### 功能：快速回滚能力

**用户故事**:
> 作为运维工程师，我希望 5 分钟内快速回滚，这样故障时能迅速恢复服务。

**验收标准**:
- [ ] Docker 镜像版本管理（语义化版本）
- [ ] 回滚脚本实现（应用 + 数据库）
- [ ] 蓝绿部署配置（Nginx）
- [ ] CI/CD 回滚 workflow
- [ ] 版本追踪（/actuator/info）

**状态**: 📋 待实施

---

## 4. 影响

### 受影响的系统

**前端**:
- DevPortal 页面
- ESLint + Prettier 配置
- Git Hooks 集成
- Vue 3 + TypeScript 技术栈

**后端**:
- DevPortal Controller
- Checkstyle 配置
- API 响应结构
- 数据脱敏模块
- 监控告警模块
- HIS 适配器模块
- **缓存管理模块（新增）**
- **并发控制模块（新增）**
- Java 17 + Spring Boot 技术栈

**数据库**:
- **MySQL 8+（唯一支持的数据库）**
- Flyway 数据库迁移

**缓存**:
- **Redis 7+（缓存策略实施）**

**基础设施**:
- GitHub Actions CI/CD
- Docker Compose 配置
- GitLab 部署流程

### 依赖项

**必需**:
- Docker & Docker Compose
- Node.js 20+
- JDK 17+
- Maven 3.9+
- **MySQL 8+**
- **Redis 7+（缓存策略、分布式锁）**
- **Resilience4j（限流器、熔断器）**
- **Redisson（分布式锁客户端）**

**可选**:
- VS Code DevContainer
- Grafana + Prometheus
- TestContainers
- **Spring Data Redis**
- **Caffeine（本地缓存）**
- **Micrometer（线程池监控）**

---

## 5. 实施计划

### Phase 1: 基础开发框架 ✅ COMPLETED

**周期**: Week 1-2

**任务**:
- [x] GitHub Actions CI 流水线
- [x] Husky Git Hooks
- [x] dev.sh 启动脚本
- [x] ESLint + Prettier 配置
- [x] Checkstyle 配置
- [x] DevPortal 基础功能

**状态**: ✅ 已完成 (22/29 任务)

---

### Phase 2: 生产级约束

**周期**: Week 3-6

**任务**:
- [ ] API 设计规范实现
- [ ] 数据脱敏模块
- [ ] 测试框架配置
- [ ] Mock 策略实现

**状态**: 📋 待开始

---

### Phase 3: 监控与高可用

**周期**: Week 7-10

**任务**:
- [ ] 监控告警系统
- [ ] 熔断降级实现
- [ ] 性能优化
- [ ] 压测执行

**状态**: 📋 待开始

---

### Phase 4: HIS 集成

**周期**: Week 11-14

**任务**:
- [ ] HIS 适配器架构
- [ ] IIH 适配器实现
- [ ] Mock HIS 测试服务
- [ ] 医院联调测试

**状态**: 📋 待开始

---

## 6. 参考资料

- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Husky 文档](https://typicode.github.io/husky/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Resilience4j](https://resilience4j.readme.io/)

---

## 附录：文件清单

```
openspec/changes/enhance-openspec-constraints/
├── proposal.md (本文档)
├── design.md (技术设计)
├── tasks.md (实施任务)
└── specs/
    ├── api-constraints.md
    ├── data-masking.md
    ├── testing-strategy.md
    ├── mock-strategy.md
    ├── monitoring-alerting.md
    └── circuit-breaker.md

项目根目录/
├── .github/workflows/ci.yml ✅
├── .husky/pre-commit ✅
├── .lintstagedrc.json ✅
├── docker-compose.dev.yml ✅
├── dev.sh ✅
├── his_agent-frontend/
│   ├── .eslintrc.js ✅
│   └── .prettierrc ✅
└── his_agent-backend/
    ├── checkstyle.xml ✅
    └── src/main/java/.../DevPortalController.java ✅
```
