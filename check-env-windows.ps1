#!/bin/bash

echo "======================================"
echo "     Windows 环境检测"
echo "======================================"
echo ""

# 检查 MySQL
if (Get-Command mysql -ErrorAction SilentlyContinue) {
    Write-Host "✅ MySQL 已安装" -ForegroundColor Green
    mysql --version
} else {
    Write-Host "❌ MySQL 未安装" -ForegroundColor Red
    Write-Host "   安装方法：choco install mysql"
}

# 检查 Redis
if (Test-NetConnection -ComputerName localhost -Port 6379 -InformationLevel Quiet) {
    Write-Host "✅ Redis 已安装并运行" -ForegroundColor Green
} else {
    Write-Host "❌ Redis 未安装或未运行" -ForegroundColor Red
    Write-Host "   安装方法：docker run -d -p 6379:6379 redis:7-alpine"
}

# 检查 Docker
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "✅ Docker 已安装" -ForegroundColor Green
    docker --version
} else {
    Write-Host "❌ Docker 未安装" -ForegroundColor Red
    Write-Host "   安装方法：winget install Docker.DockerDesktop"
}

# 检查 Java
if (Get-Command java -ErrorAction SilentlyContinue) {
    Write-Host "✅ Java 已安装" -ForegroundColor Green
    java -version
} else {
    Write-Host "❌ Java 未安装" -ForegroundColor Red
    Write-Host "   安装方法：winget install Oracle.JavaRuntimeEnvironment"
}

# 检查 Node.js
if (Get-Command node -ErrorAction SilentlyContinue) {
    Write-Host "✅ Node.js 已安装" -ForegroundColor Green
    node --version
} else {
    Write-Host "❌ Node.js 未安装" -ForegroundColor Red
    Write-Host "   安装方法：winget install OpenJS.NodeJS.LTS"
}

echo ""
echo "======================================"
echo "     端口检测"
echo "======================================"

$ports = @{
    "3306" = "MySQL"
    "6379" = "Redis"
    "8080" = "应用"
    "9090" = "Prometheus"
    "3000" = "Grafana"
    "5173" = "前端"
}

foreach ($port in $ports.Keys) {
    if (Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet) {
        Write-Host "✅ $($ports[$port]) 运行中 (端口 $port)" -ForegroundColor Green
    } else {
        Write-Host "❌ $($ports[$port]) 未运行 (端口 $port)" -ForegroundColor Red
    }
}

echo ""
echo "======================================"
echo "     Docker 容器状态"
echo "======================================"

if (Get-Command docker -ErrorAction SilentlyContinue) {
    docker ps --format "table {{.Names}}`t{{.Status}}`t{{.Ports}}"
} else {
    Write-Host "Docker 未安装" -ForegroundColor Red
}

echo ""
echo "======================================"
