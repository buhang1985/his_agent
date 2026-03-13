package com.hisagent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 配置测试
 */
@SpringBootTest
public class RedisConfigTest {

    @Resource
    private ApplicationContext applicationContext;

    @Test
    void testRedisConfigExists() {
        // 测试 RedisConfig Bean 是否存在
        assertNotNull(applicationContext.getBean("redisConfig"));
    }

    @Test
    void testRedisTemplateExists() {
        // 测试 RedisTemplate Bean 是否存在
        assertNotNull(applicationContext.getBean("redisTemplate"));
    }
}
