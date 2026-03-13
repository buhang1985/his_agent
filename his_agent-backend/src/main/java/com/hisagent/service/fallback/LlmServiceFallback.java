package com.hisagent.service.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM 服务降级实现
 * 降级链：主 LLM → 备用 LLM → 规则引擎 → 模板回复
 */
@Slf4j
@Component
public class LlmServiceFallback {

    /**
     * LLM 对话降级方法
     */
    public String chatFallback(String prompt, Throwable ex) {
        log.error("LLM service failed, triggering fallback: {}", ex.getMessage());
        
        // 降级策略 1: 尝试备用 LLM 服务
        try {
            log.info("Fallback 1: Trying backup LLM service...");
            return chatWithBackupLlm(prompt);
        } catch (Exception e) {
            log.warn("Fallback 1 failed: {}", e.getMessage());
        }
        
        // 降级策略 2: 使用规则引擎
        try {
            log.info("Fallback 2: Using rule engine...");
            return processWithRuleEngine(prompt);
        } catch (Exception e) {
            log.warn("Fallback 2 failed: {}", e.getMessage());
        }
        
        // 降级策略 3: 返回模板回复
        log.info("Fallback 3: Returning template response");
        return getTemplateResponse(prompt);
    }

    /**
     * 备用 LLM 服务
     */
    private String chatWithBackupLlm(String prompt) {
        // TODO: 实现备用 LLM 服务调用
        throw new RuntimeException("Backup LLM service not implemented yet");
    }

    /**
     * 规则引擎处理
     */
    private String processWithRuleEngine(String prompt) {
        // TODO: 实现简单规则引擎
        // 根据关键词匹配返回预设回复
        if (prompt.contains("你好") || prompt.contains("您好")) {
            return "您好，我是智能医疗助手，请问有什么可以帮助您的？";
        }
        if (prompt.contains("谢谢") || prompt.contains("感谢")) {
            return "不客气，祝您健康！";
        }
        throw new RuntimeException("No matching rule found");
    }

    /**
     * 模板回复
     */
    private String getTemplateResponse(String prompt) {
        return "抱歉，系统暂时无法处理您的问题。请稍后重试或联系人工客服。";
    }

    /**
     * 获取降级状态
     */
    public String getFallbackStatus() {
        return "LLM fallback chain: Primary LLM -> Backup LLM -> Rule Engine -> Template";
    }
}
