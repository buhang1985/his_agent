package com.hisagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 软删除基类
 * 所有需要软删除的实体继承此类
 */
@Data
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL OR deleted_at = '9999-12-31 23:59:59'")
@EqualsAndHashCode(callSuper = true)
public abstract class BaseEntity extends BaseTimeEntity {

    /**
     * 删除时间
     * null 或 9999-12-31 表示未删除
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 删除人 ID
     */
    @Column(name = "deleted_by", length = 36)
    private String deletedBy;

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 标记删除
     */
    public void markDeleted(String deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * 恢复数据
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
