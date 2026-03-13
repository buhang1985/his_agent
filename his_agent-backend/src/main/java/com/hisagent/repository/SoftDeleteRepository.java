package com.hisagent.repository;

import com.hisagent.model.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * 软删除 Repository 基接口
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {

    /**
     * 查询所有已删除的数据
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NOT NULL")
    List<T> findAllDeleted();

    /**
     * 根据删除人查询已删除数据
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedBy = :deletedBy")
    List<T> findAllDeletedBy(String deletedBy);
}
