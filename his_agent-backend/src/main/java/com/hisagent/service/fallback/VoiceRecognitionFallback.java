package com.hisagent.service.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 语音识别服务降级实现
 * 降级链：讯飞 → 阿里云 → Whisper → 手动录入
 */
@Slf4j
@Component
public class VoiceRecognitionFallback {

    /**
     * 语音转写降级方法
     * 当主服务（讯飞）失败时，依次尝试备用服务
     */
    public String transcribeFallback(byte[] audioData, Throwable ex) {
        log.error("Voice recognition failed, triggering fallback: {}", ex.getMessage());
        
        // 降级策略 1: 尝试阿里云语音识别
        try {
            log.info("Fallback 1: Trying Aliyun voice recognition...");
            return transcribeWithAliyun(audioData);
        } catch (Exception e) {
            log.warn("Fallback 1 failed: {}", e.getMessage());
        }
        
        // 降级策略 2: 尝试 Whisper 本地识别
        try {
            log.info("Fallback 2: Trying Whisper local recognition...");
            return transcribeWithWhisper(audioData);
        } catch (Exception e) {
            log.warn("Fallback 2 failed: {}", e.getMessage());
        }
        
        // 降级策略 3: 返回空结果，提示手动录入
        log.info("Fallback 3: All automatic recognition failed, suggesting manual entry");
        return null;
    }

    /**
     * 阿里云语音识别（备用方案 1）
     */
    private String transcribeWithAliyun(byte[] audioData) {
        throw new RuntimeException("Aliyun voice recognition not implemented yet");
    }

    /**
     * Whisper 本地识别（备用方案 2）
     */
    private String transcribeWithWhisper(byte[] audioData) {
        throw new RuntimeException("Whisper local recognition not implemented yet");
    }

    /**
     * 获取降级状态
     */
    public String getFallbackStatus() {
        return "Voice recognition fallback chain: iFlytek -> Aliyun -> Whisper -> Manual";
    }
}
