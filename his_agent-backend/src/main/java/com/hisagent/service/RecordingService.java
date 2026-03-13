package com.hisagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 录音数据管理服务
 * 管理录音的存储、访问、清理等生命周期
 */
@Slf4j
@Service
public class RecordingService {

    @Value("${app.recording.storage-path:./recordings}")
    private String storagePath;

    @Value("${app.recording.max-size-mb:100}")
    private long maxSizeMb;

    @Value("${app.recording.retention-days:7}")
    private int retentionDays;

    // 内存缓存：consultationId -> audioPath
    private final ConcurrentHashMap<String, String> recordingCache = new ConcurrentHashMap<>();

    /**
     * 保存录音
     */
    public String saveRecording(MultipartFile file, String consultationId) throws IOException {
        // 检查文件大小
        if (file.getSize() > maxSizeMb * 1024 * 1024) {
            throw new IOException("Recording file too large: " + file.getSize() + " bytes");
        }

        // 创建存储目录
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        // 保存文件
        String filename = consultationId + "_" + System.currentTimeMillis() + ".wav";
        Path filePath = storageDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // 添加到缓存
        recordingCache.put(consultationId, filePath.toString());

        log.info("Recording saved: {} ({} bytes)", filePath, file.getSize());
        return filePath.toString();
    }

    /**
     * 从缓存获取录音路径
     */
    public String getRecordingPath(String consultationId) {
        return recordingCache.get(consultationId);
    }

    /**
     * 删除录音（病历生成后调用）
     */
    public void deleteRecording(String consultationId) {
        String path = recordingCache.remove(consultationId);
        if (path != null) {
            try {
                Files.deleteIfExists(Paths.get(path));
                log.info("Recording deleted for consultation: {}", consultationId);
            } catch (IOException e) {
                log.error("Failed to delete recording: {}", path, e);
            }
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAllRecordings() {
        recordingCache.clear();
        log.info("All recording cache cleared");
    }

    /**
     * 获取缓存统计
     */
    public RecordingStats getStats() {
        return new RecordingStats(
            recordingCache.size(),
            maxSizeMb,
            retentionDays
        );
    }

    /**
     * 录音统计
     */
    public record RecordingStats(int cachedCount, long maxSizeMb, int retentionDays) {}
}
