#!/bin/bash
# =====================================================
# HIS Agent 部署回滚脚本
# 用途：快速回滚到上一个稳定版本
# =====================================================

set -e

# 配置
DEPLOY_DIR="/var/www/his-agent"
CURRENT_LINK="$DEPLOY_DIR/current"
BACKUP_DIR="$DEPLOY_DIR/backups"
RELEASES_DIR="$DEPLOY_DIR/releases"
MAX_RELEASES=5

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否以 root 运行
if [ "$EUID" -ne 0 ]; then
    log_error "请使用 sudo 运行此脚本"
    exit 1
fi

# 显示可用版本
show_releases() {
    log_info "可用的版本:"
    ls -1 "$RELEASES_DIR" 2>/dev/null | sort -r | head -n $MAX_RELEASES
}

# 回滚到指定版本
rollback_to() {
    local target_version=$1
    
    if [ -z "$target_version" ]; then
        # 自动回滚到上一个版本
        current_version=$(readlink "$CURRENT_LINK" | xargs basename)
        target_version=$(ls -1 "$RELEASES_DIR" | sort -r | grep -B1 "^$current_version$" | head -n1)
        
        if [ -z "$target_version" ] || [ "$target_version" = "$current_version" ]; then
            log_error "没有可用的旧版本进行回滚"
            show_releases
            exit 1
        fi
    fi
    
    local target_path="$RELEASES_DIR/$target_version"
    
    if [ ! -d "$target_path" ]; then
        log_error "版本 $target_version 不存在"
        show_releases
        exit 1
    fi
    
    log_info "开始回滚到版本：$target_version"
    
    # 备份当前版本
    if [ -L "$CURRENT_LINK" ]; then
        current_version=$(readlink "$CURRENT_LINK" | xargs basename)
        backup_path="$BACKUP_DIR/$current_version-$(date +%Y%m%d-%H%M%S)"
        log_info "备份当前版本到：$backup_path"
        cp -r "$CURRENT_LINK" "$backup_path"
    fi
    
    # 切换软链接
    log_info "切换软链接到：$target_path"
    ln -sfn "$target_path" "$CURRENT_LINK"
    
    # 清理旧版本
    cleanup_old_releases
    
    # 重启服务
    log_info "重启 Nginx 服务..."
    systemctl restart nginx
    
    log_info "回滚完成！当前版本：$target_version"
    log_warn "请验证应用是否正常运行"
    log_warn "如需再次回滚：./rollback.sh"
}

# 清理旧版本
cleanup_old_releases() {
    log_info "清理旧版本（保留最近 $MAX_RELEASES 个）..."
    
    cd "$RELEASES_DIR"
    ls -1t | tail -n +$((MAX_RELEASES + 1)) | while read version; do
        log_info "删除旧版本：$version"
        rm -rf "$version"
    done
    
    cd - > /dev/null
}

# 显示帮助
show_help() {
    echo "用法：$0 [选项]"
    echo ""
    echo "选项:"
    echo "  -l, --list          显示可用版本"
    echo "  -t, --to VERSION    回滚到指定版本"
    echo "  -h, --help          显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --list                    # 显示可用版本"
    echo "  $0 --to v1.2.3              # 回滚到 v1.2.3"
    echo "  $0                          # 回滚到上一个版本"
}

# 主程序
main() {
    case "${1:-}" in
        -l|--list)
            show_releases
            ;;
        -t|--to)
            rollback_to "$2"
            ;;
        -h|--help)
            show_help
            ;;
        "")
            rollback_to ""
            ;;
        *)
            log_error "未知选项：$1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
