-- ============================================================
-- Flyway 迁移脚本：V1__initial_schema.sql
-- 版本：1.0
-- 描述：初始化数据库 schema
-- 日期：2026-03-10
-- ============================================================

-- 创建问诊会话表
CREATE TABLE IF NOT EXISTS consultations (
    id VARCHAR(36) PRIMARY KEY COMMENT '问诊会话 ID (UUID)',
    patient_id VARCHAR(36) NOT NULL COMMENT '患者 ID',
    patient_name VARCHAR(100) COMMENT '患者姓名',
    patient_gender ENUM('male', 'female', 'other') COMMENT '患者性别',
    patient_age INT COMMENT '患者年龄',
    status ENUM('idle', 'recording', 'processing', 'review', 'completed') 
        NOT NULL DEFAULT 'idle' COMMENT '问诊状态',
    transcript TEXT COMMENT '完整转写文本',
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP NULL COMMENT '结束时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_patient_id (patient_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='问诊会话表';

-- 创建 SOAP 病历表
CREATE TABLE IF NOT EXISTS soap_notes (
    id VARCHAR(36) PRIMARY KEY COMMENT '病历 ID (UUID)',
    consultation_id VARCHAR(36) NOT NULL COMMENT '问诊会话 ID',
    chief_complaint JSON COMMENT '主诉',
    history_of_present_illness JSON COMMENT '现病史',
    vital_signs JSON COMMENT '生命体征',
    physical_exam_findings JSON COMMENT '体格检查结果',
    primary_diagnosis JSON COMMENT '主要诊断',
    differential_diagnoses JSON COMMENT '鉴别诊断列表',
    diagnostic_tests JSON COMMENT '检查建议',
    treatment_plan JSON COMMENT '治疗方案',
    is_reviewed BOOLEAN DEFAULT FALSE COMMENT '是否已审核',
    reviewed_by VARCHAR(36) COMMENT '审核医生 ID',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_soap_consultation 
        FOREIGN KEY (consultation_id) 
        REFERENCES consultations(id) 
        ON DELETE CASCADE,
    INDEX idx_consultation_id (consultation_id),
    INDEX idx_reviewed (is_reviewed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='SOAP 病历表';

-- 创建诊断建议表
CREATE TABLE IF NOT EXISTS diagnosis_suggestions (
    id VARCHAR(36) PRIMARY KEY COMMENT '诊断建议 ID (UUID)',
    consultation_id VARCHAR(36) NOT NULL COMMENT '问诊会话 ID',
    soap_note_id VARCHAR(36) COMMENT '病历 ID',
    condition_name VARCHAR(200) NOT NULL COMMENT '诊断名称',
    icd10_code VARCHAR(20) COMMENT 'ICD-10 编码',
    likelihood ENUM('high', 'medium', 'low') NOT NULL COMMENT '可能性',
    supporting_evidence JSON COMMENT '支持证据列表',
    recommended_tests JSON COMMENT '建议检查项目',
    notes TEXT COMMENT '备注说明',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_diagnosis_consultation 
        FOREIGN KEY (consultation_id) 
        REFERENCES consultations(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_diagnosis_soap_note 
        FOREIGN KEY (soap_note_id) 
        REFERENCES soap_notes(id) 
        ON DELETE SET NULL,
    INDEX idx_consultation_id (consultation_id),
    INDEX idx_icd10_code (icd10_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='诊断建议表';

-- 创建医学词库表
CREATE TABLE IF NOT EXISTS medical_terms (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '词库 ID',
    term VARCHAR(200) NOT NULL COMMENT '医学术语',
    category ENUM('disease', 'symptom', 'drug', 'test', 'procedure') 
        NOT NULL COMMENT '术语分类',
    pinyin VARCHAR(500) COMMENT '拼音',
    synonyms JSON COMMENT '同义词',
    icd10_code VARCHAR(20) COMMENT 'ICD-10 编码 (仅疾病)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_term (term),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='医学词库表';
