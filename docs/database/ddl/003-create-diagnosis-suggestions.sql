-- ============================================================
-- 脚本：003-create-diagnosis-suggestions.sql
-- 用途：创建诊断建议表
-- 作者：his_agent team
-- 日期：2026-03-10
-- ============================================================

-- 变更描述
-- - 创建 diagnosis_suggestions 表
-- - 存储 AI 生成的诊断建议
-- - 支持 ICD-10 编码

USE his_agent;

-- 创建诊断建议表
CREATE TABLE IF NOT EXISTS diagnosis_suggestions (
    -- 主键
    id VARCHAR(36) PRIMARY KEY COMMENT '诊断建议 ID (UUID)',
    
    -- 外键关联
    consultation_id VARCHAR(36) NOT NULL COMMENT '问诊会话 ID',
    soap_note_id VARCHAR(36) COMMENT '病历 ID',
    
    -- 诊断信息
    condition_name VARCHAR(200) NOT NULL COMMENT '诊断名称',
    icd10_code VARCHAR(20) COMMENT 'ICD-10 编码',
    likelihood ENUM('high', 'medium', 'low') NOT NULL COMMENT '可能性',
    
    -- 支持证据
    supporting_evidence JSON COMMENT '支持证据列表',
    
    -- 建议检查
    recommended_tests JSON COMMENT '建议检查项目',
    
    -- 备注
    notes TEXT COMMENT '备注说明',
    
    -- 时间戳
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 外键约束
    CONSTRAINT fk_diagnosis_consultation 
        FOREIGN KEY (consultation_id) 
        REFERENCES consultations(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_diagnosis_soap_note 
        FOREIGN KEY (soap_note_id) 
        REFERENCES soap_notes(id) 
        ON DELETE SET NULL,
    
    -- 索引
    INDEX idx_consultation_id (consultation_id),
    INDEX idx_icd10_code (icd10_code),
    INDEX idx_likelihood (likelihood)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='诊断建议表';

-- 验证表创建
SELECT TABLE_NAME, TABLE_COMMENT 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'his_agent' 
  AND TABLE_NAME = 'diagnosis_suggestions';

-- 预期结果：diagnosis_suggestions | 诊断建议表

-- ============================================================
-- 回滚语句
-- ============================================================
-- DROP TABLE IF EXISTS diagnosis_suggestions;
