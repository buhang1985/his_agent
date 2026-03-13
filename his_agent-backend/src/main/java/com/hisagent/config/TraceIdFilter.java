package com.hisagent.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器
 * 为每个请求生成唯一的追踪 ID，用于日志链路追踪
 */
@Slf4j
@Component
@Order(1)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // 获取或生成 TraceId
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }
        
        // 设置到 MDC 上下文
        MDC.put(TRACE_ID_KEY, traceId);
        
        try {
            // 将 TraceId 添加到响应头
            if (response instanceof jakarta.servlet.http.HttpServletResponse) {
                ((jakarta.servlet.http.HttpServletResponse) response)
                    .setHeader(TRACE_ID_HEADER, traceId);
            }
            
            // 继续处理请求
            chain.doFilter(request, response);
        } finally {
            // 清理 MDC
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 生成 TraceId
     * 格式：时间戳 -UUID 片段
     */
    private String generateTraceId() {
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return String.format("t%d-%s", timestamp, uuid);
    }
}
