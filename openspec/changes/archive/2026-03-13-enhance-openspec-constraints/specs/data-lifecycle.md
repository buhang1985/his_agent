# 数据生命周期规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：软删除规范

业务数据必须使用软删除，保留审计追溯能力。

#### 场景：软删除表结构
- **当** 设计数据表时
- **那么** 必须包含软删除字段：
```sql
-- 所有业务表必须包含软删除字段
CREATE TABLE consultations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id VARCHAR(50) NOT NULL,
    -- 业务字段...
    
    -- 软删除字段（必须）
    deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    deleted_at DATETIME COMMENT '删除时间',
    deleted_by VARCHAR(50) COMMENT '删除人 ID',
    delete_reason VARCHAR(500) COMMENT '删除原因',
    
    -- 审计字段（必须）
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    
    -- 索引
    INDEX idx_deleted (deleted),
    INDEX idx_deleted_at (deleted_at)
);
```

#### 场景：软删除查询
- **当** 查询数据时
- **那么** 必须：
```java
// 使用 Hibernate @Where 自动过滤已删除
@Entity
@Where(clause = "deleted = 0")
public class Consultation {
    // ...
}

// Repository 方法自动只查询未删除数据
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    // 所有查询自动包含 deleted = 0 条件
}
```

#### 场景：软删除操作
- **当** 删除数据时
- **那么** 必须：
```java
@Service
public class ConsultationService {
    
    @Transactional
    public void deleteConsultation(Long id, String userId, String reason) {
        Consultation consultation = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("问诊记录不存在"));
        
        // 软删除
        consultation.setDeleted(true);
        consultation.setDeletedAt(LocalDateTime.now());
        consultation.setDeletedBy(userId);
        consultation.setDeleteReason(reason);
        
        repository.save(consultation);
        
        // 记录审计日志
        auditLogger.log("CONSULTATION_DELETED", 
            Map.of("consultationId", id, "userId", userId, "reason", reason));
    }
    
    // 恢复删除的数据
    public void restoreConsultation(Long id) {
        Consultation consultation = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("问诊记录不存在"));
        
        consultation.setDeleted(false);
        consultation.setDeletedAt(null);
        consultation.setDeletedBy(null);
        consultation.setDeleteReason(null);
        
        repository.save(consultation);
    }
}
```

### 需求：物理删除 vs 软删除使用场景

必须根据数据性质选择删除策略。

#### 场景：使用软删除
- **当** 处理以下数据时
- **那么** 必须使用软删除：
  - 患者相关数据（问诊记录、病历、处方）
  - 业务交易数据（订单、支付记录）
  - 审计相关数据（操作日志、访问记录）
  - 医疗数据（诊断、检查结果）
  
**理由**: 医疗数据需要长期保存，满足合规和审计要求。

#### 场景：使用物理删除
- **当** 处理以下数据时
- **那么** 可以使用物理删除：
  - 临时数据（会话缓存、验证码）
  - 日志数据（超出保留期的日志）
  - 测试数据（测试产生的临时数据）
  - 归档后的数据（已迁移到冷存储）
  
**理由**: 这些数据不需要追溯，或已另行保存。

### 需求：数据保留策略

必须根据数据类型和合规要求设置保留时长。

#### 场景：医疗数据保留
- **当** 保留医疗数据时
- **那么** 必须：
```yaml
data-retention:
  medical-records:
    adult: 30y          # 成人病历 30 年
    pediatric: 30y      # 儿童病历（到成年后 30 年）
    prescriptions: 7y   # 处方 7 年
    lab-results: 10y    # 检查结果 10 年
    
  business-data:
    consultations: 10y  # 问诊记录 10 年
    payments: 7y        # 支付记录 7 年
    appointments: 5y    # 预约记录 5 年
    
  logs:
    audit-logs: 7y      # 审计日志 7 年（HIPAA）
    access-logs: 2y     # 访问日志 2 年
    error-logs: 1y      # 错误日志 1 年
    debug-logs: 30d     # 调试日志 30 天
```

#### 场景：数据归档
- **当** 数据超保留期时
- **那么** 必须：
  - 归档到冷存储（对象存储/磁带）
  - 保持可查询状态
  - 加密存储
  - 记录归档日志

### 需求：数据清理规范

必须定期清理过期数据，释放存储空间。

#### 场景：清理任务
- **当** 清理过期数据时
- **那么** 必须：
```java
@Service
public class DataCleanupService {
    
    // 每天凌晨 3 点执行
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredData() {
        LocalDateTime now = LocalDateTime.now();
        
        // 清理超期调试日志
        int debugLogsDeleted = logRepository.deleteByLevelAndBefore(
            LogLevel.DEBUG, now.minusDays(30));
        
        // 清理超期访问日志（归档后删除）
        int accessLogsDeleted = logRepository.deleteByLevelAndBefore(
            LogLevel.INFO, now.minusYears(2));
        
        log.info("清理完成：删除调试日志 {} 条，访问日志 {} 条", 
            debugLogsDeleted, accessLogsDeleted);
    }
    
    // 每月 1 号执行
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void archiveOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(10);
        
        // 归档 10 年前的问诊记录
        List<Consultation> oldConsultations = 
            consultationRepository.findByCreatedAtBefore(cutoff);
        
        for (Consultation consultation : oldConsultations) {
            archiveService.archive(consultation);
        }
    }
}
```

#### 场景：清理审计
- **当** 执行数据清理时
- **那么** 必须：
  - 记录清理日志（清理了什么、多少、何时）
  - 清理前备份（如需要）
  - 清理后验证
  - 发送清理报告
