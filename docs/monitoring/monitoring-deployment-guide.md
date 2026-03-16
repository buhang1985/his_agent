# HIS Agent 监控系统部署指南

**版本**: 2.0  
**日期**: 2026-03-16  
**适用人群**: 运维人员（零基础可上手）

---

## 📋 目录

1. [什么是监控系统](#什么是监控系统)
2. [系统要求](#系统要求)
3. [组件说明](#组件说明)
4. [安装步骤](#安装步骤)
5. [配置说明](#配置说明)
6. [启动服务](#启动服务)
7. [验证部署](#验证部署)
8. [常见问题](#常见问题)
9. [维护指南](#维护指南)

---

## 什么是监控系统

监控系统用于实时收集、存储和展示服务器及应用程序的运行状态。当系统出现异常时，能够及时告警，帮助运维人员快速发现问题并处理。

### 监控系统能做什么？

| 功能 | 说明 |
|------|------|
| **性能监控** | CPU、内存、磁盘、网络使用情况 |
| **应用监控** | 应用响应时间、错误率、连接池状态 |
| **数据库监控** | MySQL、Redis 的运行状态和性能指标 |
| **告警通知** | 系统异常时自动发送告警 |
| **可视化** | 通过图表直观展示系统状态 |

---

## 系统要求

### 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 50 GB | 100 GB |

### 软件要求

- 操作系统：Linux (Ubuntu 20.04+, CentOS 7+)
- 网络：能访问外网（下载安装包）
- 权限：root 或 sudo 权限

---

## 组件说明

### 核心组件

| 组件 | 端口 | 用途 | 是否必须 |
|------|------|------|----------|
| **Prometheus** | 9090 | 指标收集和存储 | ✅ 是 |
| **Grafana** | 3000 | 图表展示和仪表板 | ✅ 是 |
| **AlertManager** | 9093 | 告警通知管理 | ✅ 是 |
| **Node Exporter** | 9100 | 服务器性能指标 | ✅ 是 |

### 数据库监控组件

| 组件 | 端口 | 用途 | 是否必须 | 注意事项 |
|------|------|------|----------|----------|
| **MySQL Exporter** | 9104 | MySQL 性能指标 | ✅ 是 | **不是 MySQL 本身** |
| **Redis Exporter** | 9121 | Redis 性能指标 | ✅ 是 | **不是 Redis 本身** |

### ⚠️ 重要提示：端口区别

很多运维人员容易混淆以下端口：

| 服务 | 数据库端口 | Exporter 端口 | 说明 |
|------|------------|---------------|------|
| MySQL | 3306 | **9104** | MySQL Exporter 专门用于监控 MySQL |
| Redis | 6379 | **9121** | Redis Exporter 专门用于监控 Redis |

**Prometheus 通过 Exporter 端口获取指标，而不是直接连接数据库端口！**

---

## 安装步骤

### 步骤 1：创建安装目录

```bash
# 创建安装目录
sudo mkdir -p /mnt/d/his-agent-monitoring/{prometheus,grafana,alertmanager,node_exporter,data}

# 进入安装目录
cd /mnt/d/his-agent-monitoring
```

### 步骤 2：下载组件

```bash
# 下载 Prometheus
wget -q https://github.com/prometheus/prometheus/releases/download/v2.50.1/prometheus-2.50.1.linux-amd64.tar.gz
tar xzf prometheus-2.50.1.linux-amd64.tar.gz
mv prometheus-2.50.1.linux-amd64/* prometheus/
rm -rf prometheus-2.50.1.linux-amd64 prometheus-2.50.1.linux-amd64.tar.gz

# 下载 Grafana
wget -q https://dl.grafana.com/oss/release/grafana-10.3.3.linux-amd64.tar.gz
tar xzf grafana-10.3.3.linux-amd64.tar.gz
mv grafana-v10.3.3/* grafana/
rm -rf grafana-v10.3.3 grafana-10.3.3.linux-amd64.tar.gz

# 下载 AlertManager
wget -q https://github.com/prometheus/alertmanager/releases/download/v0.26.0/alertmanager-0.26.0.linux-amd64.tar.gz
tar xzf alertmanager-0.26.0.linux-amd64.tar.gz
mv alertmanager-0.26.0.linux-amd64/* alertmanager/
rm -rf alertmanager-0.26.0.linux-amd64 alertmanager-0.26.0.linux-amd64.tar.gz

# 下载 Node Exporter
wget -q https://github.com/prometheus/node_exporter/releases/download/v1.7.0/node_exporter-1.7.0.linux-amd64.tar.gz
tar xzf node_exporter-1.7.0.linux-amd64.tar.gz
mv node_exporter-1.7.0.linux-amd64/* node_exporter/
rm -rf node_exporter-1.7.0.linux-amd64 node_exporter-1.7.0.linux-amd64.tar.gz
```

### 步骤 3：下载 MySQL Exporter 和 Redis Exporter

```bash
# 下载 MySQL Exporter
wget -q https://github.com/prometheus/mysqld_exporter/releases/download/v0.14.0/mysqld_exporter-0.14.0.linux-amd64.tar.gz
tar xzf mysqld_exporter-0.14.0.linux-amd64.tar.gz
mv mysqld_exporter-0.14.0.linux-amd64 mysqld_exporter
rm mysqld_exporter-0.14.0.linux-amd64.tar.gz

# 下载 Redis Exporter
wget -q https://github.com/oliver006/redis_exporter/releases/download/v1.55.0/redis_exporter-v1.55.0.linux-amd64.tar.gz
tar xzf redis_exporter-v1.55.0.linux-amd64.tar.gz
mv redis_exporter redis_exporter-v1.55.0.linux-amd64
rm redis_exporter-v1.55.0.linux-amd64.tar.gz
```

### 步骤 4：复制配置文件

```bash
# 复制 Prometheus 配置
cp /home/yuzihao/workspace/his_agent/monitoring/prometheus/prometheus.yml /mnt/d/his-agent-monitoring/prometheus/

# 复制告警规则
cp -r /home/yuzihao/workspace/his_agent/monitoring/prometheus/alerts /mnt/d/his-agent-monitoring/prometheus/

# 复制 AlertManager 配置
cp /home/yuzihao/workspace/his_agent/monitoring/alertmanager/alertmanager.yml /mnt/d/his-agent-monitoring/alertmanager/
```

### 步骤 5：配置 MySQL Exporter（重要！）

MySQL Exporter 需要连接到 MySQL 数据库获取指标。首先创建配置文件：

```bash
# 创建 MySQL 导出器配置目录
mkdir -p /home/yuzihao/.my.cnf.d

# 创建配置文件
cat > /home/yuzihao/.my.cnf << 'EOF'
[client]
user=exporter
password=Exporter@2026
EOF

# 设置权限
chmod 600 /home/yuzihao/.my.cnf
```

**注意**：需要在 MySQL 中创建 `exporter` 用户：

```sql
-- 在 MySQL 中执行
CREATE USER 'exporter'@'localhost' IDENTIFIED BY 'Exporter@2026';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'localhost';
FLUSH PRIVILEGES;
```

### 步骤 6：创建 systemd 服务

创建以下服务文件：

#### Prometheus 服务

```bash
sudo cat > /etc/systemd/system/prometheus.service << 'EOF'
[Unit]
Description=Prometheus
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/mnt/d/his-agent-monitoring/prometheus/prometheus \
  --config.file=/mnt/d/his-agent-monitoring/prometheus/prometheus.yml \
  --storage.tsdb.path=/mnt/d/his-agent-monitoring/data/prometheus \
  --storage.tsdb.retention.time=15d \
  --web.enable-lifecycle \
  --web.enable-admin-api
ExecReload=/bin/kill -HUP $MAINPID
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

#### Grafana 服务

```bash
sudo cat > /etc/systemd/system/grafana.service << 'EOF'
[Unit]
Description=Grafana
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
Environment="GF_PATHS_DATA=/mnt/d/his-agent-monitoring/data/grafana"
Environment="GF_PATHS_LOGS=/mnt/d/his-agent-monitoring/logs/grafana"
Environment="GF_PATHS_PLUGINS=/mnt/d/his-agent-monitoring/grafana/plugins"
ExecStart=/mnt/d/his-agent-monitoring/grafana/bin/grafana-server \
  --homepath=/mnt/d/his-agent-monitoring/grafana \
  --config=/mnt/d/his-agent-monitoring/grafana/conf/defaults.ini
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

#### AlertManager 服务

```bash
sudo cat > /etc/systemd/system/alertmanager.service << 'EOF'
[Unit]
Description=AlertManager
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/mnt/d/his-agent-monitoring/alertmanager/alertmanager \
  --config.file=/mnt/d/his-agent-monitoring/alertmanager/alertmanager.yml \
  --storage.path=/mnt/d/his-agent-monitoring/data/alertmanager \
  --web.external-url=http://localhost:9093
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

#### Node Exporter 服务

```bash
sudo cat > /etc/systemd/system/node_exporter.service << 'EOF'
[Unit]
Description=Node Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/mnt/d/his-agent-monitoring/node_exporter/node_exporter
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

#### MySQL Exporter 服务

```bash
sudo cat > /etc/systemd/system/mysqld_exporter.service << 'EOF'
[Unit]
Description=MySQL Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/mnt/d/his-agent-monitoring/mysqld_exporter/mysqld_exporter \
  --config.my-cnf=/home/yuzihao/.my.cnf \
  --web.listen-address=:9104
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

#### Redis Exporter 服务

```bash
sudo cat > /etc/systemd/system/redis_exporter.service << 'EOF'
[Unit]
Description=Redis Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/mnt/d/his-agent-monitoring/redis_exporter/redis_exporter \
  -redis.addr=localhost:6379 \
  -web.listen-address=:9121
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
```

### 步骤 7：重新加载 systemd 并启动服务

```bash
# 重新加载 systemd 配置
sudo systemctl daemon-reload

# 设置开机自启
sudo systemctl enable prometheus grafana alertmanager node_exporter mysqld_exporter redis_exporter

# 启动所有服务
sudo systemctl start prometheus
sudo systemctl start grafana
sudo systemctl start alertmanager
sudo systemctl start node_exporter
sudo systemctl start mysqld_exporter
sudo systemctl start redis_exporter
```

---

## 配置说明

### Prometheus 配置文件位置

```
/mnt/d/his-agent-monitoring/prometheus/prometheus.yml
```

### 关键配置说明

```yaml
# 抓取间隔：15 秒
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# 监控目标配置
scrape_configs:
  # Prometheus 自监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # HIS Agent 应用监控
  - job_name: 'his-agent'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']

  # Node Exporter（系统监控）
  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']

  # MySQL Exporter（注意端口是 9104，不是 3306）
  - job_name: 'mysql'
    metrics_path: '/metrics'
    static_configs:
      - targets: ['localhost:9104']  # ✅ 正确：Exporter 端口

  # Redis Exporter（注意端口是 9121，不是 6379）
  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']  # ✅ 正确：Exporter 端口
```

### ⚠️ 常见错误：端口配置错误

**错误配置（会导致监控失败）**：
```yaml
# ❌ 错误：连接数据库端口
- targets: ['localhost:3306']  # MySQL 数据库端口
- targets: ['localhost:6379']  # Redis 数据库端口
```

**正确配置**：
```yaml
# ✅ 正确：连接 Exporter 端口
- targets: ['localhost:9104']  # MySQL Exporter 端口
- targets: ['localhost:9121']  # Redis Exporter 端口
```

---

## 启动服务

### 启动所有服务

```bash
# 启动服务
sudo systemctl start prometheus grafana alertmanager node_exporter mysqld_exporter redis_exporter

# 查看服务状态
sudo systemctl status prometheus
sudo systemctl status grafana
sudo systemctl status alertmanager
sudo systemctl status node_exporter
sudo systemctl status mysqld_exporter
sudo systemctl status redis_exporter
```

### 手动启动（调试用）

```bash
# 启动 Prometheus
/mnt/d/his-agent-monitoring/prometheus/prometheus \
  --config.file=/mnt/d/his-agent-monitoring/prometheus/prometheus.yml \
  --storage.tsdb.path=/mnt/d/his-agent-monitoring/data/prometheus \
  --storage.tsdb.retention.time=15d \
  --web.enable-lifecycle \
  --web.enable-admin-api

# 启动 MySQL Exporter
/mnt/d/his-agent-monitoring/mysqld_exporter/mysqld_exporter \
  --config.my-cnf=/home/yuzihao/.my.cnf \
  --web.listen-address=:9104

# 启动 Redis Exporter
/mnt/d/his-agent-monitoring/redis_exporter/redis_exporter \
  -redis.addr=localhost:6379 \
  -web.listen-address=:9121
```

---

## 验证部署

### 1. 检查服务状态

```bash
# 检查所有服务是否运行
sudo systemctl status prometheus | grep "Active:"
sudo systemctl status grafana | grep "Active:"
sudo systemctl status alertmanager | grep "Active:"
sudo systemctl status node_exporter | grep "Active:"
sudo systemctl status mysqld_exporter | grep "Active:"
sudo systemctl status redis_exporter | grep "Active:"
```

### 2. 检查端口监听

```bash
# 检查端口是否在监听
netstat -tlnp | grep -E "(9090|3000|9093|9100|9104|9121)"
```

### 3. 访问 Web 界面

| 组件 | 地址 | 说明 |
|------|------|------|
| Prometheus | http://localhost:9090 | 指标查询 |
| Grafana | http://localhost:3000 | 图表展示（admin/admin） |
| AlertManager | http://localhost:9093 | 告警管理 |
| Node Exporter | http://localhost:9100/metrics | 系统指标 |

### 4. 验证 Prometheus Targets

访问：http://localhost:9090/targets

**预期结果**：所有目标状态应为 **UP**

```
✅ prometheus (localhost:9090) - UP
✅ his-agent (localhost:8080) - UP
✅ node (localhost:9100) - UP
✅ mysql (localhost:9104) - UP  ← 注意端口
✅ redis (localhost:9121) - UP  ← 注意端口
```

### 5. 验证指标抓取

在 Prometheus Web 界面的 "Graph" 标签页中，输入以下查询：

```promql
# 检查 MySQL 是否正常
mysql_up

# 检查 Redis 是否正常
redis_up

# 检查系统 CPU 使用率
rate(process_cpu_seconds_total{job="his-agent"}[5m])

# 检查系统内存使用率
jvm_memory_used_bytes{job="his-agent"} / jvm_memory_max_bytes{job="his-agent"}
```

如果返回数值，说明监控正常。

### 6. 验证 Grafana 数据源

1. 访问 http://localhost:3000
2. 登录：admin / admin
3. 进入 **Configuration > Data Sources**
4. 点击 **Prometheus**
5. 点击 **Save & test**
6. 应显示 "Data source is working"

---

## 常见问题

### Q1: Prometheus 启动失败

**问题**：`systemctl start prometheus` 失败

**排查步骤**：

```bash
# 查看详细日志
journalctl -u prometheus -f

# 检查配置文件语法
/mnt/d/his-agent-monitoring/prometheus/prometheus --config.file=/mnt/d/his-agent-monitoring/prometheus/prometheus.yml --web.enable-lifecycle --web.enable-admin-api --storage.tsdb.path=/mnt/d/his-agent-monitoring/data/prometheus --storage.tsdb.retention.time=15d

# 检查端口是否被占用
sudo lsof -i :9090
```

### Q2: MySQL 或 Redis 显示 DOWN

**问题**：Prometheus targets 中 MySQL 或 Redis 状态为 DOWN

**可能原因**：
1. Exporter 没有启动
2. 端口配置错误（配置成数据库端口而不是 Exporter 端口）

**排查步骤**：

```bash
# 检查 Exporter 是否运行
ps aux | grep mysqld_exporter
ps aux | grep redis_exporter

# 检查 Exporter 端口
curl http://localhost:9104/metrics | head -5  # MySQL
curl http://localhost:9121/metrics | head -5  # Redis

# 检查 Prometheus 配置
cat /mnt/d/his-agent-monitoring/prometheus/prometheus.yml | grep -A 5 "job_name: 'mysql'"
cat /mnt/d/his-agent-monitoring/prometheus/prometheus.yml | grep -A 5 "job_name: 'redis'"
```

**正确配置**：
```yaml
# MySQL Exporter - 端口 9104（不是 3306）
- job_name: 'mysql'
  static_configs:
    - targets: ['localhost:9104']  # ✅ 正确

# Redis Exporter - 端口 9121（不是 6379）
- job_name: 'redis'
  static_configs:
    - targets: ['localhost:9121']  # ✅ 正确
```

### Q3: MySQL Exporter 连接失败

**问题**：MySQL Exporter 启动后无法获取指标

**排查步骤**：

```bash
# 检查配置文件
cat /home/yuzihao/.my.cnf

# 检查 MySQL 用户
mysql -u root -p -e "SELECT User, Host FROM mysql.user WHERE User='exporter';"

# 手动测试 Exporter
/mnt/d/his-agent-monitoring/mysqld_exporter/mysqld_exporter \
  --config.my-cnf=/home/yuzihao/.my.cnf \
  --web.listen-address=:9104
```

### Q4: Grafana 无法连接 Prometheus

**问题**：Grafana 添加数据源时测试失败

**排查步骤**：

```bash
# 检查 Prometheus 是否运行
curl http://localhost:9090/-/healthy

# 检查防火墙
sudo ufw status

# 检查网络
curl http://localhost:9090/api/v1/status/config
```

### Q5: 告警不触发

**问题**：配置了告警规则，但没有收到告警

**排查步骤**：

```bash
# 检查告警规则是否加载
curl http://localhost:9090/api/v1/rules

# 检查 AlertManager 是否运行
curl http://localhost:9093/-/healthy

# 查看告警状态
curl http://localhost:9090/api/v1/alerts
```

---

## 维护指南

### 查看服务日志

```bash
# Prometheus 日志
journalctl -u prometheus -f

# Grafana 日志
journalctl -u grafana -f

# AlertManager 日志
journalctl -u alertmanager -f

# Node Exporter 日志
journalctl -u node_exporter -f

# MySQL Exporter 日志
journalctl -u mysqld_exporter -f

# Redis Exporter 日志
journalctl -u redis_exporter -f
```

### 重启服务

```bash
sudo systemctl restart prometheus
sudo systemctl restart grafana
sudo systemctl restart alertmanager
sudo systemctl restart node_exporter
sudo systemctl restart mysqld_exporter
sudo systemctl restart redis_exporter
```

### 停止服务

```bash
sudo systemctl stop prometheus
sudo systemctl stop grafana
sudo systemctl stop alertmanager
sudo systemctl stop node_exporter
sudo systemctl stop mysqld_exporter
sudo systemctl stop redis_exporter
```

### 卸载监控系统

```bash
# 停止所有服务
sudo systemctl stop prometheus grafana alertmanager node_exporter mysqld_exporter redis_exporter

# 禁用开机自启
sudo systemctl disable prometheus grafana alertmanager node_exporter mysqld_exporter redis_exporter

# 删除服务文件
sudo rm /etc/systemd/system/prometheus.service
sudo rm /etc/systemd/system/grafana.service
sudo rm /etc/systemd/system/alertmanager.service
sudo rm /etc/systemd/system/node_exporter.service
sudo rm /etc/systemd/system/mysqld_exporter.service
sudo rm /etc/systemd/system/redis_exporter.service

# 重新加载 systemd
sudo systemctl daemon-reload

# 删除数据（谨慎操作）
sudo rm -rf /mnt/d/his-agent-monitoring
```

### 备份配置

```bash
# 备份配置文件
tar -czf monitoring-config-backup-$(date +%Y%m%d).tar.gz \
  /mnt/d/his-agent-monitoring/prometheus/prometheus.yml \
  /mnt/d/his-agent-monitoring/alertmanager/alertmanager.yml \
  /home/yuzihao/.my.cnf

# 备份数据（Prometheus 指标数据）
sudo cp -r /mnt/d/his-agent-monitoring/data /backup/monitoring-data-$(date +%Y%m%d)
```

---

## 附录

### 完整端口列表

| 组件 | 端口 | 协议 | 说明 |
|------|------|------|------|
| Prometheus | 9090 | HTTP | 监控系统主服务 |
| Grafana | 3000 | HTTP | 可视化平台 |
| AlertManager | 9093 | HTTP | 告警管理 |
| Node Exporter | 9100 | HTTP | 系统指标 |
| MySQL Exporter | 9104 | HTTP | MySQL 指标 |
| Redis Exporter | 9121 | HTTP | Redis 指标 |
| MySQL | 3306 | MySQL | 数据库服务 |
| Redis | 6379 | Redis | 缓存服务 |

### 默认账号密码

| 组件 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| Grafana | admin | admin | 首次登录需修改密码 |
| Prometheus | - | - | 无认证 |
| AlertManager | - | - | 无认证 |

### 告警级别

| 级别 | 响应时间 | 说明 |
|------|----------|------|
| P0 | 立即 | 服务不可用、CPU/内存 > 90% |
| P1 | 30 分钟 | 性能下降、响应时间过长 |
| P2 | 24 小时 | 趋势问题、需要优化 |

---

## 技术支持

- 项目文档：`/home/yuzihao/workspace/his_agent/docs/`
- 监控配置：`/home/yuzihao/workspace/his_agent/monitoring/`
- 问题反馈：联系运维团队

---

**文档版本**: 2.0  
**最后更新**: 2026-03-16
