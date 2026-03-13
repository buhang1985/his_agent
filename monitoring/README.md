# 监控系统安装指南

**版本**: 1.0  
**日期**: 2026-03-12  
**安装位置**: `/mnt/d/his-agent-monitoring`

---

## 1. 快速安装（推荐）

### 1.1 运行自动安装脚本

```bash
cd /home/yuzihao/workspace/his_agent/monitoring
chmod +x install-monitoring.sh
./install-monitoring.sh
```

### 1.2 验证安装

```bash
# 检查服务状态
systemctl status prometheus
systemctl status grafana
systemctl status alertmanager
systemctl status node_exporter
```

### 1.3 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Prometheus | http://localhost:9090 | 监控数据查询 |
| Grafana | http://localhost:3000 | 仪表板（admin/admin） |
| AlertManager | http://localhost:9093 | 告警管理 |
| Node Exporter | http://localhost:9100/metrics | 系统指标 |

---

## 2. 手动安装（如果自动安装失败）

### 2.1 下载组件（使用国内镜像）

```bash
INSTALL_DIR="/mnt/d/his-agent-monitoring"
mkdir -p $INSTALL_DIR/{prometheus,grafana,alertmanager,node_exporter,data}
cd $INSTALL_DIR

# Prometheus
wget https://mirror.tuna.tsinghua.edu.cn/github-release/prometheus/prometheus/releases/download/v2.50.1/prometheus-2.50.1.linux-amd64.tar.gz
tar xzf prometheus-2.50.1.linux-amd64.tar.gz -C prometheus/ --strip-components=1

# Grafana
wget https://dl.grafana.com/oss/release/grafana-10.3.3.linux-amd64.tar.gz
tar xzf grafana-10.3.3.linux-amd64.tar.gz -C grafana/ --strip-components=1

# AlertManager
wget https://mirror.tuna.tsinghua.edu.cn/github-release/prometheus/alertmanager/releases/download/v0.26.0/alertmanager-0.26.0.linux-amd64.tar.gz
tar xzf alertmanager-0.26.0.linux-amd64.tar.gz -C alertmanager/ --strip-components=1

# Node Exporter
wget https://mirror.tuna.tsinghua.edu.cn/github-release/prometheus/node_exporter/releases/download/v1.7.0/node_exporter-1.7.0.linux-amd64.tar.gz
tar xzf node_exporter-1.7.0.linux-amd64.tar.gz -C node_exporter/ --strip-components=1
```

### 2.2 复制配置文件

```bash
cd /home/yuzihao/workspace/his_agent/monitoring
cp prometheus/prometheus.yml /mnt/d/his-agent-monitoring/prometheus/
cp -r prometheus/alerts /mnt/d/his-agent-monitoring/prometheus/
cp alertmanager/alertmanager.yml /mnt/d/his-agent-monitoring/alertmanager/
```

### 2.3 创建 systemd 服务

参考 `install-monitoring.sh` 脚本中的 systemd 配置部分。

### 2.4 启动服务

```bash
sudo systemctl daemon-reload
sudo systemctl enable prometheus grafana alertmanager node_exporter
sudo systemctl start prometheus grafana alertmanager node_exporter
```

---

## 3. 配置说明

### 3.1 Prometheus 配置

位置：`monitoring/prometheus/prometheus.yml`

- 抓取间隔：15 秒
- 数据保留：15 天
- 监控目标：
  - HIS Agent 应用（端口 8080）
  - Node Exporter（端口 9100）
  - Redis（端口 6379）
  - MySQL（端口 3306）

### 3.2 告警规则

位置：`monitoring/prometheus/alerts/`

- **P0 告警**（立即响应）：CPU>90%、内存>90%、错误率>10%、熔断器打开
- **P1 告警**（30 分钟内）：CPU>70%、内存>80%、P99>5s、缓存命中率<50%
- **P2 告警**（24 小时内）：CPU>50%、磁盘>80%、GC 频繁

### 3.3 AlertManager 配置

位置：`monitoring/alertmanager/alertmanager.yml`

- P0 告警：邮件 + Webhook，5 分钟重复
- P1 告警：邮件 + Webhook，30 分钟重复
- P2 告警：邮件，4 小时重复

---

## 4. Grafana 配置

### 4.1 登录 Grafana

- 地址：http://localhost:3000
- 用户名：admin
- 密码：admin（首次登录会要求修改）

### 4.2 导入仪表板

推荐导入以下官方仪表板：

1. **JVM 监控**: ID 3268
2. **Spring Boot 监控**: ID 10280
3. **Node Exporter Full**: ID 1860

### 4.3 数据源配置

数据源已通过 `monitoring/grafana/provisioning/datasources/prometheus.yml` 自动配置：
- 名称：Prometheus
- URL: http://localhost:9090

---

## 5. 验证监控

### 5.1 检查 Prometheus 目标

访问：http://localhost:9090/targets

所有目标状态应为 **UP**。

### 5.2 查询指标

Prometheus Query:
```
# CPU 使用率
rate(process_cpu_seconds_total{job="his-agent"}[5m])

# 内存使用率
jvm_memory_used_bytes / jvm_memory_max_bytes

# HTTP 请求速率
rate(http_server_requests_seconds_count{job="his-agent"}[1m])

# 数据库连接池
hikaricp_connections_active{job="his-agent"}
```

### 5.3 测试告警

```bash
# 模拟服务宕机
sudo systemctl stop prometheus

# 等待 1 分钟后，AlertManager 应收到告警
curl http://localhost:9093/api/v2/alerts
```

---

## 6. 常见问题

### Q: Prometheus 启动失败

```bash
# 查看日志
journalctl -u prometheus -f

# 检查端口占用
sudo lsof -i :9090

# 检查配置文件
/mnt/d/his-agent-monitoring/prometheus/prometheus --config.file=/mnt/d/his-agent-monitoring/prometheus/prometheus.yml --config.check
```

### Q: Grafana 无法连接 Prometheus

1. 确认 Prometheus 已启动
2. 检查数据源配置：Grafana → Configuration → Data sources → Prometheus
3. 测试连接：Click "Save & test"

### Q: 告警不触发

1. 检查告警规则：Prometheus → Alerts
2. 检查 AlertManager 状态：http://localhost:9093/#/status
3. 查看 AlertManager 日志：`journalctl -u alertmanager -f`

---

## 7. 监控指标说明

### 7.1 应用指标

| 指标名称 | 说明 | 告警阈值 |
|----------|------|----------|
| `process_cpu_seconds_total` | CPU 使用率 | >90% P0 |
| `jvm_memory_used_bytes` | 内存使用率 | >90% P0 |
| `http_server_requests_seconds_count` | HTTP 请求计数 | - |
| `http_server_requests_seconds_bucket` | HTTP 响应时间 | P99>5s P1 |

### 7.2 数据库指标

| 指标名称 | 说明 | 告警阈值 |
|----------|------|----------|
| `hikaricp_connections_active` | 活跃连接数 | >95% P0 |
| `hikaricp_connection_timeout_total` | 连接超时 | >1/10m P2 |

### 7.3 熔断器指标

| 指标名称 | 说明 | 告警阈值 |
|----------|------|----------|
| `resilience4j_circuitbreaker_state` | 熔断器状态 | OPEN P0 |

---

## 8. 下一步

1. **启动 HIS Agent 应用**，确保 Actuator 端点暴露
2. **配置告警通知**，集成钉钉/企业微信
3. **创建业务仪表板**，监控问诊、患者等业务指标
4. **配置日志聚合**，集成 Loki 或 ELK

---

## 9. 相关文件

- 安装脚本：`monitoring/install-monitoring.sh`
- Prometheus 配置：`monitoring/prometheus/prometheus.yml`
- 告警规则：`monitoring/prometheus/alerts/*.yml`
- AlertManager 配置：`monitoring/alertmanager/alertmanager.yml`
- Grafana 数据源：`monitoring/grafana/provisioning/datasources/prometheus.yml`
