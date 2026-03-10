-- ============================================================
-- 脚本：001-insert-sample-data.sql
-- 用途：插入示例数据
-- 作者：his_agent team
-- 日期：2026-03-10
-- ============================================================

-- 变更描述
-- - 插入示例患者问诊记录
-- - 用于开发和测试环境

USE his_agent;

-- 插入示例问诊记录
INSERT INTO consultations (
    id, 
    patient_id, 
    patient_name, 
    patient_gender, 
    patient_age, 
    status,
    transcript,
    start_time
) VALUES 
    (
        'C001',
        'P001',
        '张三',
        'male',
        45,
        'completed',
        '患者主诉头痛、发烧 2 天，体温 38.5°C，伴咳嗽。无恶心呕吐。查体：咽部充血，扁桃体 I 度肿大。',
        '2026-03-10 09:00:00'
    ),
    (
        'C002',
        'P002',
        '李四',
        'female',
        32,
        'review',
        '患者主诉腹痛，位于右下腹，持续 6 小时。伴恶心，无呕吐。查体：麦氏点压痛阳性。',
        '2026-03-10 10:30:00'
    ),
    (
        'C003',
        'P003',
        '王五',
        'male',
        58,
        'idle',
        NULL,
        '2026-03-10 14:00:00'
    );

-- 插入示例 SOAP 病历
INSERT INTO soap_notes (
    id,
    consultation_id,
    chief_complaint,
    history_of_present_illness,
    vital_signs,
    physical_exam_findings,
    primary_diagnosis,
    differential_diagnoses,
    diagnostic_tests,
    treatment_plan
) VALUES 
    (
        'SOAP001',
        'C001',
        '{"symptom": "头痛、发烧", "duration": "2 天", "severity": 7}',
        '{"onset": "2 天前", "progression": "逐渐加重", "aggravatingFactors": [], "relievingFactors": []}',
        '{"temperature": 38.5, "bloodPressure": {"systolic": 130, "diastolic": 85}, "heartRate": 95}',
        '{"throat": "充血", "tonsils": "I 度肿大", "lungs": "呼吸音粗"}',
        '{"condition": "急性上呼吸道感染", "icd10Code": "J06.9"}',
        '[{"condition": "流行性感冒", "likelihood": "medium", "icd10Code": "J11.1"}]',
        '[{"name": "血常规", "type": "lab", "priority": "routine"}, {"name": "C 反应蛋白", "type": "lab", "priority": "routine"}]',
        '{"medications": [{"name": "对乙酰氨基酚", "dosage": "500mg", "frequency": "每 6 小时一次"}]}'
    );

-- 插入示例诊断建议
INSERT INTO diagnosis_suggestions (
    id,
    consultation_id,
    soap_note_id,
    condition_name,
    icd10_code,
    likelihood,
    supporting_evidence,
    recommended_tests
) VALUES 
    (
        'DX001',
        'C001',
        'SOAP001',
        '急性上呼吸道感染',
        'J06.9',
        'high',
        '["发烧 38.5°C", "咽部充血", "扁桃体肿大"]',
        '["血常规", "C 反应蛋白"]'
    ),
    (
        'DX002',
        'C001',
        'SOAP001',
        '流行性感冒',
        'J11.1',
        'medium',
        '["头痛", "发烧", "咳嗽"]',
        '["流感病毒检测", "血常规"]'
    );

-- 验证插入数据
SELECT 
    'consultations' AS table_name, 
    COUNT(*) AS row_count 
FROM consultations
UNION ALL
SELECT 
    'soap_notes', 
    COUNT(*) 
FROM soap_notes
UNION ALL
SELECT 
    'diagnosis_suggestions', 
    COUNT(*) 
FROM diagnosis_suggestions;

-- 预期结果：
-- consultations | 3
-- soap_notes | 1
-- diagnosis_suggestions | 2

-- ============================================================
-- 回滚语句
-- ============================================================
-- DELETE FROM diagnosis_suggestions WHERE consultation_id IN ('C001', 'C002', 'C003');
-- DELETE FROM soap_notes WHERE consultation_id IN ('C001', 'C002', 'C003');
-- DELETE FROM consultations WHERE id IN ('C001', 'C002', 'C003');
