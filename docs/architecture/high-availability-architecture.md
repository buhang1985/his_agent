# his_agent 高可用架构设计

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 已批准  
**部署模式**: 本地物理服务器 + 云端 AI 服务

---

## 1. 架构概述

### 1.1 部署原则

- **数据主权**: 所有患者数据留存院内，仅 AI 推理使用云端服务
- **高可用**: 双机热备，故障自动切换 < 3 秒
- **易迁移**: 容器化封装，一键部署
- **合规性**: 满足等保三级要求

### 1.2 架构全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                    医院内网 (Hospital LAN)                       │
│                     等保三级保护区域                              │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  HIS 系统                                                │   │
│  │  - 分配 Token 给 his_agent                              │   │
│  │  - iframe 嵌入 his_agent 前端                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                           │ postMessage (Token)                 │
│                           ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  his_agent 高可用集群                                    │   │
│  │  ┌──────────────┐          ┌──────────────┐            │   │
│  │  │  Node 1 (主) │◀─VRRP──▶ │  Node 2 (备) │            │   │
│  │  │  Spring Boot │          │  Spring Boot │            │   │
│  │  │  + Keepalived│          │  + Keepalived│            │   │
│  │  │  VIP: .100   │          │              │            │   │
│  │  └──────────────┘          └──────────────┘            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                           ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  数据层                                                  │   │
│  │  ┌──────────────┐         ┌──────────────┐             │   │
│  │  │ MySQL Master │────────▶│ MySQL Slave  │             │   │
│  │  │ (测试期单机) │  复制   │ (生产期)     │             │   │
│  │  └──────────────┘         └──────────────┘             │   │
│  │  ┌──────────────────────────────────────────┐           │   │
│  │  │  Redis Sentinel Cluster (3 节点)          │           │   │
│  │  └──────────────────────────────────────────┘           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│           ════════════════╪══════════════════════              │
│                  防火墙 (仅出站 HTTPS 443)                       │
│           ════════════════╪══════════════════════              │
│                           ▼                                     │
│              ┌────────────────────────┐                         │
│              │   云端 AI 服务          │                         │
│              │  - 讯飞医疗 ASR         │                         │
│              │  - 阿里云 Qwen LLM      │                         │
│              │  - 腾讯云 ASR (备选)    │                         │
│              └────────────────────────┘                         │
│                    公有云                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 高可用设计

### 2.1 应用层高可用 (Keepalived + Nginx)

#### 2.1.1 架构说明

采用 **Keepalived 抢占模式** 实现双机热备：

- **正常状态**: VIP 绑定在主节点 (Node 1)，所有流量路由到 Node 1
- **故障状态**: Node 1 故障时，VIP 自动漂移到 Node 2，Node 2 接管服务
- **恢复状态**: Node 1 恢复后，VIP 自动切回 Node 1 (抢占模式)

#### 2.1.2 Keepalived 配置

```bash
# Node 1 (Master) - /etc/keepalived/keepalived.conf
vrrp_script check_app {
    script "/usr/local/bin/check_app.sh"
    interval 2
    weight -20
    fall 3
    rise 1
}

vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1
    
    authentication {
        auth_type PASS
        auth_pass hisagent2026
    }
    
    track_script {
        check_app
    }
    
    virtual_ipaddress {
        192.168.1.100/24 dev eth0 label eth0:vip
    }
    
    notify_master "/etc/keepalived/notify_master.sh"
    notify_backup "/etc/keepalived/notify_backup.sh"
    notify_fault "/etc/keepalived/notify_fault.sh"
}
```

```bash
# Node 2 (Backup) - /etc/keepalived/keepalived.conf
vrrp_script check_app {
    script "/usr/local/bin/check_app.sh"
    interval 2
    weight -20
    fall 3
    rise 1
}

vrrp_instance VI_1 {
    state BACKUP
    interface eth0
    virtual_router_id 51
    priority 90
    advert_int 1
    
    authentication {
        auth_type PASS
        auth_pass hisagent2026
    }
    
    track_script {
        check_app
    }
    
    virtual_ipaddress {
        192.168.1.100/24 dev eth0 label eth0:vip
    }
}
```

```bash
#!/bin/bash
# /usr/local/bin/check_app.sh - 应用健康检查脚本

# 检查 Spring Boot 应用是否存活
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

if [ "$response" -eq 200 ]; then
    exit 0
else
    exit 1
fi
```

#### 2.1.3 Nginx 配置

```nginx
# /etc/nginx/nginx.conf
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for" '
                    'rt=$request_time uct="$upstream_connect_time" '
                    'uht="$upstream_header_time" urt="$upstream_response_time"';
    
    access_log /var/log/nginx/access.log main;
    
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    
    # 上游服务器 (Spring Boot 集群)
    upstream his_agent_backend {
        least_conn;
        server 127.0.0.1:8080 max_fails=3 fail_timeout=30s;
        keepalive 32;
    }
    
    # 前端静态文件
    server {
        listen 80;
        server_name his-agent.local;
        
        root /var/www/his_agent_frontend;
        index index.html;
        
        # 前端静态文件
        location / {
            try_files $uri $uri/ /index.html;
        }
        
        # API 代理
        location /api/ {
            proxy_pass http://his_agent_backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }
        
        # WebSocket 代理
        location /ws/ {
            proxy_pass http://his_agent_backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            
            proxy_read_timeout 86400s;
        }
    }
}
```

### 2.2 数据层高可用

#### 2.2.1 MySQL 配置 (生产期主从复制)

```ini
# Master 配置 - /etc/mysql/my.cnf
[mysqld]
server-id = 1
log_bin = mysql-bin
binlog_format = ROW
gtid_mode = ON
enforce_gtid_consistency = ON
sync_binlog = 1
innodb_flush_log_at_trx_commit = 1

# 性能优化
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
max_connections = 500
```

```ini
# Slave 配置 - /etc/mysql/my.cnf
[mysqld]
server-id = 2
log_bin = mysql-bin
binlog_format = ROW
gtid_mode = ON
enforce_gtid_consistency = ON
relay_log = mysql-relay-bin
read_only = ON
super_read_only = ON

# 性能优化
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
max_connections = 500
```

```sql
-- 创建复制用户
CREATE USER 'repl'@'%' IDENTIFIED BY 'strong_password';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- Slave 配置
CHANGE MASTER TO
    MASTER_HOST='192.168.1.50',
    MASTER_USER='repl',
    MASTER_PASSWORD='strong_password',
    MASTER_AUTO_POSITION=1;

START SLAVE;
SHOW SLAVE STATUS\G
```

#### 2.2.2 Redis Sentinel 配置

```conf
# Redis Master - /etc/redis/redis.conf
port 6379
bind 0.0.0.0
requirepass strong_password
appendonly yes
appendfsync everysec
```

```conf
# Redis Sentinel - /etc/redis/sentinel.conf
port 26379
bind 0.0.0.0

sentinel monitor mymaster 192.168.1.60 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1

sentinel auth-pass mymaster strong_password
```

### 2.3 云端 AI 服务降级策略

```java
// Resilience4j 熔断器配置
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker asrCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)  // 失败率阈值 50%
            .waitDurationInOpenState(Duration.ofSeconds(30))  // 熔断等待 30 秒
            .slidingWindowSize(10)  // 滑动窗口大小 10
            .minimumNumberOfCalls(5)  // 最小调用次数
            .recordExceptions(Exception.class)
            .build();
        
        return CircuitBreaker.of("asrService", config);
    }
    
    @Bean
    public CircuitBreaker llmCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(60)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(20)
            .minimumNumberOfCalls(5)
            .recordExceptions(Exception.class)
            .build();
        
        return CircuitBreaker.of("llmService", config);
    }
}
```

```java
// ASR 降级服务
@Service
public class SpeechRecognitionServiceImpl implements SpeechRecognitionService {
    
    private final IFlytekAsrClient iflytekClient;
    private final TencentAsrClient tencentClient;
    private final CircuitBreaker asrCircuitBreaker;
    
    @Override
    public String recognize(AudioStream audio) {
        // 主方案：讯飞 ASR
        try {
            return CircuitBreaker.decorateSupplier(
                asrCircuitBreaker,
                () -> iflytekClient.recognize(audio)
            ).get();
        } catch (Exception e) {
            log.warn("讯飞 ASR 失败，降级到腾讯云 ASR", e);
            // 备选：腾讯云 ASR
            return tencentClient.recognize(audio);
        }
    }
}
```

---

## 3. HIS Token 认证架构

### 3.1 Token 验证流程 (公钥验证方案)

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  HIS     │     │  his_    │     │  his_    │     │  HIS     │
│  Auth    │     │  agent   │     │  agent   │     │  Token   │
│  Service │     │  Frontend│     │  Backend │     │  Service │
└────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │                │
     │ 1. 医生登录     │                │                │
     │───────────────▶│                │                │
     │                │                │                │
     │ 2. 生成 JWT     │                │                │
     │    (私钥签名)   │                │                │
     │◀───────────────│                │                │
     │                │                │                │
     │ 3. 打开 iframe  │                │                │
     │───────────────▶│                │                │
     │                │                │                │
     │ 4. postMessage │                │                │
     │    Token       │                │                │
     │                │───────────────▶│                │
     │                │                │                │
     │                │ 5. API 请求     │                │
     │                │    + Token     │                │
     │                │───────────────▶│                │
     │                │                │                │
     │                │                │ 6. 公钥验证     │
     │                │                │───────────────▶│
     │                │                │                │
     │                │                │ 7. 验证结果     │
     │                │                │◀───────────────│
     │                │                │                │
     │                │ 8. 业务响应    │                │
     │                │◀───────────────│                │
     │                │                │                │
```

### 3.2 Token 格式

```java
// JWT Payload 结构
{
  "iss": "his-system",           // 签发方
  "sub": "D001",                 // 医生 ID
  "aud": "his-agent",            // 接收方
  "exp": 1710234567,             // 过期时间 (秒)
  "iat": 1710230967,             // 签发时间
  "jti": "uuid-123-456",         // Token 唯一标识
  
  // 自定义声明
  "userName": "张三",
  "role": "doctor",
  "department": "内科",
  "hospitalId": "H001"
}
```

### 3.3 后端 Token 验证器

```java
@Component
@RequiredArgsConstructor
public class HisTokenVerifier {
    
    private final TokenConfig config;
    private final PublicKey publicKey;
    
    /**
     * 验证 HIS Token
     */
    public TokenClaims verify(String token) {
        try {
            // 解析 JWT
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            // 验证接收方
            String aud = claims.getAudience();
            if (!"his-agent".equals(aud)) {
                throw new InvalidClaimException("Token 接收方不匹配");
            }
            
            // 验证过期时间
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                throw new ExpiredJwtException(null, claims, "Token 已过期");
            }
            
            // 转换为业务对象
            return TokenClaims.builder()
                .userId(claims.getSubject())
                .userName(claims.get("userName", String.class))
                .role(claims.get("role", String.class))
                .department(claims.get("department", String.class))
                .hospitalId(claims.get("hospitalId", String.class))
                .build();
                
        } catch (JwtException e) {
            throw new TokenValidationException("Token 验证失败", e);
        }
    }
}
```

### 3.4 前端 postMessage 集成

```typescript
// his_agent-frontend/src/services/his-bridge.ts

interface HisMessage {
  type: 'AUTH_TOKEN' | 'PATIENT_INFO' | 'NAVIGATION';
  data: any;
}

export class HisBridge {
  private hisOrigin: string = '';
  private token: string | null = null;
  
  constructor() {
    this.initMessageListener();
  }
  
  /**
   * 初始化消息监听
   */
  private initMessageListener() {
    window.addEventListener('message', (event) => {
      // 验证来源
      if (!this.isValidOrigin(event.origin)) {
        console.warn('Invalid HIS origin:', event.origin);
        return;
      }
      
      this.hisOrigin = event.origin;
      const message = event.data as HisMessage;
      
      switch (message.type) {
        case 'AUTH_TOKEN':
          this.handleAuthToken(message.data);
          break;
        case 'PATIENT_INFO':
          this.handlePatientInfo(message.data);
          break;
        case 'NAVIGATION':
          this.handleNavigation(message.data);
          break;
      }
    });
  }
  
  /**
   * 处理认证 Token
   */
  private handleAuthToken(data: { token: string }) {
    this.token = data.token;
    localStorage.setItem('his_token', data.token);
    
    // 通知应用已认证
    window.postMessage({ type: 'HIS_AUTHENTICATED' }, window.location.origin);
  }
  
  /**
   * 发送 API 请求到后端 (自动携带 Token)
   */
  async request<T>(url: string, options?: RequestInit): Promise<T> {
    if (!this.token) {
      throw new Error('未认证：缺少 HIS Token');
    }
    
    const response = await fetch(`/api${url}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`,
        ...options?.headers,
      },
    });
    
    if (!response.ok) {
      if (response.status === 401) {
        // Token 过期，通知 HIS 刷新
        this.requestTokenRefresh();
      }
      throw new Error(`HTTP ${response.status}`);
    }
    
    return response.json();
  }
  
  /**
   * 发送消息到 HIS
   */
  postMessage<T>(type: string, data: T) {
    if (!this.hisOrigin) {
      console.warn('HIS origin not set');
      return;
    }
    
    const message: HisMessage = { type: type as any, data };
    window.parent.postMessage(message, this.hisOrigin);
  }
  
  private isValidOrigin(origin: string): boolean {
    // 配置允许的 HIS 域名
    const allowedOrigins = [
      'https://his.hospital.com',
      'https://ehis.hospital.com',
    ];
    return allowedOrigins.includes(origin);
  }
  
  private requestTokenRefresh() {
    this.postMessage('REQUEST_TOKEN_REFRESH', {});
  }
}

// 单例
export const hisBridge = new HisBridge();
```

---

## 4. 备份策略 (可配置)

### 4.1 备份配置文件

```yaml
# config/backup.yml
backup:
  # 备份模式：local | remote | both
  mode: both
  
  # 本地备份配置
  local:
    enabled: true
    base-dir: /backup/his_agent
    retention-days: 30
    
  # 异地备份配置
  remote:
    enabled: true
    type: rsync  # rsync | sftp | s3
    host: backup.hospital.com
    port: 22
    user: backup_user
    path: /backup/his_agent
    ssh-key: /etc/his_agent/backup_key
    
  # MySQL 备份配置
  mysql:
    full-backup-time: "02:00"  # 每天 2:00 全量备份
    binlog-backup-interval: 15m  # 每 15 分钟增量备份
    compression: true
    
  # Redis 备份配置
  redis:
    rdb-backup-interval: 1h
    aof-rewrite: true
```

### 4.2 备份脚本

```bash
#!/bin/bash
# scripts/backup.sh - 可配置备份脚本

set -e

# 加载配置
CONFIG_FILE="${CONFIG_FILE:-/etc/his_agent/backup.yml}"
source /etc/his_agent/backup.env

# 时间戳
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATE=$(date +%Y%m%d)

# 备份目录
LOCAL_BACKUP_DIR="/backup/his_agent"
REMOTE_BACKUP_DIR="/backup/his_agent"
TEMP_DIR="/tmp/his_agent_backup_$$"

mkdir -p "$TEMP_DIR"
mkdir -p "$LOCAL_BACKUP_DIR/mysql"
mkdir -p "$LOCAL_BACKUP_DIR/redis"
mkdir -p "$LOCAL_BACKUP_DIR/config"

echo "=== his_agent 备份开始: $TIMESTAMP ==="

# MySQL 备份
backup_mysql() {
    echo "[1/4] 备份 MySQL 数据库..."
    
    # 全量备份
    mysqldump -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" \
        --single-transaction \
        --routines \
        --triggers \
        --compress \
        his_agent | gzip > "$TEMP_DIR/mysql_full_$TIMESTAMP.sql.gz"
    
    # 移动到本地备份目录
    mv "$TEMP_DIR/mysql_full_$TIMESTAMP.sql.gz" "$LOCAL_BACKUP_DIR/mysql/"
    
    echo "  ✓ MySQL 全量备份完成"
}

# Redis 备份
backup_redis() {
    echo "[2/4] 备份 Redis 数据..."
    
    # 触发 RDB 保存
    redis-cli -h "$REDIS_HOST" -a "$REDIS_PASSWORD" BGSAVE
    
    # 等待保存完成
    sleep 5
    
    # 复制 RDB 文件
    cp /var/lib/redis/dump.rdb "$TEMP_DIR/redis_dump_$TIMESTAMP.rdb"
    mv "$TEMP_DIR/redis_dump_$TIMESTAMP.rdb" "$LOCAL_BACKUP_DIR/redis/"
    
    echo "  ✓ Redis 备份完成"
}

# 配置文件备份
backup_config() {
    echo "[3/4] 备份配置文件..."
    
    cp -r /etc/his_agent/* "$TEMP_DIR/config/" 2>/dev/null || true
    tar -czf "$LOCAL_BACKUP_DIR/config/config_$TIMESTAMP.tar.gz" \
        -C "$TEMP_DIR" config/
    
    echo "  ✓ 配置文件备份完成"
}

# 异地备份同步
sync_remote() {
    echo "[4/4] 同步到异地备份..."
    
    if [ "$REMOTE_BACKUP_ENABLED" = "true" ]; then
        case "$REMOTE_BACKUP_TYPE" in
            rsync)
                rsync -avz --delete \
                    -e "ssh -i $REMOTE_SSH_KEY" \
                    "$LOCAL_BACKUP_DIR/" \
                    "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/"
                ;;
            sftp)
                # SFTP 上传逻辑
                ;;
            s3)
                # AWS S3 上传逻辑
                ;;
        esac
        
        echo "  ✓ 异地备份同步完成"
    else
        echo "  ⊘ 异地备份未启用"
    fi
}

# 清理过期备份
cleanup_old_backups() {
    echo "[5/5] 清理过期备份..."
    
    find "$LOCAL_BACKUP_DIR/mysql" -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
    find "$LOCAL_BACKUP_DIR/redis" -name "*.rdb" -mtime +$RETENTION_DAYS -delete
    find "$LOCAL_BACKUP_DIR/config" -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete
    
    echo "  ✓ 过期备份清理完成"
}

# 主流程
backup_mysql
backup_redis
backup_config
sync_remote
cleanup_old_backups

# 清理临时目录
rm -rf "$TEMP_DIR"

echo "=== his_agent 备份完成 ==="
```

### 4.3 定时任务配置

```bash
# /etc/cron.d/his_agent_backup

# 每天 2:00 全量备份
0 2 * * * backup_user /opt/his_agent/scripts/backup.sh >> /var/log/his_agent/backup.log 2>&1

# 每 15 分钟增量备份 (binlog)
*/15 * * * * backup_user /opt/his_agent/scripts/backup-binlog.sh >> /var/log/his_agent/backup.log 2>&1
```

---

## 5. 监控架构 (独立部署)

### 5.1 监控系统架构

```
┌─────────────────────────────────────────────────────────────┐
│  独立监控集群 (与 his_agent 分离)                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │  Prometheus  │    │   Grafana    │    │  Alertmanager│ │
│  │  (指标收集)  │    │  (可视化)    │    │  (告警管理)  │ │
│  │  :9090       │    │  :3000       │    │  :9093       │ │
│  └──────┬───────┘    └──────▲───────┘    └──────▲───────┘ │
│         │                   │                   │         │
│         │                   │                   │         │
│         ▼                   │                   │         │
│  ┌──────────────┐          │                   │         │
│  │   Node       │──────────┘                   │         │
│  │  Exporter    │                              │         │
│  │  :9100       │                              │         │
│  └──────────────┘                              │         │
│                                                 │         │
│  ┌──────────────┐          ┌──────────────┐    │         │
│  │  MySQL       │─────────▶│  Grafana     │────┘         │
│  │  Exporter    │          │  Dashboard   │              │
│  │  :9104       │          │              │              │
│  └──────────────┘          └──────────────┘              │
│  ┌──────────────┐                                        │
│  │  Redis       │────────────────────────────────────────┘
│  │  Exporter    │
│  │  :9121       │
│  └──────────────┘
│
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

rule_files:
  - "alerts/*.yml"

scrape_configs:
  # Prometheus 自监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
  
  # his_agent 应用监控
  - job_name: 'his_agent'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['192.168.1.10:8080', '192.168.1.11:8080']
        labels:
          application: 'his_agent'
  
  # MySQL 监控
  - job_name: 'mysql'
    static_configs:
      - targets: ['192.168.1.50:9104']
  
  # Redis 监控
  - job_name: 'redis'
    static_configs:
      - targets: ['192.168.1.60:9121']
  
  # 服务器节点监控
  - job_name: 'node'
    static_configs:
      - targets: ['192.168.1.10:9100', '192.168.1.11:9100']
```

### 5.3 关键告警规则

```yaml
# alerts/his_agent.yml
groups:
  - name: his_agent
    rules:
      # 应用不可用
      - alert: HisAgentDown
        expr: up{application="his_agent"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "his_agent 实例 {{ $labels.instance }} 不可用"
          description: "实例已宕机超过 1 分钟"
      
      # API 响应时间过长
      - alert: HighApiLatency
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="his_agent"}[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API 响应时间过长"
          description: "P99 延迟 {{ $value }}s 超过 2s 阈值"
      
      # 数据库连接池耗尽
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接池使用率过高"
          description: "连接池使用率 {{ $value | humanizePercentage }}"
      
      # ASR 服务失败率高
      - alert: HighAsrFailureRate
        expr: rate(asr_requests_total{status="error"}[5m]) / rate(asr_requests_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "ASR 服务失败率过高"
          description: "失败率 {{ $value | humanizePercentage }} 超过 10%"
      
      # 磁盘空间不足
      - alert: DiskSpaceLow
        expr: node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes < 0.1
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "磁盘空间不足"
          description: "可用空间 {{ $value | humanizePercentage }}"
```

### 5.4 Grafana Dashboard

关键监控面板：

1. **应用概览**
   - QPS / 响应时间 (P50/P95/P99)
   - 错误率
   - JVM 内存使用
   - 线程池状态

2. **数据库监控**
   - 连接池使用率
   - 慢查询数量
   - 主从延迟
   - InnoDB 缓冲池命中率

3. **Redis 监控**
   - 内存使用
   - 连接数
   - 命令执行时间
   - 键空间命中率

4. **业务指标**
   - 问诊会话数/分钟
   - ASR 识别成功率
   - LLM 生成延迟
   - HIS 集成成功率

---

## 6. 部署清单

### 6.1 测试期部署 (单机)

```markdown
## 前置条件
- [ ] 物理服务器就绪 (≥8 核 16G 500G SSD)
- [ ] Ubuntu 22.04 LTS 安装完成
- [ ] 网络配置完成 (固定 IP)
- [ ] 防火墙规则配置 (出站 HTTPS 443)

## 基础软件安装
- [ ] JDK 17 安装
- [ ] MySQL 8.0 安装
- [ ] Redis 7.0 安装
- [ ] Nginx 安装
- [ ] Docker & Docker Compose 安装

## 应用部署
- [ ] 配置文件准备 (.env, application.yml)
- [ ] 数据库初始化 (Flyway 迁移)
- [ ] Docker 镜像构建
- [ ] 容器启动验证

## 云端服务配置
- [ ] 讯飞 ASR API 凭证配置
- [ ] 阿里云 Qwen API 凭证配置
- [ ] 连通性测试

## 监控部署
- [ ] Prometheus 安装
- [ ] Grafana 安装
- [ ] Dashboard 导入
- [ ] 告警规则配置

## 备份配置
- [ ] 备份脚本部署
- [ ] 定时任务配置
- [ ] 备份验证测试

## 验证测试
- [ ] 健康检查通过
- [ ] API 文档可访问
- [ ] 端到端流程测试
- [ ] 故障恢复测试
```

### 6.2 生产期部署 (双机高可用)

```markdown
## 额外前置条件
- [ ] 第二台物理服务器就绪
- [ ] 内网互通验证
- [ ] 共享存储配置 (可选)

## Keepalived 部署
- [ ] Node 1 Keepalived 配置 (MASTER)
- [ ] Node 2 Keepalived 配置 (BACKUP)
- [ ] 健康检查脚本部署
- [ ] VIP 漂移测试

## MySQL 主从复制
- [ ] Master 配置
- [ ] Slave 配置
- [ ] 复制用户创建
- [ ] 主从同步验证
- [ ] 故障切换测试

## Redis Sentinel
- [ ] 3 个 Sentinel 节点部署
- [ ] 主从配置
- [ ] 故障切换测试

## Nginx 负载均衡
- [ ] Nginx 配置优化
- [ ] SSL 证书配置
- [ ] 压力测试

## 高可用验证
- [ ] Node 1 故障模拟
- [ ] VIP 漂移验证
- [ ] 服务恢复验证
- [ ] 数据一致性验证
```

---

## 7. 故障恢复预案

### 7.1 应用节点故障

| 故障场景 | 检测方式 | 恢复动作 | RTO |
|----------|----------|----------|-----|
| Node 1 宕机 | Keepalived 检测 | VIP 漂移到 Node 2 | < 3 秒 |
| Spring Boot 崩溃 | 健康检查失败 | Systemd 自动重启 | < 30 秒 |
| 内存溢出 | 监控告警 | 自动重启 + 告警通知 | < 1 分钟 |

### 7.2 数据库故障

| 故障场景 | 检测方式 | 恢复动作 | RTO |
|----------|----------|----------|-----|
| MySQL 主库宕机 | 从库检测 | 手动提升从库为主 | < 5 分钟 |
| 磁盘满 | 监控告警 | 清理日志 + 扩容 | < 10 分钟 |
| 数据损坏 | 备份验证 | 从备份恢复 | < 1 小时 |

### 7.3 云端 AI 服务故障

| 故障场景 | 检测方式 | 恢复动作 | RTO |
|----------|----------|----------|-----|
| 讯飞 ASR 不可用 | 熔断器开启 | 自动降级到腾讯云 | < 1 秒 |
| Qwen LLM 不可用 | 熔断器开启 | 降级到模板生成 | < 1 秒 |
| 网络中断 | 健康检查失败 | 本地缓存模式 | < 5 秒 |

---

## 8. 性能指标目标

| 指标 | 目标值 | 测量方式 |
|------|--------|----------|
| **可用性** | ≥ 99.9% | 月度正常运行时间 |
| **API 响应时间 (P99)** | < 2 秒 | Prometheus 指标 |
| **ASR 识别延迟** | < 500ms | 端到端测量 |
| **LLM 生成延迟** | < 10 秒 | 端到端测量 |
| **故障切换时间** | < 3 秒 | Keepalived 日志 |
| **数据恢复点目标 (RPO)** | < 15 分钟 | binlog 备份间隔 |
| **数据恢复时间目标 (RTO)** | < 1 小时 | 备份恢复测试 |

---

## 9. 参考资料

- [Keepalived 官方文档](https://www.keepalived.org/documentation.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus 最佳实践](https://prometheus.io/docs/practices/)
- [MySQL 主从复制](https://dev.mysql.com/doc/refman/8.0/en/replication.html)
- [Redis Sentinel](https://redis.io/docs/management/sentinel/)
- [Resilience4j](https://resilience4j.readme.io/)
