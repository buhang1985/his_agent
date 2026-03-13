package com.hisagent.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * 格式：XXXYYY
 * - XXX: 模块标识 (100=通用，200=用户，300=患者，400=问诊，500=语音，600=LLM)
 * - YYY: 具体错误
 */
public enum ErrorCode {

    // 使用常量记录 code 和 message

    // ========== 通用错误 (100) ==========
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // ========== 用户相关 (200) ==========
    USER_NOT_FOUND(200001, "用户不存在"),
    USER_ALREADY_EXISTS(200002, "用户已存在"),
    INVALID_CREDENTIALS(200003, "用户名或密码错误"),
    USER_LOCKED(200004, "用户已锁定"),
    TOKEN_EXPIRED(200005, "Token 已过期"),
    TOKEN_INVALID(200006, "Token 无效"),

    // ========== 患者相关 (300) ==========
    PATIENT_NOT_FOUND(300001, "患者不存在"),
    PATIENT_ALREADY_EXISTS(300002, "患者已存在"),
    INVALID_PATIENT_INFO(300003, "患者信息无效"),

    // ========== 问诊相关 (400) ==========
    CONSULTATION_NOT_FOUND(400001, "问诊会话不存在"),
    CONSULTATION_ALREADY_COMPLETED(400002, "问诊会话已完成"),
    INVALID_CONSULTATION_STATUS(400003, "问诊状态无效"),

    // ========== 语音相关 (500) ==========
    VOICE_RECOGNITION_FAILED(500001, "语音识别失败"),
    VOICE_SERVICE_UNAVAILABLE(500002, "语音服务不可用"),
    AUDIO_FORMAT_NOT_SUPPORTED(500003, "音频格式不支持"),

    // ========== LLM 相关 (600) ==========
    LLM_SERVICE_UNAVAILABLE(600001, "LLM 服务不可用"),
    LLM_TIMEOUT(600002, "LLM 请求超时"),
    LLM_RATE_LIMIT_EXCEEDED(600003, "LLM 请求超限"),

    // ========== HIS 集成 (700) ==========
    HIS_SERVICE_UNAVAILABLE(700001, "HIS 服务不可用"),
    HIS_TIMEOUT(700002, "HIS 请求超时"),
    HIS_RESPONSE_ERROR(700003, "HIS 响应错误"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据 code 获取 ErrorCode
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
