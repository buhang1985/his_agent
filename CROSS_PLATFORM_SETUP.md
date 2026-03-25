# 跨平台环境配置指南

## 📋 环境差异

### Mac 环境 (开发机 1)
- **操作系统**: macOS
- **架构**: ARM64 (M 系列芯片) 或 Intel
- **包管理**: Homebrew
- **Docker**: Docker Desktop for Mac
- **网络**: localhost 访问

### Windows 环境 (开发机 2)
- **操作系统**: Windows 10/11
- **架构**: x86_64
- **包管理**: Chocolatey / Winget
- **Docker**: Docker Desktop for Windows (WSL2)
- **网络**: localhost 访问 (通过 WSL2)

---

## 🗄️ 数据库配置

### MySQL

#### Mac 环境
```bash
# 使用 Homebrew 安装
brew install mysql@8.0

# 启动服务
brew services start mysql@8.0

# 连接配置
mysql -h localhost -P 3306 -u root -p
```

#### Windows 环境
```powershell
# 使用 Chocolatey 安装
choco install mysql

# 或使用 Windows Installer
# 下载地址：https://dev.mysql.com/downloads/mysql/

# 启动服务
net start MySQL80

# 连接配置
mysql -h localhost -P 3306 -u root -p
```

#### Docker 方式 (跨平台统一)
```bash
# Mac & Windows 通用
docker run -d \
  --name mysql-his \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=his_agent \
  -v mysql-data:/var/lib/mysql \
  mysql:8.0
```

---

## 🔴 Redis 配置

### Mac 环境
```bash
# 使用 Homebrew 安装
brew install redis

# 启动服务
brew services start redis

# 验证连接
redis-cli ping  # 应返回 PONG

# 配置文件位置
# /usr/local/etc/redis.conf (Intel)
# /opt/homebrew/etc/redis.conf (ARM)
```

#### application-mac.yml
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### Windows 环境

#### 方式 1: 使用 WSL2 (推荐)
```bash
# 在 WSL2 中安装
sudo apt update
sudo apt install redis-server

# 启动服务
sudo service redis-server start

# 验证连接
redis-cli ping
```

#### 方式 2: 使用 Docker (推荐)
```powershell
# Windows PowerShell
docker run -d `
  --name redis-his `
  -p 6379:6379 `
  -v redis-data:/data `
  redis:7-alpine
```

#### 方式 3: 使用 Windows 移植版 (不推荐)
- 项目已停止维护
- 下载地址：https://github.com/microsoftarchive/redis/releases
- 仅用于测试，不建议生产使用

#### application-windows.yml
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

---

## 📊 Prometheus 配置

### Mac 环境
```bash
# 使用 Homebrew 安装
brew install prometheus

# 启动服务
brew services start prometheus

# 配置文件位置
# /usr/local/etc/prometheus.yml

# 访问 UI
# http://localhost:9090
```

### Windows 环境
```powershell
# 使用 Chocolatey 安装
choco install prometheus

# 或下载二进制文件
# https://prometheus.io/download/

# 启动服务
prometheus.exe --config.file=prometheus.yml

# 访问 UI
# http://localhost:9090
```

### Docker 方式 (跨平台统一)
```bash
docker run -d \
  --name prometheus-his \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v prometheus-data:/prometheus \
  prom/prometheus
```

#### prometheus.yml (通用)
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'his-agent-backend'
    static_configs:
      - targets: ['host.docker.internal:8080']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
      - targets: ['localhost:8080']
```

---

## 📈 Grafana 配置

### Mac 环境
```bash
# 使用 Homebrew 安装
brew install grafana

# 启动服务
brew services start grafana

# 访问 UI
# http://localhost:3000
# 默认账号：admin / admin

# 数据文件位置
# /usr/local/var/lib/grafana
```

### Windows 环境
```powershell
# 使用 Chocolatey 安装
choco install grafana

# 启动服务
net start grafana

# 访问 UI
# http://localhost:3000
# 默认账号：admin / admin

# 数据文件位置
# C:\Program Files\GrafanaLabs\grafana\data
```

### Docker 方式 (跨平台统一)
```bash
docker run -d \
  --name grafana-his \
  -p 3000:3000 \
  -v grafana-data:/var/lib/grafana \
  -e GF_SECURITY_ADMIN_PASSWORD=admin123 \
  grafana/grafana-oss
```

#### 添加 Prometheus 数据源
1. 访问 http://localhost:3000
2. 登录：admin / admin123
3. Configuration → Data Sources → Add data source
4. 选择 Prometheus
5. URL: `http://host.docker.internal:9090` (Docker)
   或 `http://localhost:9090` (原生安装)
6. Save & Test

---

## 🔧 环境检测脚本

### Mac 检测脚本 (check-env-mac.sh)
```bash
#!/bin/bash

echo "=== Mac 环境检测 ==="

# 检查 MySQL
if command -v mysql &> /dev/null; then
    echo "✅ MySQL 已安装"
    mysql --version
else
    echo "❌ MySQL 未安装"
fi

# 检查 Redis
if command -v redis-cli &> /dev/null; then
    echo "✅ Redis 已安装"
    redis-cli ping
else
    echo "❌ Redis 未安装"
fi

# 检查 Docker
if command -v docker &> /dev/null; then
    echo "✅ Docker 已安装"
    docker --version
else
    echo "❌ Docker 未安装"
fi

# 检查端口
echo ""
echo "=== 端口检测 ==="
lsof -i :3306 | grep LISTEN && echo "✅ MySQL 运行中" || echo "❌ MySQL 未运行"
lsof -i :6379 | grep LISTEN && echo "✅ Redis 运行中" || echo "❌ Redis 未运行"
lsof -i :8080 | grep LISTEN && echo "✅ 应用运行中" || echo "❌ 应用未运行"
lsof -i :9090 | grep LISTEN && echo "✅ Prometheus 运行中" || echo "❌ Prometheus 未运行"
lsof -i :3000 | grep LISTEN && echo "✅ Grafana 运行中" || echo "❌ Grafana 未运行"
```

### Windows 检测脚本 (check-env-windows.ps1)
```powershell
Write-Host "=== Windows 环境检测 ===" -ForegroundColor Cyan

# 检查 MySQL
if (Get-Command mysql -ErrorAction SilentlyContinue) {
    Write-Host "✅ MySQL 已安装" -ForegroundColor Green
    mysql --version
} else {
    Write-Host "❌ MySQL 未安装" -ForegroundColor Red
}

# 检查 Redis
if (Test-NetConnection -ComputerName localhost -Port 6379 -InformationLevel Quiet) {
    Write-Host "✅ Redis 已安装并运行" -ForegroundColor Green
} else {
    Write-Host "❌ Redis 未安装或未运行" -ForegroundColor Red
}

# 检查 Docker
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "✅ Docker 已安装" -ForegroundColor Green
    docker --version
} else {
    Write-Host "❌ Docker 未安装" -ForegroundColor Red
}

# 检查端口
Write-Host ""
Write-Host "=== 端口检测 ===" -ForegroundColor Cyan
$ports = @{
    "3306" = "MySQL"
    "6379" = "Redis"
    "8080" = "应用"
    "9090" = "Prometheus"
    "3000" = "Grafana"
}

foreach ($port in $ports.Keys) {
    if (Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet) {
        Write-Host "✅ $($ports[$port]) 运行中 (端口 $port)" -ForegroundColor Green
    } else {
        Write-Host "❌ $($ports[$port]) 未运行 (端口 $port)" -ForegroundColor Red
    }
}
```

---

## 🚀 一键启动脚本

### Mac 启动脚本 (start-mac.sh)
```bash
#!/bin/bash

echo "🚀 启动 Mac 开发环境..."

# 启动 MySQL
brew services start mysql@8.0
echo "✅ MySQL 已启动"

# 启动 Redis
brew services start redis
echo "✅ Redis 已启动"

# 启动后端
cd his_agent-backend
mvn spring-boot:run &
echo "✅ 后端启动中..."

# 启动前端
cd ../his_agent-frontend
npm run dev &
echo "✅ 前端启动中..."

echo ""
echo "🎉 所有服务已启动!"
echo "前端：http://localhost:5173"
echo "后端：http://localhost:8080"
echo "Grafana: http://localhost:3000"
echo "Prometheus: http://localhost:9090"
```

### Windows 启动脚本 (start-windows.ps1)
```powershell
Write-Host "🚀 启动 Windows 开发环境..." -ForegroundColor Cyan

# 启动 MySQL
net start MySQL80
Write-Host "✅ MySQL 已启动" -ForegroundColor Green

# 启动 Redis (如使用 Docker)
docker start redis-his
Write-Host "✅ Redis 已启动" -ForegroundColor Green

# 启动后端
cd his_agent-backend
Start-Process mvn -ArgumentList "spring-boot:run" -WindowStyle Normal
Write-Host "✅ 后端启动中..." -ForegroundColor Green

# 启动前端
cd ../his_agent-frontend
Start-Process npm -ArgumentList "run", "dev" -WindowStyle Normal
Write-Host "✅ 前端启动中..." -ForegroundColor Green

Write-Host ""
Write-Host "🎉 所有服务已启动!" -ForegroundColor Green
Write-Host "前端：http://localhost:5173"
Write-Host "后端：http://localhost:8080"
Write-Host "Grafana: http://localhost:3000"
Write-Host "Prometheus: http://localhost:9090"
```

---

## 📦 Docker Compose 方式 (推荐跨平台统一)

### docker-compose.yml
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: mysql-his
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: his_agent
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - his-network

  redis:
    image: redis:7-alpine
    container_name: redis-his
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - his-network

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus-his
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    networks:
      - his-network

  grafana:
    image: grafana/grafana-oss:latest
    container_name: grafana-his
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin123
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - his-network

volumes:
  mysql-data:
  redis-data:
  prometheus-data:
  grafana-data:

networks:
  his-network:
    driver: bridge
```

### 启动命令 (Mac & Windows 通用)
```bash
# 启动所有服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止所有服务
docker-compose down
```

---

## 🔐 安全配置

### 生产环境配置

#### application-prod.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:prod-db.example.com}:${DB_PORT:3306}/${DB_NAME:his_agent}?useSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USER:his_user}
    password: ${DB_PASSWORD:强密码}
    
  data:
    redis:
      host: ${REDIS_HOST:prod-redis.example.com}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:强密码}
      ssl: true
```

### 环境变量文件 (.env)
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=his_agent
DB_USER=root
DB_PASSWORD=root123

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 监控配置
PROMETHEUS_HOST=localhost
GRAFANA_HOST=localhost

# 应用配置
SERVER_PORT=8080
```

---

## 📝 注意事项

### Mac 特定问题

1. **M 系列芯片兼容性**
   - 使用 `--platform linux/amd64` 运行 x86 容器
   - 或使用 ARM 版本的镜像

2. **端口冲突**
   - macOS 可能占用某些端口
   - 使用 `lsof -i :端口` 检查

3. **文件权限**
   - Docker 挂载卷需要权限
   - 使用 `chmod` 调整权限

### Windows 特定问题

1. **WSL2 网络**
   - WSL2 使用虚拟网络
   - localhost 可能需要特殊配置

2. **防火墙**
   - Windows 防火墙可能阻止端口
   - 添加入站规则

3. **路径问题**
   - 使用正斜杠 `/` 或双反斜杠 `\\`
   - 避免中文路径

---

## 🎯 推荐方案

**最佳实践**: 使用 Docker Compose 统一跨平台环境

**优点**:
- ✅ Mac 和 Windows 配置完全一致
- ✅ 快速启动和停止
- ✅ 数据持久化
- ✅ 易于部署到生产环境
- ✅ 版本控制和可重复性

**开发环境**:
```bash
# 统一使用 Docker Compose
docker-compose up -d
```

**生产环境**:
- 使用云数据库 (RDS)
- 使用云 Redis (ElastiCache)
- 使用托管 Prometheus/Grafana

---

## 📚 参考资料

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [Redis 官方文档](https://redis.io/documentation)
- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [Docker Desktop for Mac](https://docs.docker.com/desktop/mac/)
- [Docker Desktop for Windows](https://docs.docker.com/desktop/windows/)
