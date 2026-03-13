-- V1: 创建患者表
-- 日期：2026-03-12
-- 说明：患者基本信息表

CREATE TABLE IF NOT EXISTS patients (
    id VARCHAR(36) PRIMARY KEY COMMENT '患者 ID (UUID)',
    name VARCHAR(100) NOT NULL COMMENT '患者姓名',
    id_card VARCHAR(20) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '手机号',
    gender VARCHAR(10) COMMENT '性别',
    age INT COMMENT '年龄',
    address VARCHAR(500) COMMENT '地址',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_id_card (id_card),
    INDEX idx_phone (phone),
    INDEX idx_name (name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者信息表';
