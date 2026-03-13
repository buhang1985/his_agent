package com.hisagent.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存配置测试
 */
@SpringBootTest
public class CacheConfigTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRedisConnection() {
        // 测试 Redis 连接
        redisTemplate.opsForValue().set("test:key", "test-value");
        String value = (String) redisTemplate.opsForValue().get("test:key");
        assertEquals("test-value", value);
        
        // 清理测试数据
        redisTemplate.delete("test:key");
    }

    @Test
    void testCacheKeyBuilder() {
        // 测试缓存键构建器
        assertEquals("his_agent:patient:info:123", CacheKeyBuilder.patientInfo("123"));
        assertEquals("his_agent:consultation:abc-456", CacheKeyBuilder.consultation("abc-456"));
        assertEquals("his_agent:user:session:user-789", CacheKeyBuilder.userSession("user-789"));
        assertEquals("his_agent:medical:term:drug", CacheKeyBuilder.medicalTerm("drug"));
        assertEquals("his_agent:config:app-version", CacheKeyBuilder.config("app-version"));
        assertEquals("his_agent:api:response:/api/v1/patients", CacheKeyBuilder.apiResponse("/api/v1/patients"));
    }

    @Test
    void testCacheTTL() {
        // 测试缓存过期时间常量
        assertEquals(30, CacheTTL.PATIENT_DATA);
        assertEquals(2, CacheTTL.USER_SESSION);
        assertEquals(24, CacheTTL.MEDICAL_TERM);
        assertEquals(1, CacheTTL.CONFIG_DATA);
        assertEquals(10, CacheTTL.API_RESPONSE);
        assertEquals(5, CacheTTL.NULL_VALUE);
    }
}
