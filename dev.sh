#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo "========================================"
echo "  his_agent 开发环境启动"
echo "========================================"

if ! command -v docker &> /dev/null; then
  log_error "Docker 未安装"
  exit 1
fi

log_info "[1/4] 启动基础服务 (MySQL, Redis)..."
docker-compose -f docker-compose.dev.yml up -d mysql redis
sleep 5

log_info "[2/4] 启动后端..."
cd his_agent-backend
cp .env.example .env 2>/dev/null || true
mvn spring-boot:run &
BACKEND_PID=$!
cd ..

log_info "[3/4] 启动前端..."
cd his_agent-frontend
cp .env.example .env 2>/dev/null || true
npm run dev &
FRONTEND_PID=$!
cd ..

log_info "[4/4] 等待服务就绪..."
sleep 15

echo ""
echo "========================================"
echo "  ✅ 开发环境已启动"
echo "========================================"
echo ""
echo "  📱 前端：http://localhost:3000"
echo "  ☕ 后端：http://localhost:8080"
echo "  📖 API 文档：http://localhost:8080/swagger-ui.html"
echo ""
echo "  按 Ctrl+C 停止所有服务"
echo ""

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; docker-compose -f docker-compose.dev.yml down" EXIT

wait
