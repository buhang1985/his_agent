#!/bin/bash
# his_agent 快速启动脚本

export PATH="/Applications/OrbStack.app/Contents/MacOS/xbin:$PATH"

echo "🚀 his_agent 项目启动脚本"
echo "=========================="
echo ""

# 检查 Docker 是否可用
echo "📦 检查 Docker 状态..."
if docker ps >/dev/null 2>&1; then
    echo "✅ Docker 已就绪"
    
    # 启动 MySQL 和 Redis
    echo "🐬 启动 MySQL 和 Redis..."
    cd /Users/yzh/opencode_workspace/his_agent
    docker-compose -f docker-compose.dev.yml up -d
    
    echo ""
    echo "📊 容器状态:"
    docker-compose -f docker-compose.dev.yml ps
else
    echo "❌ Docker 未就绪"
    echo ""
    echo "请先完成以下步骤:"
    echo "1. 打开 OrbStack 应用 (/Applications/OrbStack.app)"
    echo "2. 等待 OrbStack 完全启动（状态栏图标变蓝）"
    echo "3. 在终端运行：docker ps"
    echo "4. 看到输出后，再次运行此脚本"
    exit 1
fi

echo ""
echo "✅ Docker 服务已启动！"
echo ""
echo "下一步:"
echo "1. 启动后端：cd his_agent-backend && mvn spring-boot:run"
echo "2. 启动前端：cd his_agent-frontend && npm run dev"
