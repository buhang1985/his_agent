#!/bin/bash
# deploy.sh - his_agent 一键部署脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 主流程
main() {
    echo "========================================"
    echo "  his_agent 一键部署脚本"
    echo "========================================"
    echo ""
    
    # 1. 检查依赖
    log_info "[1/8] 检查依赖..."
    check_dependencies
    
    # 2. 检查配置文件
    log_info "[2/8] 检查配置文件..."
    check_config
    
    # 3. 创建目录
    log_info "[3/8] 创建必要目录..."
    create_directories
    
    # 4. 加载环境变量
    log_info "[4/8] 加载环境变量..."
    source_env
    
    # 5. 构建 Docker 镜像
    log_info "[5/8] 构建 Docker 镜像..."
    build_images
    
    # 6. 启动服务
    log_info "[6/8] 启动服务..."
    start_services
    
    # 7. 健康检查
    log_info "[7/8] 等待服务启动并进行健康检查..."
    health_check
    
    # 8. 初始化数据库
    log_info "[8/8] 初始化数据库..."
    init_database
    
    echo ""
    echo "========================================"
    echo -e "${GREEN}  部署完成!${NC}"
    echo "========================================"
    echo ""
    echo "访问地址："
    echo "  - 应用：http://localhost:8080"
    echo "  - 健康检查：http://localhost:8080/actuator/health"
    echo "  - API 文档：http://localhost:8080/swagger-ui.html"
    echo ""
    echo "常用命令："
    echo "  - 查看日志：docker-compose logs -f app"
    echo "  - 停止服务：docker-compose down"
    echo "  - 重启服务：docker-compose restart"
    echo "  - 查看状态：docker-compose ps"
    echo ""
}

# 检查依赖
check_dependencies() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_info "  ✓ Docker 和 Docker Compose 已安装"
}

# 检查配置文件
check_config() {
    if [ ! -f ".env" ]; then
        log_warn ".env 文件不存在"
        if [ -f ".env.example" ]; then
            log_info "从 .env.example 创建 .env 文件"
            cp .env.example .env
            log_error "请编辑 .env 文件配置必要参数后重新运行"
            exit 1
        else
            log_error ".env.example 不存在"
            exit 1
        fi
    fi
    
    log_info "  ✓ 配置文件检查通过"
}

# 创建目录
create_directories() {
    mkdir -p logs
    mkdir -p config
    mkdir -p backup/{mysql,redis}
    chmod 755 logs config backup
    log_info "  ✓ 目录创建完成"
}

# 加载环境变量
source_env() {
    if [ -f ".env" ]; then
        export $(cat .env | grep -v '^#' | xargs)
        log_info "  ✓ 环境变量已加载"
    fi
}

# 构建镜像
build_images() {
    docker-compose build --no-cache
    log_info "  ✓ Docker 镜像构建完成"
}

# 启动服务
start_services() {
    docker-compose up -d
    log_info "  ✓ 服务已启动"
}

# 健康检查
health_check() {
    log_info "等待服务启动..."
    sleep 30
    
    local max_attempts=10
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_info "  ✓ 服务健康检查通过"
            return 0
        fi
        
        log_warn "  第 $attempt 次健康检查失败，等待重试..."
        sleep 10
        attempt=$((attempt + 1))
    done
    
    log_error "健康检查失败，请查看日志"
    docker-compose logs app
    exit 1
}

# 初始化数据库
init_database() {
    log_info "等待 MySQL 就绪..."
    sleep 10
    
    docker-compose exec -T mysql mysql -u root -p${MYSQL_ROOT_PASSWORD:-CHANGE_ME_ROOT_PASSWORD} \
        -e "SELECT 1" his_agent &> /dev/null
    
    if [ $? -eq 0 ]; then
        log_info "  ✓ 数据库连接正常"
    else
        log_warn "数据库连接失败，请手动检查"
    fi
}

# 执行主流程
main "$@"
