package com.hisagent.service;

import com.hisagent.cache.CacheKeyBuilder;
import com.hisagent.cache.CacheTTL;
import com.hisagent.cache.CacheUtils;
import com.hisagent.model.Patient;
import com.hisagent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务示例
 * 演示缓存穿透防护、雪崩防护和缓存更新策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceExample {

    private final PatientRepository patientRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询患者信息 - 使用@Cacheable 缓存
     * 缓存穿透防护：空值也会被缓存
     * 缓存雪崩防护：使用随机过期时间
     */
    @Cacheable(
        value = "patients",
        key = "#patientId",
        unless = "#result == null"
    )
    public Patient getPatientWithCache(String patientId) {
        log.debug("Querying patient from database: {}", patientId);
        
        return patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
    }

    /**
     * 查询患者信息 - 手动缓存管理（演示空值缓存）
     * 缓存穿透防护：查询为空时缓存空值标记
     */
    public Optional<Patient> getPatientWithNullCache(String patientId) {
        String cacheKey = CacheKeyBuilder.patientInfo(patientId);
        
        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (CacheUtils.isNullValue(cached)) {
                log.debug("Cache hit (null value): {}", patientId);
                return Optional.empty();
            }
            log.debug("Cache hit: {}", patientId);
            return Optional.of((Patient) cached);
        }
        
        // 查询数据库
        Optional<Patient> patient = patientRepository.findById(patientId);
        
        // 写入缓存（包括空值）
        if (patient.isPresent()) {
            long ttl = CacheUtils.calculateTTLWithJitter(
                CacheTTL.PATIENT_DATA, 
                CacheTTL.PATIENT_DATA_UNIT
            );
            redisTemplate.opsForValue().set(cacheKey, patient.get(), ttl, TimeUnit.MILLISECONDS);
            log.debug("Cache set (with jitter TTL {}ms): {}", ttl, patientId);
        } else {
            // 缓存空值，防止缓存穿透
            long ttl = CacheTTL.NULL_VALUE_UNIT.toMillis(CacheTTL.NULL_VALUE);
            redisTemplate.opsForValue().set(cacheKey, CacheUtils.getNullValueMarker(), ttl, TimeUnit.MILLISECONDS);
            log.debug("Cache set (null marker, {}ms): {}", ttl, patientId);
        }
        
        return patient;
    }

    /**
     * 更新患者信息 - 先更新 DB，再删除缓存
     * Cache-Aside 模式：更新时删除缓存，而不是更新缓存
     */
    @Caching(evict = {
        @CacheEvict(value = "patients", key = "#patient.id"),
        @CacheEvict(value = "patientInfo", key = "#patient.id")
    })
    public Patient updatePatient(Patient patient) {
        log.info("Updating patient: {}", patient.getId());
        
        // 先更新数据库
        Patient updated = patientRepository.save(patient);
        
        // @CacheEvict 会自动删除缓存
        // 如果删除失败，重试机制会在切面中处理
        log.info("Patient updated and cache invalidated: {}", patient.getId());
        
        return updated;
    }

    /**
     * 删除患者信息 - 带重试机制
     * 缓存删除失败时自动重试
     */
    @CacheEvict(value = "patients", key = "#patientId")
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public void deletePatientWithRetry(String patientId) {
        log.info("Deleting patient: {}", patientId);
        
        // 手动删除缓存（演示重试）
        String cacheKey = CacheKeyBuilder.patientInfo(patientId);
        Boolean deleted = redisTemplate.delete(cacheKey);
        
        if (Boolean.FALSE.equals(deleted)) {
            throw new RuntimeException("Failed to delete cache for patient: " + patientId);
        }
        
        patientRepository.deleteById(patientId);
        log.info("Patient deleted: {}", patientId);
    }

    /**
     * 批量查询患者 - 演示缓存雪崩防护
     * 为不同的患者 ID 设置不同的过期时间偏移
     */
    public Optional<Patient> getPatientWithAntiAvalanche(String patientId) {
        String cacheKey = CacheKeyBuilder.patientInfo(patientId);
        
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !CacheUtils.isNullValue(cached)) {
            return Optional.of((Patient) cached);
        }
        
        Optional<Patient> patient = patientRepository.findById(patientId);
        
        if (patient.isPresent()) {
            // 使用患者 ID 哈希作为随机种子，确保相同 ID 总是获得相同的 TTL
            int seed = patientId.hashCode();
            long baseTTL = CacheTTL.PATIENT_DATA_UNIT.toMillis(CacheTTL.PATIENT_DATA);
            long jitter = (seed % 20 - 10) * baseTTL / 100; // ±10% 偏移
            long actualTTL = baseTTL + jitter;
            
            redisTemplate.opsForValue().set(
                cacheKey, 
                patient.get(), 
                actualTTL, 
                TimeUnit.MILLISECONDS
            );
            log.debug("Cache set with anti-avalanche TTL {}ms: {}", actualTTL, patientId);
        }
        
        return patient;
    }
}
