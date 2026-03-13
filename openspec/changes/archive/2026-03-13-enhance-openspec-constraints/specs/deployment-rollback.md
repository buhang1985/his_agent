# 部署回滚策略规范

**版本**: 1.0  
**日期**: 2026-03-12  
**状态**: 新增

---

## 新增需求

### 需求：版本管理策略

必须实现严格的版本管理，支持快速回滚。

#### 场景：Docker 镜像版本
- **当** 构建 Docker 镜像时
- **那么** 必须：
```bash
# ✅ 正确：语义化版本标签
docker build -t his_agent-backend:1.2.3 .
docker build -t his_agent-backend:1.2.3 -t his_agent-backend:latest .
docker build -t his_agent-backend:1.2.3 -t his_agent-backend:$(git rev-parse --short HEAD) .

# ❌ 错误：仅使用 latest 标签（禁止）
docker build -t his_agent-backend:latest .  # 无法回滚
```

#### 场景：版本号规范
- **当** 定义版本号时
- **那么** 必须：
```yaml
# 语义化版本（SemVer）
version: MAJOR.MINOR.PATCH  # 例如：1.2.3

# 版本规则
# MAJOR: 不兼容的变更
# MINOR: 向后兼容的功能
# PATCH: 向后兼容的问题修复

# 预发布版本
version: 1.2.3-alpha.1
version: 1.2.3-beta.2
version: 1.2.3-rc.1
```

#### 场景：版本保留策略
- **当** 推送镜像时
- **那么** 必须：
  - 保留最近 5 个版本
  - 保留最近 3 个月的版本
  - 生产环境版本永久保留
  - 定期清理旧版本

---

### 需求：回滚脚本实现

必须实现一键回滚脚本，支持快速恢复。

#### 场景：应用回滚脚本
- **当** 需要回滚时
- **那么** 必须：
```bash
#!/bin/bash
# deploy/scripts/rollback.sh

set -e

# 配置
SERVICE_NAME="his_agent"
ROLLBACK_VERSION="${1:-}"  # 可选：指定回滚版本
MAX_RETRIES=3
RETRY_INTERVAL=10

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查参数
if [ -z "$ROLLBACK_VERSION" ]; then
    log_warn "未指定版本，将回滚到上一个版本"
    # 获取上一个版本
    CURRENT_VERSION=$(docker ps --filter "name=${SERVICE_NAME}" --format "{{.Image}}" | cut -d':' -f2)
    ROLLBACK_VERSION=$(docker images ${SERVICE_NAME} --format "{{.Tag}}" | grep -v "latest" | sort -V | tail -n 2 | head -n 1)
    
    if [ -z "$ROLLBACK_VERSION" ]; then
        log_error "找不到可回滚的版本"
        exit 1
    fi
    
    log_info "当前版本：${CURRENT_VERSION}, 回滚到：${ROLLBACK_VERSION}"
fi

# 停止当前服务
log_info "停止当前服务..."
docker-compose stop app

# 备份当前版本
log_info "备份当前版本..."
docker tag ${SERVICE_NAME}:${CURRENT_VERSION} ${SERVICE_NAME}:${CURRENT_VERSION}-backup-$(date +%Y%m%d%H%M%S)

# 启动旧版本
log_info "启动版本 ${ROLLBACK_VERSION}..."
docker-compose up -d app --no-deps --force-recreate

# 健康检查
log_info "执行健康检查..."
for i in $(seq 1 $MAX_RETRIES); do
    if docker-compose exec -T app wget -qO- http://localhost:8080/actuator/health | grep -q "UP"; then
        log_info "健康检查通过 ✅"
        exit 0
    fi
    
    log_warn "健康检查失败，等待 ${RETRY_INTERVAL}s 后重试 (${i}/${MAX_RETRIES})..."
    sleep $RETRY_INTERVAL
done

log_error "健康检查失败，回滚失败 ❌"
exit 1
```

#### 场景：数据库回滚脚本
- **当** 需要回滚数据库时
- **那么** 必须：
```bash
#!/bin/bash
# deploy/scripts/db-rollback.sh

set -e

TARGET_VERSION="${1}"

if [ -z "$TARGET_VERSION" ]; then
    echo "用法：db-rollback.sh <目标版本>"
    echo "示例：db-rollback.sh 2"
    echo ""
    echo "可用版本:"
    ls -1 src/main/resources/db/migration/V*.sql | sed 's/.*V\([0-9]*\)__.*/\1/' | sort -n
    exit 1
fi

# Flyway 回滚（需要企业版）
# 或使用手动回滚脚本

# 查找回滚脚本
ROLLBACK_SCRIPT="src/main/resources/db/migration/R__rollback_v${TARGET_VERSION}.sql"

if [ ! -f "$ROLLBACK_SCRIPT" ]; then
    echo "错误：回滚脚本不存在：$ROLLBACK_SCRIPT"
    echo "请手动创建回滚脚本"
    exit 1
fi

# 执行回滚
echo "执行数据库回滚到版本 ${TARGET_VERSION}..."
mysql -h $DB_HOST -u $DB_USER -p$DB_PASSWORD $DB_NAME < "$ROLLBACK_SCRIPT"

echo "数据库回滚完成 ✅"
```

---

### 需求：蓝绿部署策略

必须实现蓝绿部署，支持零停机回滚。

#### 场景：Nginx 蓝绿配置
- **当** 配置蓝绿部署时
- **那么** 必须：
```nginx
# /etc/nginx/conf.d/his_agent.conf

# 蓝色环境
upstream his_agent_blue {
    server 127.0.0.1:8081;
}

# 绿色环境
upstream his_agent_green {
    server 127.0.0.1:8082;
}

# 当前活跃环境（通过变量切换）
# set $active_backend "his_agent_blue";

# 灰度发布：10% 流量到绿色
map $http_x_canary $active_backend {
    default     his_agent_blue;
    "true"      his_agent_green;
}

server {
    listen 80;
    server_name his-agent.hospital.com;
    
    location / {
        proxy_pass http://$active_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### 场景：蓝绿切换脚本
- **当** 切换环境时
- **那么** 必须：
```bash
#!/bin/bash
# deploy/scripts/blue-green-switch.sh

set -e

CURRENT_ENV="${1}"
TARGET_ENV="${2}"

if [ -z "$CURRENT_ENV" ] || [ -z "$TARGET_ENV" ]; then
    echo "用法：blue-green-switch.sh <current> <target>"
    echo "示例：blue-green-switch.sh blue green"
    exit 1
fi

# 更新 Nginx 配置
sed -i "s/set \$active_backend \"his_agent_${CURRENT_ENV}\";/set \$active_backend \"his_agent_${TARGET_ENV}\";/g" /etc/nginx/conf.d/his_agent.conf

# 重载 Nginx
nginx -t && systemctl reload nginx

echo "切换完成：${CURRENT_ENV} -> ${TARGET_ENV} ✅"
```

---

### 需求：数据库回滚方案

必须实现数据库回滚方案，保证数据一致性。

#### 场景：Flyway 回滚（企业版）
- **当** 使用 Flyway 企业版时
- **那么** 必须：
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.0.0</version>
</dependency>
<!-- Flyway 企业版支持 undo 回滚 -->
```

```bash
# 执行回滚
flyway undo
```

#### 场景：手动回滚脚本（社区版）
- **当** 使用 Flyway 社区版时
- **那么** 必须：
```sql
-- src/main/resources/db/migration/R__rollback_v3_add_index.sql
-- 回滚 V3 迁移：删除索引

-- 前滚操作的逆向
DROP INDEX IF EXISTS idx_patient_id ON consultations;
DROP INDEX IF EXISTS idx_status ON consultations;

-- 记录回滚
INSERT INTO flyway_schema_history (version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES ('3', 'rollback_add_index', 'UNDO', 'R__rollback_v3_add_index.sql', NULL, 'admin', NOW(), 0, 1);
```

#### 场景：数据库备份恢复
- **当** 需要完全恢复时
- **那么** 必须：
```bash
#!/bin/bash
# deploy/scripts/db-restore.sh

set -e

BACKUP_FILE="${1}"

if [ -z "$BACKUP_FILE" ]; then
    echo "用法：db-restore.sh <备份文件>"
    echo "可用备份:"
    ls -lh /backup/mysql/
    exit 1
fi

echo "恢复数据库从备份：$BACKUP_FILE"

# 停止应用
docker-compose stop app

# 恢复数据库
gunzip < "$BACKUP_FILE" | docker-compose exec -T mysql mysql -u root -p$MYSQL_ROOT_PASSWORD

# 重启应用
docker-compose start app

echo "数据库恢复完成 ✅"
```

---

### 需求：快速回滚机制

必须实现 5 分钟内快速回滚机制。

#### 场景：回滚时间要求
- **当** 发生故障时
- **那么** 必须：
```yaml
# 回滚时间目标（RTO）
rollback_time_objective:
  检测时间：≤ 2 分钟
  决策时间：≤ 1 分钟
  执行时间：≤ 2 分钟
  总时间：≤ 5 分钟
```

#### 场景：回滚检查清单
- **当** 执行回滚时
- **那么** 必须检查：
```markdown
## 回滚检查清单

### 回滚前
- [ ] 确认故障现象和影响范围
- [ ] 确认回滚版本可用
- [ ] 通知相关人员（运维、开发、业务）
- [ ] 备份当前状态（应用 + 数据库）
- [ ] 准备回滚脚本

### 回滚中
- [ ] 停止当前版本
- [ ] 启动旧版本
- [ ] 执行健康检查
- [ ] 验证核心功能

### 回滚后
- [ ] 确认故障已解决
- [ ] 通知相关人员
- [ ] 记录回滚原因和过程
- [ ] 保留故障现场（日志、快照）
- [ ] 安排事后分析（Post-mortem）
```

---

### 需求：CI/CD 回滚集成

CI/CD 流水线必须集成回滚能力。

#### 场景：GitHub Actions 回滚
- **当** 配置 CI/CD 时
- **那么** 必须：
```yaml
# .github/workflows/rollback.yml
name: Rollback

on:
  workflow_dispatch:
    inputs:
      version:
        description: '回滚到的版本'
        required: true
        type: string

jobs:
  rollback:
    runs-on: ubuntu-latest
    environment: production
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Pull old version
        run: |
          docker pull his_agent-backend:${{ inputs.version }}
      
      - name: Deploy old version
        run: |
          docker-compose stop app
          docker-compose up -d app --no-deps --force-recreate
      
      - name: Health check
        run: |
          for i in {1..5}; do
            if curl -f http://localhost:8080/actuator/health; then
              echo "✅ Health check passed"
              exit 0
            fi
            sleep 10
          done
          echo "❌ Health check failed"
          exit 1
```

---

### 需求：版本追踪

必须实现版本追踪，便于审计和回滚。

#### 场景：版本信息暴露
- **当** 应用启动时
- **那么** 必须：
```yaml
# application.yml
management:
  info:
    env:
      enabled: true
    git:
      enabled: true
      mode: full

# 构建时注入版本信息
# pom.xml
<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
    <version>5.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>revision</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 场景：版本信息查询
- **当** 查询版本时
- **那么** 必须：
```bash
# 查询当前版本
curl http://localhost:8080/actuator/info

# 响应示例
{
  "git": {
    "branch": "main",
    "commit": {
      "id": "abc1234",
      "time": "2026-03-12T00:00:00Z"
    }
  },
  "build": {
    "version": "1.2.3",
    "artifact": "his_agent-backend",
    "time": "2026-03-12T00:00:00Z"
  }
}
```

---

## 验收标准

### 版本管理
- [ ] Docker 镜像使用语义化版本
- [ ] 禁止仅使用 latest 标签
- [ ] 保留最近 5 个版本
- [ ] Git commit hash 作为额外标签

### 回滚脚本
- [ ] 应用回滚脚本可用
- [ ] 数据库回滚脚本可用
- [ ] 回滚脚本经过测试
- [ ] 回滚脚本有文档说明

### 蓝绿部署
- [ ] Nginx 蓝绿配置完成
- [ ] 环境切换脚本可用
- [ ] 支持灰度发布（Canary）

### 数据库回滚
- [ ] 每个迁移有对应回滚脚本
- [ ] 回滚脚本经过测试
- [ ] 数据库备份可用

### 快速回滚
- [ ] 回滚时间 ≤ 5 分钟
- [ ] 回滚检查清单完整
- [ ] 回滚流程文档化

### CI/CD 集成
- [ ] GitHub Actions 回滚 workflow
- [ ] 一键回滚能力
- [ ] 回滚自动健康检查

### 版本追踪
- [ ] /actuator/info 暴露版本信息
- [ ] Git commit 信息可查
- [ ] 构建时间可追溯
