# Prometheus MySQL/Redis/Node 监控修复指南

**版本**: 1.0  
**日期**: 2026-03-13  
**状态**: ✅ MySQL/Redis 已解决，⚠️ Node Exporter 需手动启动

---

## ❌ 问题描述

Prometheus 监控报错：

```
MySQL: Get "http://localhost:3306/metrics": net/http: HTTP/1.x transport connection broken: malformed HTTP response "[\x00\x00\x00"
Node: Get "http://localhost:9100/metrics": dial tcp 127.0.0.1:9100: connect: connection refused
Redis: Get "http://localhost:6379/metrics": EOF
```

**根本原因**：
- MySQL 和 Redis 不是 HTTP 服务，不提供 `/metrics` 端点
- Node Exporter 没有运行
- Prometheus 配置错误（直接指向数据库端口而非 Exporter 端口）

---

## ✅ 解决方案

### 1️⃣ MySQL Exporter（✅ 已解决）

**安装**：
```bash
cd /tmp
wget https://github.com/prometheus/mysqld_exporter/releases/download/v0.15.1/mysqld_exporter-0.15.1.linux-amd64.tar.gz
tar xzf mysqld_exporter-0.15.1.linux-amd64.tar.gz
```

**MySQL 用户配置**：
```sql
CREATE USER 'exporter'@'localhost' IDENTIFIED BY 'Exporter@2026';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'localhost';
FLUSH PRIVILEGES;
```

**启动**：
```bash
/tmp/mysqld_exporter-0.15.1.linux-amd64/mysqld_exporter \
  --config.my-cnf=/home/yuzihao/.my.cnf \
  --web.listen-address=":9104" &
```

**验证**：
```bash
curl -s http://localhost:9104/metrics | grep "^mysql_up"
# 返回：mysql_up 1
```

---

### 2️⃣ Redis Exporter（✅ 已解决）

**安装**：
```bash
cd /tmp
wget https://github.com/oliver006/redis_exporter/releases/download/v1.55.0/redis_exporter-v1.55.0.linux-amd64.tar.gz
tar xzf redis_exporter-v1.55.0.linux-amd64.tar.gz
```

**启动**：
```bash
/tmp/redis_exporter-v1.55.0.linux-amd64/redis_exporter \
  -redis.addr="localhost:6379" \
  -web.listen-address=":9121" &
```

**验证**：
```bash
curl -s http://localhost:9121/metrics | grep "^redis_up"
# 返回：redis_up 1
```

---

### 3️⃣ Node Exporter（⚠️ 需手动启动）

**安装**：
```bash
cd /tmp
wget https://github.com/prometheus/node_exporter/releases/download/v1.7.0/node_exporter-1.7.0.linux-amd64.tar.gz
tar xzf node_exporter-1.7.0.linux-amd64.tar.gz
```

**启动**：
```bash
/tmp/node_exporter-1.7.0.linux-amd64/node_exporter &
```

**验证**：
```bash
curl -s http://localhost:9100/metrics | grep "^node_exporter_build_info"
```

---

## 🔧 Prometheus 配置

编辑 `monitoring/prometheus/prometheus.yml`：

```yaml
scrape_configs:
  # MySQL Exporter
  - job_name: 'mysql'
    static_configs:
      - targets: ['localhost:9104']  # ← 不是 3306
        labels:
          service: 'mysql'
  
  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']  # ← 不是 6379
        labels:
          service: 'redis'
  
  # Node Exporter
  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']
        labels:
          instance: 'his-agent-server'
```

**重载配置**：
```bash
curl -X POST http://localhost:9090/-/reload
```

---

## 📊 关键指标

### MySQL 指标（端口 9104）
- `mysql_up` - 连接状态（1=正常）
- `mysql_global_connections` - 总连接数
- `mysql_global_status_queries` - 查询总数

### Redis 指标（端口 9121）
- `redis_up` - 连接状态（1=正常）
- `redis_connected_clients` - 连接客户端数
- `redis_used_memory_bytes` - 内存使用

### Node 指标（端口 9100）
- `node_cpu_seconds_total` - CPU 使用时间
- `node_memory_MemAvailable_bytes` - 可用内存
- `node_filesystem_avail_bytes` - 文件系统可用空间

---

## 🛠️ 故障排查

### MySQL Exporter 无法连接
```bash
# 检查进程
ps aux | grep mysqld_exporter

# 检查日志
cat /tmp/mysql-exporter.log

# 测试连接
mysql -u exporter -p'Exporter@2026' -e "SELECT 1;"
```

### Redis Exporter 无法连接
```bash
# 检查进程
ps aux | grep redis_exporter

# 检查 Redis 连接
redis-cli ping

# 测试 Exporter
curl -s http://localhost:9121/metrics | head -5
```

### Node Exporter 无法连接
```bash
# 检查进程
ps aux | grep node_exporter

# 重新启动
/tmp/node_exporter-1.7.0.linux-amd64/node_exporter &

# 测试
curl -s http://localhost:9100/metrics | head -5
```

---

## 📝 服务管理（systemd）

### MySQL Exporter 服务
```bash
sudo cat > /etc/systemd/system/mysqld-exporter.service << EOF
[Unit]
Description=MySQL Exporter
After=mysql.service

[Service]
ExecStart=/tmp/mysqld_exporter-0.15.1.linux-amd64/mysqld_exporter --config.my-cnf=/home/yuzihao/.my.cnf --web.listen-address=":9104"
Restart=always

[Install]
WantedBy=multi-user.target
