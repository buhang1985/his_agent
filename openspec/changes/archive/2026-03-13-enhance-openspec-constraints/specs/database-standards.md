# 数据库规范细化

**版本**: 1.0  
**日期**: 2026-03-12  
**状态**: 新增

---

## 新增需求

### 需求：表命名规范

数据库表命名必须遵循统一规范，便于维护和自动化处理。

#### 场景：表名格式
- **当** 创建新表时
- **那么** 必须：
  - 使用小写字母 + 下划线
  - 使用复数形式（`users`, `consultations`）
  - 表名长度 ≤ 64 字符（MySQL 限制）
  - 禁止使用 MySQL 保留字

#### 场景：表名示例
- **当** 命名业务表时
- **那么** 必须：
```sql
-- ✅ 正确
CREATE TABLE consultations (...);
CREATE TABLE soap_notes (...);
CREATE TABLE diagnosis_suggestions (...);

-- ❌ 错误（单数）
CREATE TABLE consultation (...);

-- ❌ 错误（大写）
CREATE TABLE Consultations (...);

-- ❌ 错误（保留字）
CREATE TABLE order (...);  -- ORDER 是保留字
```

#### 场景：关联表命名
- **当** 创建多对多关联表时
- **那么** 必须：
```sql
-- 格式：表 1_表 2（按字母顺序）
CREATE TABLE user_roles (...);  -- user + roles
CREATE TABLE role_permissions (...);  -- role + permissions
```

---

### 需求：字段命名规范

字段命名必须遵循统一规范，保持可读性和一致性。

#### 场景：字段名格式
- **当** 创建字段时
- **那么** 必须：
  - 使用小写字母 + 下划线
  - 使用有意义的英文名称
  - 字段名长度 ≤ 64 字符
  - 禁止使用拼音、缩写（除非通用）

#### 场景：标准字段
- **当** 定义通用字段时
- **那么** 必须：
```sql
-- 主键
id VARCHAR(36) PRIMARY KEY  -- 或 INT AUTO_INCREMENT

-- 时间戳
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）'

-- 外键
user_id VARCHAR(36) NOT NULL COMMENT '用户 ID',
consultation_id VARCHAR(36) COMMENT '问诊会话 ID'

-- 状态
status ENUM('active', 'inactive') NOT NULL DEFAULT 'active' COMMENT '状态'
```

#### 场景：字段类型选择
- **当** 选择字段类型时
- **那么** 必须：
```sql
-- 字符串
VARCHAR(50)     -- 短字符串（姓名、邮箱）
VARCHAR(200)    -- 中等字符串（地址、URL）
TEXT            -- 长文本（描述、内容）

-- 数字
TINYINT         -- 状态标志（0/1）
INT             -- 计数、ID
BIGINT          -- 大数（订单号）
DECIMAL(10,2)   -- 金额

-- 时间
DATETIME        -- 业务时间
TIMESTAMP       -- 系统时间（自动时区转换）

-- JSON
JSON            -- 结构化数据（配置、扩展字段）
```

#### 场景：字段注释
- **当** 创建字段时
- **那么** 必须添加 COMMENT：
```sql
-- ✅ 正确
patient_name VARCHAR(100) COMMENT '患者姓名',
patient_gender ENUM('male', 'female', 'other') COMMENT '患者性别',
chief_complaint JSON COMMENT '主诉（结构化数据）'

-- ❌ 错误（无注释）
patient_name VARCHAR(100),
```

---

### 需求：索引设计规范

索引必须合理规划，平衡查询性能和写入性能。

#### 场景：必须创建索引的字段
- **当** 字段满足以下条件时
- **那么** 必须创建索引：
  - 主键（自动创建）
  - 外键字段
  - WHERE 子句常用字段
  - ORDER BY 字段
  - GROUP BY 字段
  - JOIN 连接字段

#### 场景：索引命名规范
- **当** 创建索引时
- **那么** 必须：
```sql
-- 主键索引
PRIMARY KEY (id)

-- 唯一索引
UNIQUE KEY uk_username (username)
UNIQUE KEY uk_patient_id_card (patient_id_card)

-- 普通索引
KEY idx_patient_id (patient_id)
KEY idx_status_created (status, created_at)  -- 复合索引

-- 全文索引（MySQL 5.6+）
FULLTEXT INDEX ft_content (content)
```

#### 场景：复合索引设计
- **当** 创建复合索引时
- **那么** 必须：
  - 最左前缀原则（查询条件从最左列开始）
  - 高频查询字段放在前面
  - 区分度高的字段放在前面
```sql
-- ✅ 正确：支持 (status), (status, created_at)
KEY idx_status_created (status, created_at)

-- ❌ 错误：顺序不合理
KEY idx_created_status (created_at, status)  -- status 区分度更高
```

#### 场景：索引数量限制
- **当** 设计表索引时
- **那么** 必须：
  - 单表索引数 ≤ 5 个
  - 复合索引列数 ≤ 3 列
  - 避免重复索引
  - 定期分析未使用索引

---

### 需求：慢查询监控阈值

系统必须实现慢查询监控，及时发现性能问题。

#### 场景：MySQL 慢查询日志
- **当** 配置 MySQL 时
- **那么** 必须：
```ini
# /etc/mysql/my.cnf
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/mysql-slow.log
long_query_time = 2  # 超过 2 秒的查询记录
log_queries_not_using_indexes = 1  # 记录未使用索引的查询
min_examined_row_limit = 1000  # 仅记录扫描 1000 行以上的查询
```

#### 场景：应用层慢查询监控
- **当** 配置 Spring Boot 时
- **那么** 必须：
```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        # 记录慢 SQL（毫秒）
        session_events_log_level: INFO
        # 慢查询阈值
        query_plan_cache_max_entries: 2048

# 自定义配置
his_agent:
  database:
    slow-query-threshold: 2000  # 2 秒
    log-slow-queries: true
```

#### 场景：慢查询告警阈值
- **当** 监控慢查询时
- **那么** 必须：
```yaml
# 告警规则
alerts:
  slow_query:
    # P0 告警：单条查询超过 10 秒
    - threshold: 10s
      severity: P0
      action: 立即通知 DBA
    
    # P1 告警：单条查询超过 5 秒
    - threshold: 5s
      severity: P1
      action: 发送告警
    
    # P2 告警：慢查询比例超过 5%
    - threshold: 5%
      window: 5m
      severity: P2
      action: 记录日志
```

#### 场景：慢查询分析
- **当** 发现慢查询时
- **那么** 必须：
  - 使用 EXPLAIN 分析执行计划
  - 检查是否使用索引
  - 检查扫描行数
  - 检查临时表使用
  - 检查文件排序

---

### 需求：数据库读写分离策略

明确数据库读写分离策略，避免复杂性。

#### 场景：初期架构（单体）
- **当** 项目初期时
- **那么** 必须：
  - 使用单 MySQL 实例
  - 读写共用连接池
  - 通过 HikariCP 管理连接
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

#### 场景：中期架构（读写分离）
- **当** 读请求超过写请求 10 倍时
- **那么** 可以考虑：
  - 主从复制（一主一从）
  - 读写分离
  - 写操作走主库，读操作走从库
```java
// 路由数据源
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        // 写操作或事务中走主库
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return "slave";
        }
        return "master";
    }
}
```

#### 场景：明确不支持
- **当** 项目初期（1 年内）
- **那么** 明确不支持：
  - ❌ 分库分表
  - ❌ 多主架构
  - ❌ 分布式事务
  - ❌ 数据库集群

---

### 需求：Flyway 迁移文件命名规范

数据库迁移文件必须遵循统一命名规范。

#### 场景：版本迁移文件
- **当** 创建版本迁移时
- **那么** 必须：
```bash
# 格式：V<版本>__<描述>.sql
V1__initial_schema.sql              # 初始 schema
V2__add_user_table.sql              # 添加用户表
V3__add_index_patient_id.sql        # 添加索引
V4__alter_column_patient_name.sql   # 修改字段

# 版本规则
# - 版本号必须递增（1, 2, 3...）
# - 使用双下划线分隔版本和描述
# - 描述使用小写 + 下划线
```

#### 场景：重复迁移文件（可重复执行）
- **当** 创建可重复执行的迁移时
- **那么** 必须：
```bash
# 格式：R__<描述>.sql
R__insert_initial_data.sql          # 插入初始数据
R__update_config_values.sql         # 更新配置值
R__create_views.sql                 # 创建视图

# 使用场景
# - 插入/更新基础数据
# - 创建/修改视图、存储过程
# - 幂等操作
```

#### 场景：回滚脚本（手动）
- **当** 需要回滚迁移时
- **那么** 必须：
```bash
# 格式：R__rollback_<描述>.sql（手动执行）
R__rollback_v3_add_index.sql        # 回滚 V3 迁移

# 注意：Flyway 社区版不支持自动回滚
# 回滚脚本必须手动执行，并记录在案
```

#### 场景：迁移文件内容
- **当** 编写迁移脚本时
- **那么** 必须：
```sql
-- ============================================================
-- Flyway 迁移：V2__add_user_table.sql
-- 版本：2.0
-- 描述：添加用户表
-- 日期：2026-03-12
-- 作者：developer
-- ============================================================

-- 前滚操作
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 回滚操作（注释）
-- DROP TABLE IF EXISTS users;
```

#### 场景：迁移验证
- **当** 应用启动时
- **那么** 必须：
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true  # 验证迁移文件完整性
    clean-disabled: true  # 生产环境禁止 clean
```

---

### 需求：连接池配置规范

必须合理配置 HikariCP 连接池，平衡性能和资源。

#### 场景：开发环境配置
- **当** 配置开发环境时
- **那么** 必须：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000  # 连接泄漏检测
```

#### 场景：生产环境配置
- **当** 配置生产环境时
- **那么** 必须：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 根据服务器配置调整
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 0  # 生产环境关闭（性能影响）
      connection-init-sql: SELECT 1  # 连接测试
```

#### 场景：连接池监控
- **当** 监控连接池时
- **那么** 必须暴露指标：
  - 活跃连接数
  - 空闲连接数
  - 等待连接数
  - 连接获取时间
  - 连接泄漏次数

---

## 验收标准

### 表命名
- [ ] 所有表使用复数形式
- [ ] 所有表使用小写 + 下划线
- [ ] 无保留字冲突

### 字段命名
- [ ] 所有字段使用小写 + 下划线
- [ ] 所有字段有 COMMENT 注释
- [ ] 时间戳字段统一（created_at, updated_at）
- [ ] 主键统一（id）
- [ ] 外键命名规范（xxx_id）

### 索引设计
- [ ] 主键、外键有索引
- [ ] 查询频繁字段有索引
- [ ] 复合索引遵循最左前缀原则
- [ ] 索引命名规范（idx_, uk_）
- [ ] 单表索引 ≤ 5 个

### 慢查询监控
- [ ] MySQL 慢查询日志开启
- [ ] 慢查询阈值配置（2 秒）
- [ ] 应用层慢查询监控
- [ ] 告警规则配置（P0/P1/P2）

### Flyway 迁移
- [ ] 迁移文件命名规范（V__ / R__）
- [ ] 迁移文件包含元数据注释
- [ ] 包含回滚 SQL（注释）
- [ ] validate-on-migrate 开启
- [ ] clean-disabled 开启（生产）

### 连接池
- [ ] HikariCP 配置合理
- [ ] 连接池监控指标暴露
- [ ] 连接泄漏检测开启（开发）
