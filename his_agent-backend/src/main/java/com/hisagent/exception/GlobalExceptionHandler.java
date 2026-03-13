package com.hisagent.exception;

import com.hisagent.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.warn("Validation error: {}", errors);
        return ApiResponse.error(400, "参数验证失败：" + errors, getTraceId());
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.warn("Bind error: {}", errors);
        return ApiResponse.error(400, "参数绑定失败：" + errors, getTraceId());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("Business error: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage(), getTraceId());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ApiResponse.error(400, "非法参数：" + e.getMessage(), getTraceId());
    }

    /**
     * 处理未授权异常
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleSecurityException(SecurityException e) {
        log.warn("Security error: {}", e.getMessage());
        return ApiResponse.error(401, "未授权：" + e.getMessage(), getTraceId());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.error(500, "服务器内部错误：" + e.getMessage(), getTraceId());
    }

    /**
     * 获取当前请求的 TraceId
     */
    private String getTraceId() {
        return MDC.get("traceId");
    }
}
