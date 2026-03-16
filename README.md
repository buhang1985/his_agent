# his_agent

> 医疗 AI 助手 - 嵌入 HIS 系统的智能诊疗辅助工具

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4+-42b883.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 项目简介

his_agent 是一个嵌入现有医院信息系统 (HIS) 的医疗 AI 助手，通过 Web 页面嵌入方式 (iframe) 集成到 HIS 系统中。核心功能包括：

- 🎤 **智能问诊** - 语音录入医患对话，实时转写生成结构化病历
- 🩺 **诊断建议** - 基于症状和病史提供鉴别诊断参考
- 📋 **病历生成** - 自动生成符合医疗规范的 SOAP 格式电子病历
- 💊 **医学知识查询** - 快速检索药品信息、疾病指南、相互作用检查

## 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        HIS 宿主系统                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    iframe 嵌入区域                       │    │
│  │  ┌───────────────────────────────────────────────────┐  │    │
│  │  │     his_agent Frontend (Vue 3 + TypeScript)       │  │    │
│  │  │  ┌─────────┐ ┌─────────┐ ┌─────────────────────┐  │  │    │
│  │  │  │ 语音录入 │ │ 诊断建议 │ │   病历生成/展示     │  │  │    │
│  │  │  └─────────┘ └─────────┘ └─────────────────────┘  │  │    │
│  │  └───────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                    REST API / WebSocket                          │
└──────────────────────────────┼──────────────────────────────────┘
                                │
                     ┌──────────┴──────────┐
                     ▼                     ▼
            ┌────────────────┐   ┌────────────────┐
            │  Spring Boot   │   │  本地 LLM 服务  │
            │  后端服务       │   │ (Ollama/vLLM)  │
            └────────────────┘   └────────────────┘
                                │
                    ┌───────────┴─────────────┐
                    │                         │
                    ▼                         ▼
            ┌──────────────┐          ┌──────────────┐
            │   MySQL 8+   │          │  Redis 7+    │
            │   数据库     │          │   缓存       │
            └──────────────┘          └──────────────┘
                                │
                    ┌───────────┴─────────────┐
                    │                         │
                    ▼                         ▼
            ┌──────────────┐          ┌──────────────┐
            │ Prometheus   │          │  Grafana     │
            │  (指标收集)  │          │  (可视化)    │
            │  :9090       │          │  :3000       │
            └──────────────┘          └──────────────┘
                    │                         │
                    ▼                         ▼
            ┌──────────────┐          ┌──────────────┐
            │ Node Exporter│          │ MySQL Exporter│
            │   :9100      │          │   :9104      │
            └──────────────┘          └──────────────┘
                    │
                    ▼
            ┌──────────────┐
            │ Redis Exporter│
            │   :9121      │
            └──────────────┘
```

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | Java 17+ | LTS 版本 |
| **框架** | Spring Boot 3.4.x | 约定优于配置 |
| **LLM 集成** | Spring AI 1.0.0-M5+ | 统一 LLM 接口 |
| **构建工具** | Maven 3.9+ | 依赖管理 |
| **数据库** | MySQL 8+ | 关系型存储 |
| **测试** | JUnit 5 + Mockito | 单元测试 |
| **集成测试** | TestContainers | 容器化测试 |
| **覆盖率** | Jacoco | ≥80% 覆盖率要求 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | TypeScript 5+ | 类型安全 |
| **框架** | Vue 3.4+ | Composition API |
| **构建工具** | Vite 6+ | 快速开发 |
| **UI 框架** | Element Plus 2.8+ | 组件库 |
| **状态管理** | Pinia 2.2+ | Vue 状态管理 |
| **路由** | Vue Router 4.4+ | 前端路由 |
| **测试** | Vitest 2.1+ | Vite 原生测试 |

### 外部服务
| 服务 | 技术 | 部署方式 |
|------|------|----------|
| 语音识别 | Deepgram Nova-3 Medical | 云端 API |
| 语音识别 | Whisper.cpp | 本地部署 |
| LLM | Qwen (通义千问) | 阿里云 API |
| LLM | Claude | Anthropic API |
| LLM | Qwen2.5/Llama3 | Ollama 本地部署 |

### 监控系统
| 组件 | 端口 | 说明 |
|------|------|------|
| **Prometheus** | 9090 | 指标收集和存储 |
| **Grafana** | 3000 | 图表展示 (admin/admin) |
| **AlertManager** | 9093 | 告警管理 |
| **Node Exporter** | 9100 | 服务器性能指标 |
| **MySQL Exporter** | 9104 | MySQL 指标 |
| **Redis Exporter** | 9121 | Redis 指标 |
| **部署目录** | - | `/mnt/d/his-agent-monitoring` |
| **部署文档** | - | [docs/monitoring/](docs/monitoring/) |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20+
- Maven 3.9+
- MySQL 8+
- Redis 7+ (推荐)

### 监控系统部署（可选但推荐）

```bash
cd /home/yuzihao/workspace/his_agent/monitoring

# 运行自动安装脚本
chmod +x install-monitoring.sh
./install-monitoring.sh

# 验证部署
curl http://localhost:9090/-/healthy
curl http://localhost:3000/api/health
```

详细部署文档：[docs/monitoring/monitoring-deployment-guide.md](docs/monitoring/monitoring-deployment-guide.md)

### 后端启动

```bash
cd his_agent-backend

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，配置 API Key 和数据库连接

# 构建并启动（包含测试）
mvn clean install
mvn spring-boot:run

# 仅运行不测试
mvn spring-boot:run -DskipTests
```

### 前端启动

```bash
cd his_agent-frontend

# 安装依赖
npm install

# 配置环境变量
cp .env.example .env

# 启动开发服务器
npm run dev

# 运行测试
npm run test
```

## 项目结构

```
his_agent/
├── his_agent-backend/          # 后端 Spring Boot 项目
│   ├── src/main/java/
│   │   └── com/hisagent/
│   │       ├── controller/     # REST 控制器
│   │       ├── service/        # 业务逻辑
│   │       ├── repository/     # 数据访问
│   │       ├── model/          # 数据模型
│   │       ├── config/         # 配置类
│   │       └── ai/             # AI 服务集成
│   ├── src/test/java/          # 单元测试
│   ├── src/main/resources/
│   │   ├── application.yml     # 应用配置
│   │   └── db/migration/       # Flyway 迁移脚本
│   └── pom.xml
│
├── his_agent-frontend/         # 前端 Vue 项目
│   ├── src/
│   │   ├── components/         # Vue 组件
│   │   ├── composables/        # Composition API
│   │   ├── views/              # 页面视图
│   │   ├── services/           # API 服务
│   │   ├── stores/             # Pinia 状态
│   │   └── types/              # TypeScript 类型
│   ├── package.json
│   └── vite.config.ts
│
├── monitoring/                 # 监控系统
│   ├── prometheus/             # Prometheus 配置
│   ├── grafana/                # Grafana 配置
│   ├── alertmanager/           # AlertManager 配置
│   ├── install-monitoring.sh   # 自动安装脚本
│   └── README.md               # 监控部署说明
│
├── openspec/                   # OpenSpec 配置
│   ├── config.yaml
│   ├── specs/                  # 功能规格说明
│   └── changes/                # 变更工件
│
└── README.md
```

## 测试规范

### 后端测试

```bash
# 运行单元测试
mvn clean test

# 运行集成测试
mvn verify

# 生成覆盖率报告
mvn jacoco:report

# 检查覆盖率 (≥80%)
mvn jacoco:check
```

### 前端测试

```bash
# 运行单元测试
npm run test

# 运行带 UI 的测试
npm run test:ui

# 生成覆盖率报告
npm run test:coverage

# 类型检查
npm run typecheck
```

### 串行执行要求

**重要**: 每个任务必须等待上一个任务测试通过后才能执行下一个任务。

```
Task 1 → 测试通过 ✅ → Task 2 → 测试通过 ✅ → Task 3 → ...
         ↓                      ↓
      失败 ❌                失败 ❌
         ↓                      ↓
      修复 → 重测            修复 → 重测
```

## OpenSpec 使用指南

本项目使用 OpenSpec 进行 spec-driven 开发。

### 命令列表

| 命令 | 描述 |
|------|------|
| `/opsx-explore` | 进入探索模式，构思想法、调查问题、澄清需求 |
| `/opsx-propose` | 提案新变更，一步创建并生成所有产出物 |
| `/opsx-apply` | 实现 OpenSpec 变更中的任务 |
| `/opsx-archive` | 归档实验性工作流中已完成的变更 |

### 开发流程

```
1. 探索需求 → /opsx-explore <topic>
2. 创建变更 → /opsx-propose <feature-name>
3. 实施任务 → /opsx-apply <feature-name> (串行执行，测试通过后才执行下一个)
4. 归档变更 → /opsx-archive <feature-name>
```

### 规格说明文档

当前已定义的规格：
- [技术栈规格](openspec/specs/tech-stack.md)
- [智能问诊功能](openspec/specs/01-voice-consultation.md)

## 配置说明

### 环境变量

#### 后端 (.env)

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=his_agent
DB_USER=root
DB_PASSWORD=your_password

# LLM 配置
DASHSCOPE_API_KEY=your_dashscope_key  # 阿里云通义千问
ANTHROPIC_API_KEY=your_anthropic_key  # Claude

# 语音识别配置
DEEPGRAM_API_KEY=your_deepgram_key    # Deepgram 语音转写

# HIS 系统配置
HIS_BASE_URL=https://your-his-system.com
```

#### 前端 (.env)

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

## API 文档

启动后端后访问：
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 监控系统访问

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **AlertManager**: http://localhost:9093

## 安全与合规

- **数据加密**: HTTPS/TLS 传输加密，AES-GCM 静态加密
- **访问控制**: RBAC 权限管理，与医院 AD 集成
- **审计日志**: 完整操作日志，不含 PHI 内容
- **PII 保护**: 患者数据脱敏处理
- **紧急重定向**: 检测到紧急症状时提示就医

## 开发约定

- 遵循 [Conventional Commits](https://www.conventionalcommits.org/)
- 后端代码遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 前端代码使用 ESLint + Prettier 格式化
- **所有功能必须有单元测试，覆盖率 ≥ 80%**
- **任务串行执行，上任务测试通过后才执行下一个**
- 所有 PR 需要代码审查

## Git 工作流

本项目使用 Git Flow 分支模型：

| 分支 | 说明 | 保护 |
|------|------|------|
| `main` | 生产分支，随时可部署 | ✅ 受保护 |
| `develop` | 开发分支，集成功能 | ✅ 受保护 |
| `feature/*` | 功能分支，开发新功能 | ❌ 不保护 |

**开发流程**:
1. 从 `develop` 创建 `feature/*` 分支
2. 在功能分支上开发并提交
3. 完成后创建 PR 合并到 `develop`
4. `develop` 测试验证通过后，询问是否合并到 `main`

详见：[Git 工作流规范](docs/manual/git-workflow.md)

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 文档中心

完整文档请访问：[docs/](docs/)

| 文档类型 | 路径 | 说明 |
|----------|------|------|
| 📚 API 文档 | [docs/api/](docs/api/) | 后端/前端 API 接口文档 |
| 📋 用例文档 | [docs/use-cases/](docs/use-cases/) | 功能用例说明 |
| 🧪 测试报告 | [docs/test-reports/](docs/test-reports/) | 单元测试/集成测试报告 |
| 🏗️ 架构文档 | [docs/architecture/](docs/architecture/) | 系统架构/技术栈说明 |
| 📖 开发者指南 | [docs/manual/](docs/manual/) | 开发规范/部署指南 |
| 📊 监控文档 | [docs/monitoring/](docs/monitoring/) | 监控系统部署指南 |

## 联系方式

- 项目仓库：https://github.com/buhang1985/his_agent
- 问题反馈：https://github.com/buhang1985/his_agent/issues
