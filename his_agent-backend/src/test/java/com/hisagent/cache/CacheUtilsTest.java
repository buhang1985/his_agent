package com.hisagent.cache;

import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存工具类单元测试
 */
public class CacheUtilsTest {

    @Test
    void testCalculateTTLWithJitter() {
        // 测试带随机偏移的 TTL 计算
        long baseTTL = 60; // 60 秒
        long result = CacheUtils.calculateTTLWithJitter(baseTTL, TimeUnit.SECONDS, 0.2);
        
        // 结果以毫秒为单位，应该在±20% 范围内（48000-72000 毫秒）
        long baseMillis = TimeUnit.SECONDS.toMillis(baseTTL);
        long minExpected = (long)(baseMillis * 0.8);
        long maxExpected = (long)(baseMillis * 1.2);
        
        assertTrue(result >= minExpected, "TTL should be at least " + minExpected + "ms");
        assertTrue(result <= maxExpected, "TTL should be at most " + maxExpected + "ms");
    }

    @Test
    void testCalculateTTLWithJitterDefault() {
        // 测试默认偏移（±20%）
        long baseTTL = 1800; // 30 分钟
        long result = CacheUtils.calculateTTLWithJitter(baseTTL, TimeUnit.SECONDS);
        
        long baseMillis = TimeUnit.SECONDS.toMillis(baseTTL);
        long minExpected = (long)(baseMillis * 0.8);
        long maxExpected = (long)(baseMillis * 1.2);
        
        assertTrue(result >= minExpected, "TTL should be at least " + minExpected + "ms");
        assertTrue(result <= maxExpected, "TTL should be at most " + maxExpected + "ms");
    }

    @Test
    void testCalculateTTLWithJitterZero() {
        // 测试 0 偏移
        long baseTTL = 60;
        long result = CacheUtils.calculateTTLWithJitter(baseTTL, TimeUnit.SECONDS, 0);
        
        assertEquals(baseTTL * 1000, result, "Zero offset should return exact TTL in milliseconds");
    }

    @Test
    void testCalculateTTLWithJitterMinimum() {
        // 测试最小值为 1 毫秒
        long result = CacheUtils.calculateTTLWithJitter(1, TimeUnit.NANOSECONDS, 0.5);
        
        assertTrue(result >= 1, "TTL should be at least 1 millisecond");
    }

    @Test
    void testCalculateTTLWithJitterInvalidOffset() {
        // 测试无效偏移值
        assertThrows(IllegalArgumentException.class, () -> {
            CacheUtils.calculateTTLWithJitter(60, TimeUnit.SECONDS, -0.1);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            CacheUtils.calculateTTLWithJitter(60, TimeUnit.SECONDS, 1.5);
        });
    }

    @Test
    void testIsNullValue() {
        // 测试空值判断
        assertTrue(CacheUtils.isNullValue(null));
        assertTrue(CacheUtils.isNullValue(CacheUtils.getNullValueMarker()));
        assertFalse(CacheUtils.isNullValue("some value"));
        assertFalse(CacheUtils.isNullValue(123));
    }

    @Test
    void testGetNullValueMarker() {
        // 测试空值标记
        String marker = CacheUtils.getNullValueMarker();
        assertNotNull(marker);
        assertEquals("__NULL__CACHE__VALUE__", marker);
    }

    @Test
    void testWrapAndUnwrapValue() {
        // 测试值包装和解包
        String original = "test value";
        Object wrapped = CacheUtils.wrapValue(original);
        String unwrapped = CacheUtils.unwrapValue(wrapped);
        
        assertEquals(original, unwrapped);
    }

    @Test
    void testWrapAndUnwrapNull() {
        // 测试 null 值的包装和解包
        Object wrapped = CacheUtils.wrapValue(null);
        String unwrapped = CacheUtils.unwrapValue(wrapped);
        
        assertNull(unwrapped);
        assertEquals(CacheUtils.getNullValueMarker(), wrapped);
    }
}
