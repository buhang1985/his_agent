-- V2: 创建问诊会话表
-- 日期：2026-03-12
-- 说明：医患问诊会话记录表

CREATE TABLE IF NOT EXISTS consultations (
    id VARCHAR(36) PRIMARY KEY COMMENT '问诊会话 ID (UUID)',
    patient_id VARCHAR(36) NOT NULL COMMENT '患者 ID',
    doctor_id VARCHAR(36) COMMENT '医生 ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending, in_progress, completed, cancelled',
    chief_complaint TEXT COMMENT '主诉',
    present_illness TEXT COMMENT '现病史',
    diagnosis TEXT COMMENT '诊断结果',
    prescription TEXT COMMENT '处方',
    audio_url VARCHAR(500) COMMENT '录音文件 URL',
    audio_duration INT COMMENT '录音时长 (秒)',
    started_at TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient_id (patient_id),
    INDEX idx_doctor_id (doctor_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问诊会话表';
