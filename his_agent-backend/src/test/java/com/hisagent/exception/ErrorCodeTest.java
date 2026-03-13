package com.hisagent.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 单元测试
 */
class ErrorCodeTest {

    @Test
    void testErrorCodeValues() {
        assertEquals(200, ErrorCode.SUCCESS.getCode());
        assertEquals(400, ErrorCode.BAD_REQUEST.getCode());
        assertEquals(500, ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void testErrorCodeMessages() {
        assertEquals("成功", ErrorCode.SUCCESS.getMessage());
        assertEquals("请求参数错误", ErrorCode.BAD_REQUEST.getMessage());
        assertEquals("服务器内部错误", ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    @Test
    void testFromCode() {
        assertEquals(ErrorCode.SUCCESS, ErrorCode.fromCode(200));
        assertEquals(ErrorCode.BAD_REQUEST, ErrorCode.fromCode(400));
        assertEquals(ErrorCode.NOT_FOUND, ErrorCode.fromCode(404));
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.fromCode(999));
    }

    @Test
    void testModuleCodes() {
        // 用户相关 200xxx
        assertEquals(200001, ErrorCode.USER_NOT_FOUND.getCode());
        // 患者相关 300xxx
        assertEquals(300001, ErrorCode.PATIENT_NOT_FOUND.getCode());
        // 问诊相关 400xxx
        assertEquals(400001, ErrorCode.CONSULTATION_NOT_FOUND.getCode());
    }
}
