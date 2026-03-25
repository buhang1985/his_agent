#!/bin/bash

echo "======================================"
echo "     Mac 环境检测"
echo "======================================"
echo ""

# 检查 MySQL
if command -v mysql &> /dev/null; then
    echo "✅ MySQL 已安装"
    mysql --version
else
    echo "❌ MySQL 未安装"
    echo "   安装方法：brew install mysql@8.0"
fi

# 检查 Redis
if command -v redis-cli &> /dev/null; then
    echo "✅ Redis 已安装"
    if redis-cli ping &> /dev/null; then
        echo "   状态：运行中"
    else
        echo "   状态：未运行 (brew services start redis)"
    fi
else
    echo "❌ Redis 未安装"
    echo "   安装方法：brew install redis"
fi

# 检查 Docker
if command -v docker &> /dev/null; then
    echo "✅ Docker 已安装"
    docker --version
else
    echo "❌ Docker 未安装"
    echo "   安装方法：brew install --cask docker"
fi

# 检查 Java
if command -v java &> /dev/null; then
    echo "✅ Java 已安装"
    java -version
else
    echo "❌ Java 未安装"
    echo "   安装方法：brew install openjdk@17"
fi

# 检查 Node.js
if command -v node &> /dev/null; then
    echo "✅ Node.js 已安装"
    node --version
else
    echo "❌ Node.js 未安装"
    echo "   安装方法：brew install node"
fi

echo ""
echo "======================================"
echo "     端口检测"
echo "======================================"

check_port() {
    local port=$1
    local name=$2
    if lsof -Pi :$port -sTCP:LISTEN -t &> /dev/null; then
        echo "✅ $name 运行中 (端口 $port)"
    else
        echo "❌ $name 未运行 (端口 $port)"
    fi
}

check_port 3306 "MySQL"
check_port 6379 "Redis"
check_port 8080 "应用"
check_port 9090 "Prometheus"
check_port 3000 "Grafana"
check_port 5173 "前端"

echo ""
echo "======================================"
echo "     Docker 容器状态"
echo "======================================"

if command -v docker &> /dev/null; then
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
    echo "Docker 未安装"
fi

echo ""
echo "======================================"
