package com.hisagent.config;

import com.hisagent.service.RecordingService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 录音数据生命周期管理
 * - 应用关闭前清空所有录音
 * - 定期清理过期录音
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordingLifecycleConfig {

    private final RecordingService recordingService;

    /**
     * 应用关闭前清空所有录音
     */
    @PreDestroy
    public void onShutdown() {
        log.info("Application shutting down, clearing all recordings...");
        recordingService.clearAllRecordings();
        log.info("All recordings cleared");
    }

    /**
     * 每天凌晨 2 点清理过期录音
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredRecordings() {
        log.info("Running scheduled recording cleanup...");
        // TODO: 实现过期录音清理逻辑
        // 可以扫描存储目录，删除超过 retentionDays 的文件
        log.info("Recording cleanup completed");
    }
}
