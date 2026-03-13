# his_agent 部署指南

## 快速开始

### 测试期部署（单机）

```bash
cd /home/yuzihao/workspace/his_agent/deploy

# 1. 复制并编辑配置文件
cp config/.env.example config/.env
vim config/.env

# 2. 执行一键部署脚本
chmod +x scripts/deploy.sh
./scripts/deploy.sh

# 3. 验证部署
curl http://localhost:8080/actuator/health
```

### 生产期部署（双机高可用）

```bash
# Node 1 (Master)
cd /home/yuzihao/workspace/his_agent/deploy

# 1. 安装 Keepalived
sudo apt-get install keepalived

# 2. 配置 Keepalived
sudo cp keepalived/keepalived-master.conf /etc/keepalived/keepalived.conf
sudo cp keepalived/check_app.sh /usr/local/bin/check_app.sh
sudo chmod +x /usr/local/bin/check_app.sh

# 3. 启动 Keepalived
sudo systemctl enable keepalived
sudo systemctl start keepalived

# 4. 部署应用
./scripts/deploy.sh

# Node 2 (Backup) - 重复以上步骤，使用 backup 配置
sudo cp keepalived/keepalived-backup.conf /etc/keepalived/keepalived.conf
```

---

## 配置说明

### 必需配置项

编辑 `config/.env` 文件：

```bash
# 数据库配置
DB_HOST=192.168.1.50
DB_USER=hisagent
DB_PASSWORD=<强密码>

# Redis 配置
REDIS_PASSWORD=<强密码>

# 云端 AI 服务
XUNFEI_APP_ID=<讯飞 APP ID>
XUNFEI_API_KEY=<讯飞 API Key>
XUNFEI_API_SECRET=<讯飞 API Secret>
DASHSCOPE_API_KEY=<阿里云 DashScope Key>

# HIS 集成
HIS_TOKEN_PUBLIC_KEY=<HIS 公钥>
```

### 可选配置项

```bash
# 备份配置
BACKUP_MODE=both  # local | remote | both
REMOTE_BACKUP_ENABLED=true
REMOTE_HOST=backup.hospital.com

# 保留策略
RETENTION_DAYS=30
```

---

## 运维手册

### 查看日志

```bash
# 应用日志
docker-compose logs -f app

# 查看错误日志
docker-compose logs --tail=100 app | grep ERROR

# MySQL 日志
docker-compose logs mysql

# Redis 日志
docker-compose logs redis
```

### 服务管理

```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看服务状态
docker-compose ps

# 进入容器
docker-compose exec app bash
docker-compose exec mysql mysql -u root -p
docker-compose exec redis redis-cli
```

### 备份管理

```bash
# 手动备份
./scripts/backup.sh

# 查看备份文件
ls -lh /backup/his_agent/

# 恢复数据库
gunzip < /backup/his_agent/mysql/mysql_YYYYMMDD_HHMMSS.sql.gz | \
  docker-compose exec -T mysql mysql -u root -p$MYSQL_ROOT_PASSWORD
```

### 监控告警

访问 Grafana: `http://<监控服务器>:3000`

默认账号：`admin` / `admin`

关键 Dashboard：
- his_agent 应用监控
- MySQL 监控
- Redis 监控
- 服务器资源监控

---

## 故障排查

### 应用无法启动

```bash
# 1. 查看日志
docker-compose logs app

# 2. 检查依赖服务
docker-compose ps mysql redis

# 3. 验证数据库连接
docker-compose exec app curl http://mysql:3306

# 4. 检查内存
docker stats
```

### Keepalived VIP 不漂移

```bash
# 1. 检查 Keepalived 状态
systemctl status keepalived

# 2. 查看日志
journalctl -u keepalived -f

# 3. 验证健康检查脚本
/usr/local/bin/check_app.sh
echo $?

# 4. 检查 VRRP 广播
tcpdump -n vrrp
```

### 数据库主从不同步

```bash
# 1. 登录从库
mysql -u root -p

# 2. 查看同步状态
SHOW SLAVE STATUS\G

# 3. 如果 Slave_IO_Running 或 Slave_SQL_Running 为 No
STOP SLAVE;
START SLAVE;

# 4. 重新同步（极端情况）
# 在 Master 执行
mysqldump --single-transaction --master-data=2 his_agent | gzip > backup.sql.gz

# 在 Slave 执行
gunzip < backup.sql.gz | mysql his_agent
```

---

## 性能优化

### JVM 调优

编辑 `docker-compose.yml`：

```yaml
services:
  app:
    environment:
      - JAVA_OPTS=-Xmx2g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### MySQL 调优

编辑 `/etc/mysql/my.cnf`：

```ini
[mysqld]
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
max_connections = 500
query_cache_size = 128M
```

### Nginx 调优

编辑 `/etc/nginx/nginx.conf`：

```nginx
worker_processes auto;
worker_rlimit_nofile 65535;

events {
    worker_connections 8192;
    multi_accept on;
}

http {
    keepalive_timeout 65;
    keepalive_requests 1000;
}
```

---

## 安全加固

### 防火墙配置

```bash
# 允许出站 HTTPS（云端 AI 服务）
sudo ufw allow out 443/tcp

# 允许内网通信
sudo ufw allow from 192.168.1.0/24

# 禁止外部访问数据库
sudo ufw deny 3306/tcp
sudo ufw deny 6379/tcp

# 启用防火墙
sudo ufw enable
```

### SSH 加固

```bash
# 编辑 /etc/ssh/sshd_config
Port 2222
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
```

### 定期更新

```bash
# 系统更新
sudo apt-get update && sudo apt-get upgrade -y

# Docker 镜像更新
docker-compose pull
docker-compose up -d
```

---

## 常见问题

### Q: 如何重置管理员密码？

A: 直接修改数据库：

```sql
UPDATE users SET password_hash = '<新密码哈希>' WHERE username = 'admin';
```

### Q: 如何扩容磁盘？

A: 如果是 LVM：

```bash
lvextend -L +100G /dev/mapper/data
resize2fs /dev/mapper/data
```

### Q: 如何迁移到新服务器？

A: 使用备份恢复：

1. 在新服务器部署应用
2. 从备份恢复数据库
3. 复制配置文件
4. 验证并切换 VIP

---

## 联系支持

- 技术文档：`/home/yuzihao/workspace/his_agent/docs/`
- 架构文档：`docs/architecture/high-availability-architecture.md`
- 应急联系人：运维团队
