# Git 工作流规范

## 分支策略

```
main (受保护分支)
  │
  ├── develop (开发分支)
  │     │
  │     ├── feature/voice-consultation
  │     ├── feature/diagnosis-suggestion
  │     └── fix/login-bug
  │
  └── release/v1.0.0 (发布分支)
```

### 分支说明

| 分支 | 说明 | 保护 | 合并规则 |
|------|------|------|----------|
| `main` | 生产分支，随时可部署 | ✅ 受保护 | 只能从 develop 合并 |
| `develop` | 开发分支，集成功能 | ✅ 受保护 | 只能从 feature 分支合并 |
| `feature/*` | 功能分支，开发新功能 | ❌ 不保护 | 必须合并回 develop |
| `release/*` | 发布分支，准备新版本 | ✅ 受保护 | 测试后合并到 main 和 develop |
| `hotfix/*` | 热修复分支，紧急修复 | ❌ 不保护 | 合并到 main 和 develop |

---

## 开发流程

### 1. 开始新功能

```bash
# 1. 切换到 develop 分支
git checkout develop

# 2. 拉取最新代码
git pull origin develop

# 3. 创建功能分支
git checkout -b feature/voice-consultation

# 4. 推送功能分支
git push -u origin feature/voice-consultation
```

### 2. 开发过程中

```bash
# 频繁提交，遵循 Conventional Commits
git add .
git commit -m "feat(consultation): 添加问诊创建接口"

# 定期同步 develop 分支
git fetch origin
git rebase origin/develop
```

### 3. 完成功能开发

```bash
# 1. 确保所有测试通过
mvn clean test
npm run test

# 2. 确保文档已更新
# - API 文档
# - 用例文档
# - 测试报告

# 3. 推送到远程
git push origin feature/voice-consultation

# 4. 创建 Pull Request 到 develop
# GitHub: https://github.com/buhang1985/his_agent/pulls
```

### 4. 代码审查

- 至少 1 人审查
- 所有 CI 检查通过
- 解决所有评论

### 5. 合并到 develop

```bash
# 审查通过后，合并到 develop
# 使用 Squash Merge 或 Rebase Merge

# 本地更新 develop
git checkout develop
git pull origin develop
```

---

## 发布流程

### 1. 创建发布分支

```bash
# 从 develop 创建发布分支
git checkout -b release/v1.0.0 develop

# 推送发布分支
git push -u origin release/v1.0.0
```

### 2. 发布测试

- 在 release 分支上进行最终测试
- 修复发现的问题 (直接提交到 release 分支)
- 更新版本号 (pom.xml, package.json)
- 更新 CHANGELOG.md

### 3. 合并到 main

**测试验证通过后，询问是否合并到 main 分支**:

```bash
# 合并到 main
git checkout main
git pull origin main
git merge --no-ff release/v1.0.0
git push origin main

# 创建 Git Tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### 4. 合并回 develop

```bash
# 合并 release 分支回 develop
git checkout develop
git merge --no-ff release/v1.0.0
git push origin develop

# 删除 release 分支
git branch -d release/v1.0.0
git push origin --delete release/v1.0.0
```

---

## 热修复流程

### 1. 创建热修复分支

```bash
# 从 main 创建热修复分支
git checkout -b hotfix/login-bug main
```

### 2. 修复并测试

```bash
# 修复问题
git add .
git commit -m "fix(auth): 修复 token 验证问题"

# 运行测试
mvn clean test
```

### 3. 合并到 main 和 develop

```bash
# 合并到 main
git checkout main
git merge --no-ff hotfix/login-bug
git push origin main

# 合并到 develop
git checkout develop
git merge --no-ff hotfix/login-bug
git push origin develop

# 删除热修复分支
git branch -d hotfix/login-bug
```

---

## 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

### 类型

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `docs` | 文档更新 |
| `style` | 代码格式 (不影响代码运行) |
| `refactor` | 重构 (既不是新功能也不是 bug 修复) |
| `test` | 测试相关 |
| `chore` | 构建/工具/配置 |

### 作用域

| 作用域 | 说明 |
|--------|------|
| `consultation` | 问诊相关 |
| `diagnosis` | 诊断相关 |
| `speech` | 语音识别 |
| `auth` | 认证授权 |
| `his` | HIS 集成 |
| `database` | 数据库相关 |
| `frontend` | 前端相关 |
| `backend` | 后端相关 |

### 示例

```bash
# 新功能
git commit -m "feat(consultation): 添加问诊创建接口"

# 修复 bug
git commit -m "fix(auth): 修复 token 验证问题"

# 文档更新
git commit -m "docs(api): 更新问诊 API 文档"

# 测试
git commit -m "test(service): 添加服务层单元测试"

# 数据库
git commit -m "feat(database): 创建问诊会话表"
```

---

## 分支保护规则

### main 分支

- ✅ 需要 Pull Request
- ✅ 至少 1 人审查
- ✅ CI 检查必须通过
- ✅ 禁止强制推送
- ✅ 需要签署提交 (可选)

### develop 分支

- ✅ 需要 Pull Request
- ✅ 至少 1 人审查
- ✅ CI 检查必须通过
- ✅ 禁止强制推送

---

## 检查清单

### 合并到 develop 前

- [ ] 所有单元测试通过
- [ ] 测试覆盖率 ≥ 80%
- [ ] 代码已格式化
- [ ] API 文档已更新
- [ ] 用例文档已更新
- [ ] 测试报告已生成

### 合并到 main 前 (发布)

- [ ] develop 分支所有测试通过
- [ ] 发布测试通过
- [ ] 版本号已更新
- [ ] CHANGELOG.md 已更新
- [ ] 数据库脚本已评审
- [ ] 性能测试通过 (如有)

---

## 常见问题

### Q: 如何撤销已提交的更改？

```bash
# 撤销工作区更改
git checkout -- <file>

# 撤销暂存区更改
git reset HEAD <file>

# 撤销最后一次提交
git reset --soft HEAD~1

# 撤销提交并丢弃更改
git reset --hard HEAD~1
```

### Q: 如何合并多个提交？

```bash
# 交互式 rebase
git rebase -i HEAD~3

# 在编辑器中选择:
# pick 第 1 个提交
# squash 第 2 个提交 (合并到上一个)
# squash 第 3 个提交 (合并到上一个)
```

### Q: 如何处理冲突？

```bash
# 1. 拉取最新代码
git pull origin develop

# 2. 解决冲突文件中的冲突标记

# 3. 标记冲突已解决
git add <file>

# 4. 继续 rebase 或 merge
git rebase --continue
# 或
git commit -m "fix: resolve merge conflicts"
```

---

## 参考资料

- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
