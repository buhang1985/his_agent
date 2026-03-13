# 数据库命名规范

## 1. 表命名规范

### 基本原则
- 使用小写字母和下划线分隔
- 使用复数形式
- 表名长度不超过 64 个字符
- 避免使用 MySQL 保留字

### 命名格式
```
{业务模块}_{实体类型}

示例：
- patients          # 患者信息表
- consultations     # 问诊会话表
- users             # 用户表
- auth_tokens       # 认证令牌表
- medical_records   # 病历记录表
```

### 关联表命名
```
{表 1}_{表 2}_rel 或 {表 1}_{表 2}_mapping

示例：
- doctor_patient_rel    # 医患关系表
- user_role_mapping     # 用户角色映射表
```

## 2. 字段命名规范

### 基本原则
- 使用小写字母和下划线分隔
- 语义清晰，避免缩写（除非是通用缩写）
- 布尔类型使用 is/has/can 前缀

### 标准字段
```
id              # 主键（所有表统一使用 id）
created_at      # 创建时间（TIMESTAMP）
updated_at      # 更新时间（TIMESTAMP）
deleted_at      # 删除时间（软删除时使用）
created_by      # 创建人 ID
updated_by      # 更新人 ID
```

### 数据类型规范
```
VARCHAR(n)      # 字符串，n 根据实际需求设定
TEXT            # 长文本
INT             # 整数
BIGINT          # 大整数（ID、计数等）
DECIMAL(10,2)   # 金额
DATE            # 日期
DATETIME        # 日期时间
TINYINT(1)      # 布尔值（0/1）
JSON            # JSON 数据
```

## 3. 索引命名规范

### 命名格式
```
idx_{表名}_{字段名}           # 普通索引
uk_{表名}_{字段名}            # 唯一索引
fk_{表名}_{关联表名}          # 外键索引

示例：
- idx_patients_name           # patients 表 name 字段索引
- uk_users_username           # users 表 username 唯一索引
- fk_consultations_patients   # consultations 表 patients 外键索引
```

### 索引设计原则
1. 为 WHERE 子句中的字段创建索引
2. 为 JOIN 字段创建索引
3. 为 ORDER BY 字段创建索引
4. 避免在低基数字段上创建索引
5. 使用复合索引时注意最左前缀原则

## 4. 示例

### 患者表
```sql
CREATE TABLE patients (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '患者姓名',
    id_card VARCHAR(20) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '手机号',
    gender VARCHAR(10) COMMENT '性别',
    age INT COMMENT '年龄',
    address VARCHAR(500) COMMENT '地址',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_patients_name (name),
    INDEX idx_patients_phone (phone),
    UNIQUE INDEX uk_patients_id_card (id_card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息表';
```

## 5. 审查清单

### 表设计审查
- [ ] 表名是否符合复数形式
- [ ] 是否有 id、created_at、updated_at 字段
- [ ] 字符集是否为 utf8mb4
- [ ] 存储引擎是否为 InnoDB
- [ ] 是否有适当的注释

### 索引设计审查
- [ ] 主键是否已定义
- [ ] 查询字段是否有索引
- [ ] 外键字段是否有索引
- [ ] 唯一约束是否使用唯一索引
- [ ] 索引数量是否合理（不超过 5 个）

### SQL 审查
- [ ] 是否避免 SELECT *
- [ ] 是否使用参数化查询
- [ ] 是否避免 N+1 查询
- [ ] 是否有适当的 LIMIT
- [ ] 是否避免在索引列上使用函数
