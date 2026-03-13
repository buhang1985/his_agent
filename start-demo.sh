#!/bin/bash
# =====================================================
# HIS Agent Demo 启动脚本
# 用途：一键启动前后端服务
# =====================================================

echo "=========================================="
echo "🏥 HIS Agent Demo 启动"
echo "=========================================="
echo ""

# 设置颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# 检查 Node
echo -n "检查 Node 环境... "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v)
    echo -e "${GREEN}✓ $NODE_VERSION${NC}"
else
    echo -e "${RED}✗ Node 未安装${NC}"
    exit 1
fi

echo ""
echo "=========================================="
echo "📦 启动后端服务"
echo "=========================================="

# 启动后端（后台运行）
cd /home/yuzihao/workspace/his_agent/his_agent-backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

echo "正在启动后端服务..."
nohup mvn spring-boot:run -Dspring-boot.run.fork=false > /tmp/his-backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > /tmp/his-backend.pid

echo "等待后端启动（约 30 秒）..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/patients/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 后端服务启动成功！${NC}"
        echo "   访问地址：http://localhost:8080"
        echo "   健康检查：http://localhost:8080/api/patients/health"
        break
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "=========================================="
echo "🎨 启动前端服务"
echo "=========================================="

# 启动前端（后台运行）
cd /home/yuzihao/workspace/his_agent/his_agent-frontend

echo "正在启动前端服务..."
nohup npm run dev > /tmp/his-frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > /tmp/his-frontend.pid

echo "等待前端启动（约 10 秒）..."
for i in {1..10}; do
    if curl -s http://localhost:5173 > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 前端服务启动成功！${NC}"
        echo "   访问地址：http://localhost:5173"
        break
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "=========================================="
echo "✅ 启动完成！"
echo "=========================================="
echo ""
echo "📱 访问地址:"
echo "   前端页面：http://localhost:5173"
echo "   后端 API:  http://localhost:8080"
echo ""
echo "🛑 停止服务:"
echo "   后端：kill \$(cat /tmp/his-backend.pid)"
echo "   前端：kill \$(cat /tmp/his-frontend.pid)"
echo ""
echo "📋 查看日志:"
echo "   后端：tail -f /tmp/his-backend.log"
echo "   前端：tail -f /tmp/his-frontend.log"
echo ""
echo "=========================================="
