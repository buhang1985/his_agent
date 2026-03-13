package com.hisagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应基类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code = 200;
    private String message = "success";
    private T data;
    private String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, null);
    }

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(200, "success", data, traceId);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
