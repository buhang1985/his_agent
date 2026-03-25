# MySQL 初始化脚本
-- 创建数据库和表
USE his_agent;

-- 创建患者表
CREATE TABLE IF NOT EXISTS patient (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    id_card VARCHAR(18) UNIQUE,
    phone VARCHAR(20),
    gender VARCHAR(10),
    age INT,
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_name (name),
    INDEX idx_phone (phone),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建用户表
CREATE TABLE IF NOT EXISTS user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    enabled TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试数据
INSERT INTO user (id, username, password, role, enabled) VALUES
('1', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKTm8VWYKfJZK3qZJZK3qZJZK3qZ', 'ADMIN', 1),
('2', 'doctor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKTm8VWYKfJZK3qZJZK3qZJZK3qZ', 'DOCTOR', 1);

INSERT INTO patient (id, name, id_card, phone, gender, age, address) VALUES
('p1', '张三', '110101199001011234', '13800138001', '男', 30, '北京市'),
('p2', '李四', '110101199002022345', '13800138002', '女', 25, '上海市'),
('p3', '王五', '110101199003033456', '13800138003', '男', 35, '广州市');
