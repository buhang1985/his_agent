# Prometheus MySQL 和 Redis 监控配置指南

**版本**: 1.0  
**日期**: 2026-03-13  
**适用**: 运维工程师、系统管理员

---

## ❌ 问题描述

Prometheus 监控报错：
```
MySQL: Get "http://localhost:3306/metrics": net/http: HTTP/1.x transport connection broken: malformed HTTP response "[\x00\x00\x00"
Redis: Get "http://localhost:6379/metrics": EOF
```

**原因**：MySQL 和 Redis 不是 HTTP 服务，不提供 `/metrics` 端点。

---

## ✅ 解决方案

安装专门的 Exporter 来转换数据库指标为 Prometheus 格式。

### 1️⃣ 安装 MySQL Exporter

#### 下载并安装

```bash
cd /tmp
wget https://github.com/prometheus/mysqld_exporter/releases/download/v0.15.1/mysqld_exporter-0.15.1.linux-amd64.tar.gz
tar xzf mysqld_exporter-0.15.1.linux-amd64.tar.gz
mv mysqld_exporter-0.15.1.linux-amd64 /opt/
```

#### 创建 MySQL 用户

```sql
-- 登录 MySQL
sudo mysql -u root

-- 创建 exporter 用户
CREATE USER 'exporter'@'localhost' IDENTIFIED BY 'Exporter@2026';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 创建配置文件

```bash
cat > ~/.my.cnf << EOF
[client]
user=exporter
password=Exporter@2026
