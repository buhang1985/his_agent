# 数据库脚本

本目录存放 his_agent 的所有数据库操作脚本。

## 目录结构

```
database/
├── README.md                    # 本文档
├── ddl/                         # DDL 脚本 (数据定义)
│   ├── 001-create-consultations.sql
│   ├── 002-create-soap-notes.sql
│   └── 003-create-diagnosis-suggestions.sql
├── dml/                         # DML 脚本 (数据操作)
│   ├── 001-insert-sample-data.sql
│   └── 002-insert-medical-terms.sql
└── migrations/                  # 迁移脚本
    ├── V1__initial_schema.sql
    ├── V2__add-indexes.sql
    └── V3__add-ai-features.sql
```

## DDL 脚本 (数据定义语言)

DDL 脚本用于创建、修改、删除数据库对象。

### 命名规范

```
<序号>-<操作>-<表名>.sql
```

**示例**:
- `001-create-consultations.sql` - 创建问诊表
- `002-alter-users-add-column.sql` - 用户表添加字段
- `003-drop-temp-table.sql` - 删除临时表

### 脚本内容

每个 DDL 脚本应包含：
1. 脚本说明 (用途、作者、日期)
2. 变更描述
3. DDL 语句
4. 回滚语句 (可选)

**模板**:
```sql
-- ============================================================
-- 脚本：001-create-consultations.sql
-- 用途：创建问诊会话表
-- 作者：姓名
-- 日期：2026-03-10
-- ============================================================

-- 变更描述
-- - 创建 consultations 表
-- - 添加主键、外键约束
-- - 创建索引

-- DDL 语句
CREATE TABLE IF NOT EXISTS consultations (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patient_id (patient_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊会话表';

-- 回滚语句 (可选)
-- DROP TABLE IF EXISTS consultations;
```

---

## DML 脚本 (数据操作语言)

DML 脚本用于插入、更新、删除数据。

### 命名规范

```
<序号>-<操作>-<数据描述>.sql
```

**示例**:
- `001-insert-sample-data.sql` - 插入示例数据
- `002-update-medical-terms.sql` - 更新医学词库
- `003-delete-temp-data.sql` - 删除临时数据

### 脚本内容

每个 DML 脚本应包含：
1. 脚本说明
2. 变更描述
3. DML 语句
4. 验证查询 (可选)

**模板**:
```sql
-- ============================================================
-- 脚本：001-insert-sample-data.sql
-- 用途：插入示例患者数据
-- 作者：姓名
-- 日期：2026-03-10
-- ============================================================

-- 变更描述
-- - 插入 3 个示例患者
-- - 插入 2 个示例问诊记录

-- DML 语句
INSERT INTO patients (id, name, gender, age, medical_record_number)
VALUES 
    ('P001', '张三', 'male', 45, 'MRN001'),
    ('P002', '李四', 'female', 32, 'MRN002'),
    ('P003', '王五', 'male', 58, 'MRN003');

-- 验证查询
SELECT COUNT(*) FROM patients;
-- 预期结果：3
```

---

## 迁移脚本 (Flyway)

项目使用 Flyway 进行数据库版本管理。

### 命名规范

```
V<主版本>__<描述>.sql
R<描述>.sql  (可重复运行)
```

**示例**:
- `V1__initial_schema.sql` - 初始 schema
- `V2__add_indexes.sql` - 添加索引
- `R__medical_terms.sql` - 医学词库 (可重复运行)

### 迁移历史

Flyway 自动跟踪迁移历史：

```sql
-- Flyway 历史表
CREATE TABLE flyway_schema_history (
    installed_rank INT PRIMARY KEY,
    version VARCHAR(50),
    description VARCHAR(200),
    type VARCHAR(20),
    script VARCHAR(1000),
    checksum INT,
    installed_by VARCHAR(100),
    installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_time INT,
    success BOOLEAN
);
```

---

## 数据库初始化

### 开发环境

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE his_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 运行 Flyway 迁移
cd his_agent-backend
mvn flyway:migrate

# 3. 插入示例数据 (可选)
mysql -u root -p his_agent < docs/database/dml/001-insert-sample-data.sql
```

### 生产环境

```bash
# 1. 备份现有数据
mysqldump -u root -p his_agent > backup_$(date +%Y%m%d).sql

# 2. 运行 Flyway 迁移
mvn flyway:migrate -Dflyway.url=jdbc:mysql://prod-db:3306/his_agent

# 3. 验证迁移
mvn flyway:info
```

---

## 数据库文档

### ER 图

```
┌─────────────────────────────────────────┐
│           his_agent 数据库 ER 图          │
└─────────────────────────────────────────┘

┌──────────────┐       ┌──────────────┐
│ consultations│       │   patients   │
├──────────────┤       ├──────────────┤
│ id (PK)      │       │ id (PK)      │
│ patient_id   │──────▶│ name         │
│ status       │       │ gender       │
│ created_at   │       │ age          │
└──────────────┘       └──────────────┘
       │
       │ 1:1
       ▼
┌──────────────┐       ┌──────────────┐
│  soap_notes  │       │  diagnosis   │
├──────────────┤       ├──────────────┤
│ id (PK)      │       │ id (PK)      │
│ consultation │       │ consultation │
│ subjective   │       │ condition    │
│ objective    │       │ likelihood   │
│ assessment   │       │ icd10_code   │
│ plan         │       └──────────────┘
└──────────────┘
```

### 表结构说明

详见各 DDL 脚本中的 COMMENT 注释。

---

## 版本管理

| 版本 | 日期 | 说明 | 脚本 |
|------|------|------|------|
| 1.0 | 2026-03-10 | 初始 schema | V1__initial_schema.sql |
| 1.1 | - | 添加索引 | V2__add_indexes.sql |

---

## 参考资料

- [Flyway 文档](https://flywaydb.org/documentation/)
- [MySQL 文档](https://dev.mysql.com/doc/)
- [SQL 风格指南](https://github.com/treffynnon/sql-style-guide)
