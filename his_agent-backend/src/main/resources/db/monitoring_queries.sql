-- =====================================================
-- 数据库监控查询
-- 用途：数据库性能监控和诊断
-- =====================================================

-- 1. 慢查询统计（最近 1 小时）
SELECT 
    DATE_FORMAT(start_time, '%Y-%m-%d %H:%i') AS time_slot,
    COUNT(*) AS slow_query_count,
    AVG(query_time) AS avg_query_time,
    MAX(query_time) AS max_query_time
FROM mysql.slow_log
WHERE start_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY DATE_FORMAT(start_time, '%Y-%m-%d %H:%i')
ORDER BY time_slot DESC;

-- 2. 表空间使用统计
SELECT 
    table_name AS `Table`,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS `Size (MB)`,
    ROUND((data_length / 1024 / 1024), 2) AS `Data (MB)',
    ROUND((index_length / 1024 / 1024), 2) AS `Index (MB)',
    table_rows AS `Rows`
FROM information_schema.TABLES
WHERE table_schema = 'his_agent'
ORDER BY (data_length + index_length) DESC;

-- 3. 索引使用统计
SELECT 
    table_name,
    index_name,
    seq_in_index,
    column_name,
    cardinality,
    nullable,
    index_type
FROM information_schema.STATISTICS
WHERE table_schema = 'his_agent'
ORDER BY table_name, index_name, seq_in_index;

-- 4. 连接池监控（需要 performance_schema）
SELECT 
    variable_name,
    variable_value
FROM performance_schema.global_status
WHERE variable_name LIKE 'Threads_%'
   OR variable_name LIKE 'Aborted_%'
ORDER BY variable_name;

-- 5. 锁等待查询
SELECT 
    r.trx_id waiting_trx_id,
    r.trx_mysql_pid waiting_thread,
    r.trx_query waiting_query,
    b.trx_id blocking_trx_id,
    b.trx_mysql_pid blocking_thread,
    b.trx_query blocking_query
FROM information_schema.innodb_lock_waits w
INNER JOIN information_schema.innodb_trx b ON b.trx_id = w.blocking_trx_id
INNER JOIN information_schema.innodb_trx r ON r.trx_id = w.requesting_trx_id;

-- 6. 表锁和行锁统计
SELECT 
    table_schema,
    table_name,
    rows_read,
    rows_inserted,
    rows_updated,
    rows_deleted
FROM information_schema.tables
WHERE table_schema = 'his_agent'
ORDER BY table_name;

-- =====================================================
-- 使用说明：
-- 1. 在 MySQL 客户端执行：source monitoring_queries.sql
-- 2. 定期执行以监控数据库健康状态
-- 3. 建议集成到监控系统中自动告警
-- =====================================================
