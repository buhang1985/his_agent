package com.hisagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应用上下文测试
 */
@SpringBootTest
@ActiveProfiles("test")
class HisAgentApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能够正常加载
        assertTrue(true, "Application context should load successfully");
    }
}
