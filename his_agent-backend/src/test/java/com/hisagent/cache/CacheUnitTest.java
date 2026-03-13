package com.hisagent.cache;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存单元测试 - 不需要 Spring 上下文
 */
public class CacheUnitTest {

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
        assertEquals(1, CacheTTL.RECORDING);
    }

    @Test
    void testCacheTTLTimeUnits() {
        // 测试时间单位
        assertEquals(java.util.concurrent.TimeUnit.MINUTES, CacheTTL.PATIENT_DATA_UNIT);
        assertEquals(java.util.concurrent.TimeUnit.HOURS, CacheTTL.USER_SESSION_UNIT);
        assertEquals(java.util.concurrent.TimeUnit.HOURS, CacheTTL.MEDICAL_TERM_UNIT);
        assertEquals(java.util.concurrent.TimeUnit.HOURS, CacheTTL.CONFIG_DATA_UNIT);
        assertEquals(java.util.concurrent.TimeUnit.MINUTES, CacheTTL.API_RESPONSE_UNIT);
        assertEquals(java.util.concurrent.TimeUnit.MINUTES, CacheTTL.NULL_VALUE_UNIT);
        assertEquals(java.util.concurrent.TimeUnit.HOURS, CacheTTL.RECORDING_UNIT);
    }
}
