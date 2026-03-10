-- ============================================================
-- 脚本：002-create-soap-notes.sql
-- 用途：创建 SOAP 病历表
-- 作者：his_agent team
-- 日期：2026-03-10
-- ============================================================

-- 变更描述
-- - 创建 soap_notes 表
-- - 添加主键、外键约束
-- - 创建 JSON 字段存储结构化病历

USE his_agent;

-- 创建 SOAP 病历表
CREATE TABLE IF NOT EXISTS soap_notes (
    -- 主键
    id VARCHAR(36) PRIMARY KEY COMMENT '病历 ID (UUID)',
    
    -- 外键关联
    consultation_id VARCHAR(36) NOT NULL COMMENT '问诊会话 ID',
    
    -- 主观资料 (Subjective)
    chief_complaint JSON COMMENT '主诉 {"symptom": "", "duration": ""}',
    history_of_present_illness JSON COMMENT '现病史',
    
    -- 客观资料 (Objective)
    vital_signs JSON COMMENT '生命体征 {"temperature": 36.5, "bp": {"systolic": 120, "diastolic": 80}}',
    physical_exam_findings JSON COMMENT '体格检查结果',
    
    -- 评估 (Assessment)
    primary_diagnosis JSON COMMENT '主要诊断 {"condition": "", "icd10Code": ""}',
    differential_diagnoses JSON COMMENT '鉴别诊断列表',
    
    -- 计划 (Plan)
    diagnostic_tests JSON COMMENT '检查建议',
    treatment_plan JSON COMMENT '治疗方案',
    
    -- 审核状态
    is_reviewed BOOLEAN DEFAULT FALSE COMMENT '是否已审核',
    reviewed_by VARCHAR(36) COMMENT '审核医生 ID',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    
    -- 时间戳
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    CONSTRAINT fk_soap_consultation 
        FOREIGN KEY (consultation_id) 
        REFERENCES consultations(id) 
        ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_consultation_id (consultation_id),
    INDEX idx_reviewed (is_reviewed),
    INDEX idx_created_at (created_at)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='SOAP 病历表';

-- 验证表创建
SELECT TABLE_NAME, TABLE_COMMENT 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'his_agent' 
  AND TABLE_NAME = 'soap_notes';

-- 预期结果：soap_notes | SOAP 病历表

-- ============================================================
-- 回滚语句
-- ============================================================
-- DROP TABLE IF EXISTS soap_notes;
