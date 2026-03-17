#!/bin/bash
# =====================================================
# HIS Agent 全栈启动脚本
# 启动所有服务：MySQL, Redis, Node Exporter, MySQL Exporter, Redis Exporter, Backend, Frontend
# =====================================================

set -e

echo "=========================================="
echo "🏥 HIS Agent 全栈启动脚本"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查 Java
echo -n "检查 Java 环境... "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    echo -e "${GREEN}✓ $JAVA_VERSION${NC}"
else
    echo -e "${RED}✗ Java 未安装${NC}"
    exit 1
fi

# 检查 Node.js
echo -n "检查 Node.js 环境... "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v)
    echo -e "${GREEN}✓ $NODE_VERSION${NC}"
else
    echo -e "${RED}✗ Node.js 未安装${NC}"
    exit 1
fi

# 启动 MySQL
echo -n "启动 MySQL 服务... "
echo "YUzh@1985223" | sudo -S service mysql start 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 启动成功${NC}"
else
    echo -e "${YELLOW}⚠️  启动失败或已运行${NC}"
fi

# 启动 Redis
echo -n "启动 Redis 服务... "
echo "YUzh@1985223" | sudo -S service redis-server start 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 启动成功${NC}"
else
    echo -e "${YELLOW}⚠️  启动失败或已运行${NC}"
fi

# 检查并启动 Node Exporter
echo -n "启动 Node Exporter... "
if pgrep -f "node_exporter" > /dev/null; then
    echo -e "${YELLOW}⚠️  已运行${NC}"
else
    if [ -f "/tmp/prometheus-2.50.1.linux-amd64/node_exporter" ]; then
        /tmp/prometheus-2.50.1.linux-amd64/node_exporter &
        sleep 2
        echo -e "${GREEN}✓ 启动成功${NC}"
    else
        echo -e "${YELLOW}⚠️  未找到二进制文件${NC}"
    fi
fi

# 检查并启动 MySQL Exporter
echo -n "启动 MySQL Exporter... "
if pgrep -f "mysqld_exporter" > /dev/null; then
    echo -e "${YELLOW}⚠️  已运行${NC}"
else
    if [ -f "/tmp/mysqld_exporter-0.15.1.linux-amd64/mysqld_exporter" ]; then
        /tmp/mysqld_exporter-0.15.1.linux-amd64/mysqld_exporter --config.my-cnf=/home/yuzihao/.my.cnf --web.listen-address=":9104" &
        sleep 2
        echo -e "${GREEN}✓ 启动成功${NC}"
    else
        echo -e "${YELLOW}⚠️  未找到二进制文件${NC}"
    fi
fi

# 检查并启动 Redis Exporter
echo -n "启动 Redis Exporter... "
if pgrep -f "redis_exporter" > /dev/null; then
    echo -e "${YELLOW}⚠️  已运行${NC}"
else
    if [ -f "/tmp/redis_exporter-v1.55.0.linux-amd64/redis_exporter" ]; then
        /tmp/redis_exporter-v1.55.0.linux-amd64/redis_exporter -redis.addr=localhost:6379 -web.listen-address=":9121" &
        sleep 2
        echo -e "${GREEN}✓ 启动成功${NC}"
    else
        echo -e "${YELLOW}⚠️  未找到二进制文件${NC}"
    fi
fi

# 检查并启动 Prometheus
echo -n "启动 Prometheus... "
if pgrep -f "prometheus.*9090" > /dev/null; then
    echo -e "${YELLOW}⚠️  已运行${NC}"
else
    if [ -f "/tmp/prometheus-2.50.1.linux-amd64/prometheus" ]; then
        /tmp/prometheus-2.50.1.linux-amd64/prometheus --config.file=/home/yuzihao/workspace/his_agent/prometheus.yml --storage.tsdb.path=/tmp/prometheus-data --web.listen-address=":9090" --web.enable-lifecycle &
        sleep 5
        echo -e "${GREEN}✓ 启动成功${NC}"
    else
        echo -e "${YELLOW}⚠️  未找到二进制文件${NC}"
    fi
fi

# 启动后端
echo -n "启动后端服务... "
if pgrep -f "HisAgentApplication" > /dev/null; then
    echo -e "${YELLOW}⚠️  已运行${NC}"
else
    cd /home/yuzihao/workspace/his_agent/his_agent-backend
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    mvn spring-boot:run -DskipTests -Dcheckstyle.skip=true > /tmp/backend-all.log 2>&1 &
    sleep 10
    echo -e "${GREEN}✓ 启动中${NC}"
fi

# 启动前端
echo -n "启动前端服务... "
if pgrep -f "vite" > /dev/null; then
    echo -e "${YELLOW}⚠️  已运行${NC}"
else
    cd /home/yuzihao/workspace/his_agent/his_agent-frontend
    npm run dev > /tmp/frontend-all.log 2>&1 &
    sleep 5
    echo -e "${GREEN}✓ 启动中${NC}"
fi

echo ""
echo "=========================================="
echo "🎉 所有服务启动完成！"
echo "=========================================="
echo ""
echo "📊 服务状态检查："
echo ""

# 检查服务状态
echo -n "MySQL: "
if mysql -u root -p'YUzh@1985223' -e "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${RED}✗ 停止${NC}"
fi

echo -n "Redis: "
if redis-cli ping > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中 (PONG)${NC}"
else
    echo -e "${RED}✗ 停止${NC}"
fi

echo -n "Node Exporter: "
if curl -s http://localhost:9100/metrics > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${RED}✗ 停止${NC}"
fi

echo -n "MySQL Exporter: "
if curl -s http://localhost:9104/metrics > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${RED}✗ 停止${NC}"
fi

echo -n "Redis Exporter: "
if curl -s http://localhost:9121/metrics > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${RED}✗ 停止${NC}"
fi

echo -n "后端: "
if curl -s http://localhost:8080/api/patients/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${RED}✗ 停止${NC}"
fi

echo -n "前端: "
if curl -s http://localhost:3001 > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 运行中${NC}"
else
    echo -e "${RED}✗ 停止${NC}"
fi

echo ""
echo "🔗 访问地址："
echo "   前端页面: http://localhost:3001"
echo "   后端 API:  http://localhost:8080"
echo "   监控面板: http://localhost:9090"
echo "   Node 指标: http://localhost:9100/metrics"
echo "   MySQL 指标: http://localhost:9104/metrics"
echo "   Redis 指标: http://localhost:9121/metrics"
echo ""
echo "🔧 停止服务：pkill -f 'HisAgentApplication|vite|exporter'"
echo "=========================================="
