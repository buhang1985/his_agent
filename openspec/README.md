# OpenSpec 使用指南

本项目使用 OpenSpec 进行 **spec-driven development**（规格驱动开发）。

## 命令列表

| 命令 | 描述 | 使用场景 |
|------|------|----------|
| `/opsx-explore` | 进入探索模式 | 构思想法、调查问题、澄清需求 |
| `/opsx-propose` | 提案新变更 | 一步创建提案、设计、任务 |
| `/opsx-apply` | 实现变更任务 | 执行 OpenSpec 变更中的任务 |
| `/opsx-archive` | 归档已完成变更 | 将实验性变更归档 |

## 开发流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   探索      │ ──▶ │   提案      │ ──▶ │   实施      │ ──▶ │   归档      │
│  (Explore)  │     │  (Propose)  │     │   (Apply)   │     │  (Archive)  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

### 1. 探索需求

```bash
/opsx-explore <topic>
```

**用途**: 在进入实施前，先充分讨论和明确需求。

**示例**:
```bash
/opsx-explore 智能问诊功能
/opsx-explore HIS 系统集成方案
/opsx-explore 语音识别技术选型
```

**产出**: 
- 需求澄清
- 技术方案对比
- 风险评估

### 2. 创建变更提案

```bash
/opsx-propose <feature-name>
```

**用途**: 基于探索结果，创建完整的变更提案。

**示例**:
```bash
/opsx-propose voice-consultation
/opsx-propose his-integration
/opsx-propose speech-recognition
```

**产出**:
- `openspec/changes/<name>/proposal.md` - 变更提案
- `openspec/changes/<name>/design.md` - 技术设计
- `openspec/changes/<name>/tasks.md` - 任务分解

### 3. 实施任务

```bash
/opsx-apply <feature-name>
```

**用途**: 根据任务分解，逐步实施功能。

**示例**:
```bash
/opsx-apply voice-consultation
```

**流程**:
1. 读取 `tasks.md` 中的任务列表
2. 逐个完成任务
3. 每个任务完成后更新状态
4. 所有任务完成后进入归档

### 4. 归档变更

```bash
/opsx-archive <feature-name>
```

**用途**: 将完成的变更归档，合并到主分支。

**示例**:
```bash
/opsx-archive voice-consultation
```

**产出**:
- 更新 `openspec/changes/<name>/archive.md`
- 标记变更状态为 `completed`

## 文件结构

```
openspec/
├── config.yaml                    # OpenSpec 配置
├── README.md                      # 本使用指南
├── specs/                         # 功能规格说明
│   ├── tech-stack.md             # 技术栈规格
│   └── 01-voice-consultation.md  # 智能问诊规格
└── changes/                       # 变更工件
    └── voice-consultation/
        ├── proposal.md           # 变更提案
        ├── design.md             # 技术设计
        ├── tasks.md              # 任务分解
        └── plan.md               # 实施计划
```

## 规格说明文档

### 现有规格

| 文档 | 描述 | 状态 |
|------|------|------|
| [tech-stack.md](specs/tech-stack.md) | 技术栈规格说明 | ✅ 已批准 |
| [01-voice-consultation.md](specs/01-voice-consultation.md) | 智能问诊功能规格 | ✅ 已批准 |

### 创建新规格

新规格的命名规范：`<序号>-<feature-name>.md`

示例：
```
specs/02-diagnosis-suggestion.md
specs/03-medical-knowledge-query.md
```

## 变更状态

| 状态 | 描述 |
|------|------|
| `proposed` | 已提案，待审核 |
| `approved` | 已批准，可实施 |
| `in_progress` | 实施中 |
| `completed` | 已完成，待归档 |
| `archived` | 已归档 |

## 开发约定

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

### 任务分解

每个任务应满足：
- **独立性**: 可独立完成，不依赖其他任务
- **可测试**: 有明确的验收标准
- **小粒度**: 最多 4 小时工作量

### 代码审查

- 所有 PR 需要代码审查
- 医疗相关功能需要安全审查
- 确保测试覆盖

## 最佳实践

### 1. 先探索，后实施

不要急于写代码。先用 `/opsx-explore` 充分讨论：
- 需求是否明确？
- 技术方案是否合理？
- 有哪些风险？

### 2. 规格先行

在实施前，确保规格文档已完成并批准：
- 功能边界清晰
- 接口定义明确
- 验收标准具体

### 3. 小步快跑

任务分解要小，频繁提交：
- 每个任务独立可测试
- 完成一个任务就提交一次
- 避免大兵团作战

### 4. 文档同步更新

代码变更的同时更新文档：
- API 变更更新 Swagger
- 配置变更更新 README
- 架构变更更新设计文档

## 常见问题

### Q: 如何查看当前有哪些变更？

```bash
openspec-cn list --json
```

### Q: 如何查看变更详情？

```bash
cat openspec/changes/<name>/proposal.md
```

### Q: 任务实施中途卡住了怎么办？

1. 使用 `/opsx-explore` 重新讨论问题
2. 更新 `design.md` 反映新的理解
3. 调整 `tasks.md` 任务分解

### Q: 如何回滚已实施的变更？

1. 使用 git 回滚到变更前的提交
2. 更新变更状态为 `cancelled`
3. 记录回滚原因

## 参考资料

- [OpenSpec 官方文档](https://github.com/openspec-project/openspec)
- [Spec-Driven Development](https://spec-driven.dev/)
