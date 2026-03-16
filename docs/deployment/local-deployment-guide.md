# HIS Agent 本地化部署指南

**版本**: 1.0  
**日期**: 2026-03-13  
**适用**: 运维工程师、系统管理员

---

## 1. 系统要求

### 硬件要求
- CPU: 4 核心以上
- 内存：8GB 以上
- 磁盘：50GB 可用空间
- 网络：100Mbps 以上

### 软件要求
- 操作系统：Ubuntu 24.04 LTS
- Java: OpenJDK 17+
- Node.js: 20+
- MySQL: 8.0+
- Redis: 7.0+

---

## 2. 安装步骤

### 2.1 安装 Java 17

```bash
# 安装 OpenJDK 17
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk

# 验证安装
java -version
# 应显示：openjdk version "17.x.x"

# 配置 JAVA_HOME
echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> ~/.bashrc
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
source ~/.bashrc
```

### 2.2 安装 Node.js 20

```bash
# 使用 NodeSource 仓库
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# 验证安装
node -v  # 应显示：v20.x.x
npm -v   # 应显示：10.x.x
```

### 2.3 安装 MySQL 8

```bash
# 安装 MySQL 服务器和客户端
sudo apt-get install -y mysql-server mysql-client

# 启动 MySQL 服务
sudo service mysql start

# 验证 MySQL 版本
mysql --version
# 应显示：mysql  Ver 8.0.x
```

#### 配置 MySQL 用户和数据库

```bash
# 以 root 用户登录 MySQL
sudo mysql -u root

# 在 MySQL 命令行中执行：
mysql> -- 创建专用用户（使用 mysql_native_password 认证）
mysql> CREATE USER 'his_agent'@'localhost' 
       IDENTIFIED WITH mysql_native_password BY 'HisAgent@2026';

mysql> -- 授予权限
mysql> GRANT ALL PRIVILEGES ON *.* TO 'his_agent'@'localhost' WITH GRANT OPTION;
mysql> FLUSH PRIVILEGES;

mysql> -- 创建数据库
mysql> CREATE DATABASE his_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

mysql> -- 验证
mysql> SELECT User, Host, plugin FROM mysql.user WHERE User='his_agent';
mysql> SHOW DATABASES;

mysql> -- 退出
mysql> EXIT;
```

#### 测试 MySQL 连接

```bash
# 使用新用户连接
mysql -u his_agent -p'HisAgent@2026' -e "SELECT 'Connection OK!' AS status;"
```

### 2.4 安装 Redis 7

```bash
# 安装 Redis 服务器
sudo apt-get install -y redis-server

# 启动 Redis 服务
sudo service redis-server start

# 验证 Redis 版本
redis-cli --version
# 应显示：redis-cli 7.x.x

# 测试 Redis 连接
redis-cli ping
# 应返回：PONG
```

---

## 3. 部署应用

### 3.1 克隆代码

```bash
# 克隆项目
cd /opt
sudo git clone https://github.com/buhang1985/his_agent.git
sudo chown -R $USER:$USER his_agent
cd his_agent
```

### 3.2 配置后端

```bash
# 编辑后端配置文件
cd /opt/his_agent/his_agent-backend/src/main/resources
vi application.yml

# 确认数据库配置：
# spring:
#   datasource:
#     url: jdbc:mysql://localhost:3306/his_agent?...
#     username: his_agent
#     password: HisAgent@2026
#     driver-class-name: com.mysql.cj.jdbc.Driver
```

### 3.3 编译后端

```bash
cd /opt/his_agent/his_agent-backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# 编译项目
mvn clean package -DskipTests -Dcheckstyle.skip=true

# 编译成功后会生成 jar 文件
ls -lh target/*.jar
```

### 3.4 启动后端

```bash
# 方式 1：使用 Maven（开发环境）
nohup mvn spring-boot:run -DskipTests > /var/log/his-agent-backend.log 2>&1 &

# 方式 2：使用 jar 包（生产环境）
nohup java -jar target/his-agent-backend-0.1.0-SNAPSHOT.jar \
  -Dspring.profiles.active=prod \
  > /var/log/his-agent-backend.log 2>&1 &

# 等待启动（约 15 秒）
sleep 15

# 验证后端启动
tail -100 /var/log/his-agent-backend.log | grep "Started"
# 应显示：Started HisAgentApplication in xx.xxx seconds

# 测试后端健康检查
curl http://localhost:8080/actuator/health
# 应返回：{"status":"UP"}
```

### 3.5 部署前端

```bash
cd /opt/his_agent/his_agent-frontend

# 安装依赖
npm ci

# 编译生产版本
npm run build

# 启动开发服务器（开发环境）
nohup npm run dev > /var/log/his-agent-frontend.log 2>&1 &

# 或使用 Nginx 部署生产版本（生产环境）
# 配置 Nginx 指向 dist/ 目录
```

### 3.6 验证服务

```bash
# 检查所有服务状态
echo "=== 后端服务 ==="
ps aux | grep HisAgentApplication | grep -v grep

echo ""
echo "=== 前端服务 ==="
ps aux | grep vite | grep -v grep

echo ""
echo "=== MySQL 服务 ==="
sudo service mysql status | head -3

echo ""
echo "=== Redis 服务 ==="
redis-cli ping

echo ""
echo "=== API 测试 ==="
curl -s http://localhost:8080/api/patients/health | python3 -m json.tool

echo ""
echo "=== 前端测试 ==="
curl -s http://localhost:3001 -o /dev/null -w "HTTP 状态码：%{http_code}\n"
```

---

## 4. Prometheus 监控配置

### 4.1 验证监控端点

```bash
# 检查 Actuator 端点
curl http://localhost:8080/actuator

# 检查 Prometheus 指标
curl http://localhost:8080/actuator/prometheus | head -20
```

### 4.2 关键监控指标

| 指标 | 说明 | 正常值 |
|------|------|--------|
| `up{job="his-agent"}` | 服务状态 | 1 |
| `hikaricp_connections_active` | 活跃数据库连接 | < 15 |
| `hikaricp_connections_idle` | 空闲数据库连接 | > 2 |
| `redis_connected` | Redis 连接状态 | 1 |
| `jvm_memory_used_bytes` | JVM 内存使用 | < 80% |

### 4.3 配置 Prometheus Server

```yaml
# /etc/prometheus/prometheus.yml
scrape_configs:
  - job_name: 'his-agent'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          environment: 'production'
```

---

## 5. 服务管理

### 5.1 创建 systemd 服务

#### 后端服务

```bash
sudo vi /etc/systemd/system/his-agent-backend.service
```

```ini
[Unit]
Description=HIS Agent Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=his-agent
WorkingDirectory=/opt/his_agent/his_agent-backend
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
ExecStart=/usr/bin/java -jar target/his-agent-backend-0.1.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

#### 前端服务

```bash
sudo vi /etc/systemd/system/his-agent-frontend.service
```

```ini
[Unit]
Description=HIS Agent Frontend Service
After=network.target

[Service]
Type=simple
User=his-agent
WorkingDirectory=/opt/his_agent/his_agent-frontend
ExecStart=/usr/bin/npm run dev
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 5.2 服务管理命令

```bash
# 重新加载 systemd 配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start his-agent-backend
sudo systemctl start his-agent-frontend

# 设置开机自启
sudo systemctl enable his-agent-backend
sudo systemctl enable his-agent-frontend

# 查看服务状态
sudo systemctl status his-agent-backend
sudo systemctl status his-agent-frontend

# 查看日志
sudo journalctl -u his-agent-backend -f
sudo journalctl -u his-agent-frontend -f

# 重启服务
sudo systemctl restart his-agent-backend

# 停止服务
sudo systemctl stop his-agent-backend
```

---

## 6. 故障排查

### 6.1 后端启动失败

```bash
# 检查日志
tail -200 /var/log/his-agent-backend.log

# 检查端口占用
lsof -i :8080

# 检查数据库连接
mysql -u his_agent -p'HisAgent@2026' -e "SELECT 1;"

# 检查 Redis 连接
redis-cli ping
```

### 6.2 MySQL 连接问题

```bash
# 检查 MySQL 服务状态
sudo service mysql status

# 检查用户权限
mysql -u root -e "SELECT User, Host, plugin FROM mysql.user WHERE User='his_agent';"

# 重新创建用户
sudo mysql -u root <<EOF
DROP USER IF EXISTS 'his_agent'@'localhost';
CREATE USER 'his_agent'@'localhost' IDENTIFIED WITH mysql_native_password BY 'HisAgent@2026';
GRANT ALL PRIVILEGES ON *.* TO 'his_agent'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EOF
```

### 6.3 Redis 连接问题

```bash
# 检查 Redis 服务状态
sudo service redis-server status

# 重启 Redis
sudo service redis-server restart

# 检查 Redis 配置
cat /etc/redis/redis.conf | grep -E "bind|port"
```

### 6.4 前端无法访问

```bash
# 检查前端进程
ps aux | grep vite

# 检查前端日志
tail -100 /var/log/his-agent-frontend.log

# 检查端口
lsof -i :3001

# 重启前端
pkill -f vite
cd /opt/his_agent/his_agent-frontend && npm run dev &
```

---

## 7. 性能优化

### 7.1 MySQL 优化

```sql
-- 在 MySQL 中执行
SET GLOBAL max_connections = 200;
SET GLOBAL innodb_buffer_pool_size = 2G;
```

### 7.2 JVM 优化

```bash
# 编辑启动脚本，添加 JVM 参数
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"
```

### 7.3 Redis 优化

```bash
# 编辑 Redis 配置
sudo vi /etc/redis/redis.conf

# 修改以下参数：
maxmemory 2gb
maxmemory-policy allkeys-lru
```

---

## 8. 备份策略

### 8.1 MySQL 备份

```bash
# 创建备份脚本
cat > /opt/his-agent/backup-mysql.sh <<'EOF'
#!/bin/bash
BACKUP_DIR="/opt/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

mysqldump -u his_agent -p'HisAgent@2026' his_agent > $BACKUP_DIR/his_agent_$DATE.sql
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
EOF

chmod +x /opt/his-agent/backup-mysql.sh

# 添加定时任务
crontab -e
# 每天凌晨 2 点备份
0 2 * * * /opt/his-agent/backup-mysql.sh
```

### 8.2 应用备份

```bash
# 备份应用目录
tar -czf /opt/backups/his-agent-app-$(date +%Y%m%d).tar.gz \
  /opt/his_agent/his_agent-backend/target/*.jar
```

---

## 9. 监控告警

### 9.1 Prometheus 告警规则

```yaml
# /etc/prometheus/alerts.yml
groups:
  - name: his-agent
    rules:
      - alert: BackendDown
        expr: up{job="his-agent"} == 0
        for: 1m
        annotations:
          summary: "HIS Agent 后端宕机"
      
      - alert: DatabaseConnectionHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        annotations:
          summary: "数据库连接池使用率超过 90%"
```

### 9.2 Grafana 仪表板

导入以下官方仪表板：
- JVM 监控：ID 3268
- Spring Boot 监控：ID 10280
- MySQL 监控：ID 7362
- Redis 监控：ID 11835

---

## 10. 联系支持

**技术支持**: support@his-agent.local  
**文档**: /opt/his_agent/docs/  
**日志目录**: /var/log/his-agent-*.log

---

## 附录：快速检查清单

- [ ] Java 17 已安装
- [ ] Node.js 20 已安装
- [ ] MySQL 8 已安装并配置用户
- [ ] Redis 7 已安装并运行
- [ ] 后端编译成功
- [ ] 后端启动成功（端口 8080）
- [ ] 前端启动成功（端口 3001）
- [ ] 健康检查通过（/actuator/health）
- [ ] Prometheus 指标可访问（/actuator/prometheus）
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] systemd 服务已配置
- [ ] 备份脚本已配置
- [ ] 监控告警已配置

---

**部署完成！** 🎉
