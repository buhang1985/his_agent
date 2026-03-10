-- ============================================================
-- 脚本：002-insert-medical-terms.sql
-- 用途：插入医学词库数据
-- 作者：his_agent team
-- 日期：2026-03-10
-- ============================================================

-- 变更描述
-- - 插入常见疾病名称
-- - 插入常用药品名称
-- - 插入检查检验项目

USE his_agent;

-- 创建医学词库表 (如不存在)
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

-- 插入常见疾病
INSERT INTO medical_terms (term, category, icd10_code) VALUES
    ('急性上呼吸道感染', 'disease', 'J06.9'),
    ('流行性感冒', 'disease', 'J11.1'),
    ('急性支气管炎', 'disease', 'J20.9'),
    ('肺炎', 'disease', 'J18.9'),
    ('高血压', 'disease', 'I10'),
    ('糖尿病', 'disease', 'E11.9'),
    ('冠心病', 'disease', 'I25.1'),
    ('急性胃炎', 'disease', 'K29.0'),
    ('尿路感染', 'disease', 'N39.0'),
    ('贫血', 'disease', 'D64.9');

-- 插入常见症状
INSERT INTO medical_terms (term, category) VALUES
    ('头痛', 'symptom'),
    ('发烧', 'symptom'),
    ('咳嗽', 'symptom'),
    ('腹痛', 'symptom'),
    ('恶心', 'symptom'),
    ('呕吐', 'symptom'),
    ('腹泻', 'symptom'),
    ('乏力', 'symptom'),
    ('头晕', 'symptom'),
    ('胸闷', 'symptom');

-- 插入常用药品
INSERT INTO medical_terms (term, category) VALUES
    ('对乙酰氨基酚', 'drug'),
    ('布洛芬', 'drug'),
    ('阿莫西林', 'drug'),
    ('头孢克肟', 'drug'),
    ('奥美拉唑', 'drug'),
    ('二甲双胍', 'drug'),
    ('硝苯地平', 'drug'),
    ('阿司匹林', 'drug'),
    ('氯雷他定', 'drug'),
    ('蒙脱石散', 'drug');

-- 插入检查检验项目
INSERT INTO medical_terms (term, category) VALUES
    ('血常规', 'test'),
    ('尿常规', 'test'),
    ('大便常规', 'test'),
    ('肝功能', 'test'),
    ('肾功能', 'test'),
    ('血糖', 'test'),
    ('血脂', 'test'),
    ('心电图', 'test'),
    ('X 光胸片', 'test'),
    ('CT 检查', 'test');

-- 验证插入数据
SELECT 
    category, 
    COUNT(*) AS count 
FROM medical_terms 
GROUP BY category;

-- 预期结果：
-- disease | 10
-- symptom | 10
-- drug | 10
-- test | 10

-- ============================================================
-- 回滚语句
-- ============================================================
-- DELETE FROM medical_terms WHERE category IN ('disease', 'symptom', 'drug', 'test');
-- DROP TABLE IF EXISTS medical_terms;
