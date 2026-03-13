# 监控告警配置文档

**版本**: 1.0  
**日期**: 2026-03-12  
**适用**: 运维工程师、后端开发人员

---

## 1. 概述

本系统基于 Spring Boot Actuator + Micrometer + Prometheus + Grafana 构建监控告警体系。

---

## 2. 技术指标监控

### 2.1 应用健康检查

```bash
# 健康检查端点
GET http://localhost:8080/actuator/health

# 响应示例
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 2.2 核心指标

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `jvm.memory.used` | Gauge | JVM 内存使用量 |
| `jvm.threads.live` | Gauge | 活跃线程数 |
| `http.server.requests` | Counter | HTTP 请求计数 |
| `hikaricp.connections.active` | Gauge | 活跃数据库连接数 |
| `hikaricp.connections.pending` | Gauge | 等待连接数 |
| `resilience4j.circuitbreaker.state` | Gauge | 熔断器状态 |

### 2.3 Prometheus 配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'his-agent'
    scrape_interval: 15s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

---

## 3. 业务指标监控

### 3.1 自定义业务指标

```java
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class ConsultationService {
    
    private final MeterRegistry meterRegistry;
    
    public ConsultationService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 注册计数器
        meterRegistry.counter("his.consultation.created.total");
        meterRegistry.counter("his.consultation.completed.total");
    }
    
    public Consultation createConsultation(CreateConsultationRequest request) {
        Consultation consultation = repository.save(convert(request));
        
        // 记录业务指标
        meterRegistry.counter("his.consultation.created.total").increment();
        
        return consultation;
    }
}
```

### 3.2 业务指标列表

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `his.consultation.created.total` | Counter | 问诊创建总数 |
| `his.consultation.completed.total` | Counter | 问诊完成总数 |
| `his.patient.query.total` | Counter | 患者查询总数 |
| `his.voice.transcription.total` | Counter | 语音转写总数 |
| `his.llm.calls.total` | Counter | LLM 调用总数 |
| `his.llm.calls.failed` | Counter | LLM 调用失败数 |

---

## 4. 告警规则配置

### 4.1 P0 级告警（立即响应）

```yaml
# prometheus-alerts.yml
groups:
  - name: his-agent-p0
    interval: 30s
    rules:
      # CPU 使用率 > 90% 持续 5 分钟
      - alert: HighCPUUsage
        expr: avg(rate(process_cpu_seconds_total[5m])) > 0.9
        for: 5m
        labels:
          severity: p0
        annotations:
          summary: "CPU 使用率过高"
          description: "实例 {{ $labels.instance }} CPU 使用率超过 90%"
      
      # 内存使用率 > 90% 持续 5 分钟
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: p0
        annotations:
          summary: "内存使用率过高"
          description: "实例 {{ $labels.instance }} 内存使用率超过 90%"
      
      # 接口错误率 > 10% 持续 2 分钟
      - alert: HighErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[2m])) / sum(rate(http_server_requests_seconds_count[2m])) > 0.1
        for: 2m
        labels:
          severity: p0
        annotations:
          summary: "接口错误率过高"
          description: "实例 {{ $labels.instance }} 接口错误率超过 10%"
      
      # 数据库连接池耗尽
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.95
        for: 2m
        labels:
          severity: p0
        annotations:
          summary: "数据库连接池耗尽"
          description: "实例 {{ $labels.instance }} 数据库连接池使用率超过 95%"
```

### 4.2 P1 级告警（30 分钟内响应）

```yaml
  - name: his-agent-p1
    interval: 1m
    rules:
      # CPU 使用率 > 70% 持续 10 分钟
      - alert: MediumCPUUsage
        expr: avg(rate(process_cpu_seconds_total[10m])) > 0.7
        for: 10m
        labels:
          severity: p1
        annotations:
          summary: "CPU 使用率偏高"
      
      # 接口响应时间 P99 > 5 秒
      - alert: HighP99Latency
        expr: histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 5
        for: 5m
        labels:
          severity: p1
        annotations:
          summary: "P99 响应时间过长"
          description: "实例 {{ $labels.instance }} P99 响应时间超过 5 秒"
      
      # 缓存命中率 < 50%
      - alert: LowCacheHitRate
        expr: redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total) < 0.5
        for: 10m
        labels:
          severity: p1
        annotations:
          summary: "缓存命中率过低"
```

### 4.3 P2 级告警（24 小时内响应）

```yaml
  - name: his-agent-p2
    interval: 5m
    rules:
      # CPU 使用率 > 50% 持续 30 分钟
      - alert: LowCPUUsage
        expr: avg(rate(process_cpu_seconds_total[30m])) > 0.5
        for: 30m
        labels:
          severity: p2
        annotations:
          summary: "CPU 使用率持续增长"
      
      # 磁盘使用率 > 80%
      - alert: HighDiskUsage
        expr: node_filesystem_avail_bytes / node_filesystem_size_bytes < 0.2
        for: 30m
        labels:
          severity: p2
        annotations:
          summary: "磁盘空间不足"
```

---

## 5. 告警通知渠道

### 5.1 AlertManager 配置

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alertmanager@example.com'

route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default'
  
  routes:
    - match:
        severity: p0
      receiver: 'p0-critical'
    - match:
        severity: p1
      receiver: 'p1-warning'
    - match:
        severity: p2
      receiver: 'p2-info'

receivers:
  - name: 'p0-critical'
    email_configs:
      - to: 'ops-team@example.com'
    webhook_configs:
      - url: 'http://dingtalk-webhook.example.com/p0'
  
  - name: 'p1-warning'
    email_configs:
      - to: 'dev-team@example.com'
  
  - name: 'p2-info'
    email_configs:
      - to: 'dev-team@example.com'
```

### 5.2 通知升级策略

| 告警级别 | 通知渠道 | 升级策略 |
|----------|----------|----------|
| P0 | 邮件 + 钉钉 + 电话 | 5 分钟未响应 → 升级至主管 |
| P1 | 邮件 + 钉钉 | 30 分钟未响应 → 升级至 P0 |
| P2 | 邮件 | 24 小时未响应 → 升级至 P1 |

---

## 6. Grafana 仪表板

### 6.1 核心监控面板

导入以下 Grafana 仪表板：

- **JVM 监控**: ID 3268（官方 JVM 仪表板）
- **Spring Boot 监控**: ID 10280（官方 Spring Boot 仪表板）
- **自定义业务监控**: 自行创建

### 6.2 自定义业务面板配置

```json
{
  "dashboard": {
    "title": "HIS Agent 业务监控",
    "panels": [
      {
        "title": "问诊创建趋势",
        "targets": [
          {
            "expr": "rate(his_consultation_created_total[5m])",
            "legendFormat": "创建速率"
          }
        ]
      },
      {
        "title": "LLM 调用成功率",
        "targets": [
          {
            "expr": "1 - (sum(rate(his_llm_calls_failed_total[5m])) / sum(rate(his_llm_calls_total[5m])))",
            "legendFormat": "成功率"
          }
        ]
      }
    ]
  }
}
```

---

## 7. 相关文档

- [API 约束使用文档](./api-constraints-guide.md)
- [熔断降级配置文档](./circuit-breaker-guide.md)
