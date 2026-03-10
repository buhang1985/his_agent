# his_agent 文档中心

本文档中心分类存放项目的所有技术文档。

## 文档结构

```
docs/
├── README.md                    # 本文档索引
├── api/                         # API 接口文档
│   ├── README.md               # API 文档说明
│   ├── backend/                # 后端接口文档
│   │   ├── overview.md         # 接口概览
│   │   ├── consultation-api.md # 问诊接口
│   │   ├── speech-api.md       # 语音接口
│   │   └── his-api.md          # HIS 集成接口
│   └── frontend/               # 前端 API 调用文档
│       ├── services.md         # 服务层说明
│       └── api-client.md       # API 客户端使用
│
├── use-cases/                   # 用例文档
│   ├── README.md               # 用例文档说明
│   ├── UC-001-智能问诊.md      # 智能问诊用例
│   ├── UC-002-诊断建议.md      # 诊断建议用例
│   └── UC-003-病历生成.md      # 病历生成用例
│
├── test-reports/                # 测试结果文档
│   ├── README.md               # 测试报告说明
│   ├── backend/                # 后端测试报告
│   │   ├── unit-tests/         # 单元测试报告
│   │   └── integration-tests/  # 集成测试报告
│   └── frontend/               # 前端测试报告
│       └── unit-tests/         # 单元测试报告
│
├── database/                    # 数据库脚本
│   ├── README.md               # 数据库脚本说明
│   ├── ddl/                    # DDL 脚本 (数据定义)
│   │   ├── 001-create-consultations.sql
│   │   ├── 002-create-soap-notes.sql
│   │   └── 003-create-diagnosis-suggestions.sql
│   ├── dml/                    # DML 脚本 (数据操作)
│   │   ├── 001-insert-sample-data.sql
│   │   └── 002-insert-medical-terms.sql
│   └── migrations/              # 迁移脚本 (Flyway)
│       └── V1__initial_schema.sql
│
├── architecture/                # 架构文档
│   ├── system-architecture.md  # 系统架构
│   ├── tech-stack.md           # 技术栈说明
│   └── deployment.md           # 部署架构
│
└── manual/                      # 操作手册
    ├── developer-guide.md      # 开发者指南
    ├── deployment-guide.md     # 部署指南
    ├── git-workflow.md         # Git 工作流
    └── security-guide.md       # 安全规范指南
```

## 文档规范

### API 接口文档

每个接口必须包含：
- 接口描述
- 请求方法 (GET/POST/PUT/DELETE)
- 请求路径
- 请求参数 (Header/Query/Body)
- 响应格式
- 错误码说明
- 调用示例

### 用例文档

每个用例必须包含：
- 用例编号 (UC-XXX)
- 用例名称
- 参与者
- 前置条件
- 后置条件
- 基本流程
- 备选流程
- 异常流程

### 测试结果文档

每个测试报告必须包含：
- 测试概述
- 测试范围
- 测试环境
- 测试结果汇总
- 通过率统计
- 覆盖率统计
- 缺陷列表
- 测试结论

## 文档更新流程

1. **新增功能** → 同步更新 API 文档和用例文档
2. **修改接口** → 更新 API 文档，标注版本号
3. **测试完成** → 生成测试结果文档
4. **架构变更** → 更新架构文档

## 文档生成

### 后端 API 文档

启动后端后自动访问 Swagger UI：
```
http://localhost:8080/swagger-ui.html
```

导出 OpenAPI 规范：
```bash
curl http://localhost:8080/v3/api-docs -o docs/api/backend/openapi.json
```

### 前端 API 调用文档

使用 TypeDoc 生成：
```bash
cd his_agent-frontend
npx typedoc src/services --out docs/api/frontend
```

### 测试报告

**后端**:
```bash
cd his_agent-backend
mvn clean test
mvn jacoco:report
# 报告位置：target/jacoco-report/index.html
# 复制报告到 docs/test-reports/backend/
```

**前端**:
```bash
cd his_agent-frontend
npm run test:coverage
# 报告位置：coverage/index.html
# 复制报告到 docs/test-reports/frontend/
```

## 文档版本

| 文档类型 | 版本管理 |
|----------|----------|
| API 文档 | 随 API 版本更新 |
| 用例文档 | 随功能版本更新 |
| 测试报告 | 每次测试生成新报告 |
| 架构文档 | 重大变更时更新 |
| 数据库脚本 | 随 schema 变更更新 (Flyway 版本化) |
| 安全规范 | 随安全策略更新 |

## 参考资料

- [OpenAPI 规范](https://swagger.io/specification/)
- [用例建模指南](https://www.sparxsystems.com/resources/user-guides/15.0/modeling/requirements-use-case-modeling.html)
- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Vitest 文档](https://vitest.dev/)
