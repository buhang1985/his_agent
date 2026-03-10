# 开发者指南

## 开发环境搭建

### 必需工具

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | 后端开发 |
| Node.js | 20+ | 前端开发 |
| Maven | 3.9+ | 后端构建 |
| Git | 2.40+ | 版本控制 |
| MySQL | 8+ | 数据库 |

### 可选工具

| 工具 | 用途 |
|------|------|
| IntelliJ IDEA | Java IDE |
| VS Code | 前端编辑器 |
| Postman | API 测试 |
| DBeaver | 数据库管理 |

## 代码规范

### 后端 (Java)

遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

**命名规范**:
```java
// 类名：大驼峰
public class ConsultationService { }

// 方法名：小驼峰
public void createConsultation() { }

// 常量：大写 + 下划线
public static final int MAX_RETRY = 3;

// 变量：小驼峰
String patientId = "P001";
```

**注释规范**:
```java
/**
 * 创建新的问诊会话
 * 
 * @param patientId 患者 ID
 * @return 问诊会话信息
 * @throws BusinessException 当创建失败时
 */
public Consultation createConsultation(String patientId) {
    // 方法实现
}
```

### 前端 (TypeScript/Vue)

**命名规范**:
```typescript
// 组件名：大驼峰
const VoiceInput = defineComponent({ });

// 函数名：小驼峰
function createConsultation() { }

// 类型名：大驼峰
interface ConsultationData { }

// 常量：小驼峰
const apiBaseUrl = '/api';
```

**TSDoc 规范**:
```typescript
/**
 * 创建问诊会话
 * @param patientId - 患者 ID
 * @returns 问诊会话响应
 * @throws Error 当 API 调用失败时
 */
async function createConsultation(
  patientId: string
): Promise<ConsultationResponse> {
  // 实现
}
```

## 文档规范

### 接口文档

每个新接口必须创建文档：

**文件位置**: `docs/api/backend/<feature>-api.md`

**模板**:
```markdown
# <功能名称> API

## 接口说明

简要描述接口功能。

## 请求

- **方法**: POST
- **路径**: `/api/v1/<resource>`
- **Content-Type**: `application/json`

### 请求参数

```json
{
  "param1": "value1",
  "param2": "value2"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| param1 | string | 是 | 说明 |
| param2 | string | 否 | 说明 |

### 请求头

| 头 | 值 | 说明 |
|----|----|------|
| Authorization | Bearer <token> | 认证 token |

## 响应

### 成功响应

```json
{
  "code": 200,
  "data": { },
  "message": "success"
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "错误描述",
  "errors": [ ]
}
```

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |

## 调用示例

```bash
curl -X POST http://localhost:8080/api/v1/<resource> \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"param1":"value1"}'
```
```

### 用例文档

每个新功能必须创建用例文档：

**文件位置**: `docs/use-cases/UC-XXX-<功能名称>.md`

使用 [用例模板](../use-cases/README.md)。

### 测试文档

测试完成后必须生成测试报告：

**文件位置**: `docs/test-reports/<backend|frontend>/<type>/<date>-report.md`

使用 [测试报告模板](../test-reports/README.md)。

## Git 工作流

### 分支策略

```
main (受保护)
  │
  ├── develop (开发分支)
  │     │
  │     ├── feature/voice-consultation
  │     ├── feature/diagnosis-suggestion
  │     └── fix/login-bug
  │
  └── release/v1.0.0 (发布分支)
```

### 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试
chore: 构建/工具
```

**示例**:
```bash
git commit -m "feat(consultation): 添加问诊创建接口"
git commit -m "fix(auth): 修复 token 验证问题"
git commit -m "docs(api): 更新问诊 API 文档"
git commit -m "test(service): 添加服务层单元测试"
```

## 测试流程

### 后端测试

```bash
# 1. 运行单元测试
mvn clean test

# 2. 检查覆盖率
mvn jacoco:check

# 3. 生成报告
mvn jacoco:report

# 4. 复制报告到 docs
cp -r target/jacoco-report docs/test-reports/backend/unit-tests/$(date +%Y-%m-%d)

# 5. 创建测试报告文档
# 编辑 docs/test-reports/backend/unit-tests/<date>-report.md
```

### 前端测试

```bash
# 1. 运行单元测试
npm run test -- --run

# 2. 生成覆盖率
npm run test:coverage -- --run

# 3. 复制报告
cp -r coverage docs/test-reports/frontend/unit-tests/$(date +%Y-%m-%d)

# 4. 创建测试报告文档
```

## 文档更新检查清单

在提交代码前，确认：

- [ ] 新增接口已更新 API 文档
- [ ] 新功能已创建用例文档
- [ ] 测试已完成并生成报告
- [ ] README 已更新 (如有重大变更)
- [ ] 变更日志已更新

## 发布流程

### 版本号规范

遵循 [Semantic Versioning](https://semver.org/)：

```
主版本号。次版本号.修订号
  ↑      ↑      ↑
  │      │      └─ 向后兼容的问题修复
  │      └─ 向后兼容的新功能
  └─ 不兼容的 API 变更
```

### 发布步骤

1. 更新版本号 (pom.xml, package.json)
2. 更新 CHANGELOG.md
3. 运行完整测试
4. 生成测试报告
5. 创建 Git Tag
6. 发布说明

## 常见问题

### Q: 如何调试后端？

```bash
# 远程调试模式启动
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Q: 如何跳过测试？

```bash
# 后端
mvn clean install -DskipTests

# 前端
npm run build -- --no-test
```

### Q: 如何查看 API 文档？

启动后端后访问：
```
http://localhost:8080/swagger-ui.html
```

## 参考资料

- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Vue 3 文档](https://vuejs.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
