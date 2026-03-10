-- ============================================================
-- 脚本：001-create-consultations.sql
-- 用途：创建问诊会话表
-- 作者：his_agent team
-- 日期：2026-03-10
-- ============================================================

-- 变更描述
-- - 创建 consultations 表
-- - 添加主键约束
-- - 创建索引用于查询优化

-- 使用数据库
USE his_agent;

-- 创建问诊会话表
CREATE TABLE IF NOT EXISTS consultations (
    -- 主键
    id VARCHAR(36) PRIMARY KEY COMMENT '问诊会话 ID (UUID)',
    
    -- 患者信息
    patient_id VARCHAR(36) NOT NULL COMMENT '患者 ID',
    patient_name VARCHAR(100) COMMENT '患者姓名',
    patient_gender ENUM('male', 'female', 'other') COMMENT '患者性别',
    patient_age INT COMMENT '患者年龄',
    
    -- 问诊状态
    status ENUM('idle', 'recording', 'processing', 'review', 'completed') 
        NOT NULL DEFAULT 'idle' COMMENT '问诊状态',
    
    -- 语音转写
    transcript TEXT COMMENT '完整转写文本',
    
    -- 时间戳
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP NULL COMMENT '结束时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_patient_id (patient_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='问诊会话表';

-- 验证表创建
SELECT TABLE_NAME, TABLE_COMMENT 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'his_agent' 
  AND TABLE_NAME = 'consultations';

-- 预期结果：consultations | 问诊会话表

-- ============================================================
-- 回滚语句 (如需撤销)
-- ============================================================
-- DROP TABLE IF EXISTS consultations;
