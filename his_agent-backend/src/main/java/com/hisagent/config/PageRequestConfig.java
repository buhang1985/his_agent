package com.hisagent.config;

import com.hisagent.exception.ErrorCode;
import com.hisagent.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 分页参数验证配置
 */
@RestControllerAdvice
public class PageRequestConfig implements WebMvcConfigurer, HandlerInterceptor {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 验证分页参数
        String pageStr = request.getParameter("page");
        String sizeStr = request.getParameter("size");

        if (pageStr != null) {
            try {
                int page = Integer.parseInt(pageStr);
                if (page < 0) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "page 参数必须 >= 0");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "page 参数格式错误");
            }
        }

        if (sizeStr != null) {
            try {
                int size = Integer.parseInt(sizeStr);
                if (size <= 0 || size > MAX_PAGE_SIZE) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, 
                        "size 参数必须在 1-" + MAX_PAGE_SIZE + " 之间");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "size 参数格式错误");
            }
        }

        return true;
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public com.hisagent.dto.ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("参数验证失败");
        
        return com.hisagent.dto.ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), errorMsg);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public com.hisagent.dto.ApiResponse<Void> handleBindException(BindException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("参数绑定失败");
        
        return com.hisagent.dto.ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), errorMsg);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public com.hisagent.dto.ApiResponse<Void> handleBusinessException(BusinessException e) {
        return com.hisagent.dto.ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
    }
}
