package com.hisagent.service;

import com.hisagent.model.BaseEntity;
import com.hisagent.repository.SoftDeleteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 软删除服务
 * 提供统一的软删除、恢复、查询已删除数据功能
 */
@Slf4j
@RequiredArgsConstructor
public class SoftDeleteService<T extends BaseEntity> {

    private final SoftDeleteRepository<T, String> repository;

    /**
     * 软删除
     */
    @Transactional
    public void softDelete(String id, String deletedBy) {
        Optional<T> entity = repository.findById(id);
        if (entity.isPresent()) {
            T t = entity.get();
            t.markDeleted(deletedBy);
            repository.save(t);
            log.info("Entity {} marked as deleted by {}", id, deletedBy);
        } else {
            log.warn("Entity {} not found for deletion", id);
        }
    }

    /**
     * 批量软删除
     */
    @Transactional
    public void softDeleteBatch(List<String> ids, String deletedBy) {
        List<T> entities = repository.findAllById(ids);
        entities.forEach(entity -> {
            entity.markDeleted(deletedBy);
            repository.save(entity);
        });
        log.info("Batch deleted {} entities by {}", entities.size(), deletedBy);
    }

    /**
     * 恢复数据
     */
    @Transactional
    public void restore(String id) {
        Optional<T> entity = repository.findById(id);
        if (entity.isPresent()) {
            T t = entity.get();
            t.restore();
            repository.save(t);
            log.info("Entity {} restored", id);
        }
    }

    /**
     * 查询已删除的数据
     */
    public List<T> findDeleted() {
        return repository.findAllDeleted();
    }

    /**
     * 永久删除（谨慎使用）
     */
    @Transactional
    public void hardDelete(String id) {
        repository.deleteById(id);
        log.warn("Entity {} permanently deleted", id);
    }
}
