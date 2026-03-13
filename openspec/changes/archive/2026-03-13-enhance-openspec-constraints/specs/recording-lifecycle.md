# 录音数据生命周期规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：录音数据临时存储

录音数据必须临时存储在内存中，禁止持久化到磁盘。

#### 场景：内存缓存
- **当** 存储录音数据时
- **那么** 必须：
```java
@Service
public class RecordingStorageService {
    
    // 使用内存缓存（不落地）
    private final Cache<String, byte[]> recordingCache = 
        CacheBuilder.newBuilder()
            .maximumSize(1000)           // 最多 1000 个录音
            .maximumWeight(100 * 1024 * 1024)  // 最多 100MB
            .weigher((key, value) -> value.length)
            .expireAfterWrite(5, TimeUnit.MINUTES)  // 5 分钟后过期
            .removalListener(notification -> {
                // 记录删除日志
                log.info("录音数据已自动删除：{}", notification.getKey());
            })
            .build();
    
    // 存储录音
    public void storeRecording(String sessionId, byte[] audioData) {
        recordingCache.put(sessionId, audioData);
        log.info("录音已存储：sessionId={}, size={}", sessionId, audioData.length);
    }
    
    // 获取录音
    public byte[] getRecording(String sessionId) {
        return recordingCache.getIfPresent(sessionId);
    }
    
    // 删除录音
    public void deleteRecording(String sessionId) {
        recordingCache.invalidate(sessionId);
        log.info("录音已手动删除：sessionId={}", sessionId);
    }
}
```

#### 场景：存储限制
- **当** 配置存储限制时
- **那么** 必须：
  - 单个录音最大：10MB
  - 总会话数：1000 个
  - 总内存占用：100MB
  - 超出限制时自动淘汰最旧数据

### 需求：录音数据自动清理

录音数据必须在病历生成后立即自动删除。

#### 场景：清理触发
- **当** 病历生成完成后
- **那么** 必须：
```java
@Service
public class SOAPNoteGenerationService {
    
    @Autowired
    private RecordingStorageService recordingStorage;
    
    @Transactional
    public SOAPNote generateSOAPNote(String sessionId, String transcript) {
        try {
            // 生成病历
            SOAPNote soapNote = llmService.generate(transcript);
            
            // 保存病历
            soapNoteRepository.save(soapNote);
            
            // 立即删除录音
            recordingStorage.deleteRecording(sessionId);
            
            // 记录审计日志
            auditLogger.log("RECORDING_DELETED_AFTER_GENERATION",
                Map.of("sessionId", sessionId));
            
            return soapNote;
            
        } catch (Exception e) {
            log.error("病历生成失败", e);
            // 即使失败也要删除录音
            recordingStorage.deleteRecording(sessionId);
            throw e;
        }
    }
}
```

#### 场景：超时清理
- **当** 录音数据超时时
- **那么** 必须：
  - 5 分钟无访问自动删除
  - 问诊会话结束后删除
  - 系统关闭前全部删除

### 需求：录音数据访问审计

所有录音数据访问必须记录审计日志。

#### 场景：访问日志
- **当** 访问录音数据时
- **那么** 必须记录：
```java
public byte[] getRecording(String sessionId) {
    byte[] data = recordingCache.getIfPresent(sessionId);
    
    if (data != null) {
        // 记录访问日志
        auditLogger.log("RECORDING_ACCESSED",
            Map.of(
                "sessionId", sessionId,
                "userId", SecurityContextHolder.getUserId(),
                "timestamp", LocalDateTime.now(),
                "size", data.length
            ));
    }
    
    return data;
}
```

#### 场景：审计字段
- **当** 记录审计日志时
- **那么** 必须包含：
  - 访问时间（ISO 8601 格式）
  - 访问用户 ID
  - 会话 ID
  - 操作类型（访问/删除）
  - 录音大小
  - IP 地址

### 需求：录音数据不持久化

录音数据禁止以任何形式持久化到磁盘或数据库。

#### 场景：禁止持久化
- **当** 处理录音数据时
- **那么** 必须：
  - 禁止写入文件系统
  - 禁止存入数据库
  - 禁止上传到对象存储
  - 禁止记录到日志文件
  - 仅允许内存缓存

#### 场景：异常处理
- **当** 发生异常时
- **那么** 必须：
```java
@PreDestroy
public void cleanup() {
    // 应用关闭前清空所有录音
    recordingCache.invalidateAll();
    log.info("应用关闭，已清空所有录音数据");
}
```

### 需求：录音数据转写流程

录音数据必须在规定时间内完成转写并删除。

#### 场景：转写流程
- **当** 录音完成后
- **那么** 必须：
```
录音完成 → 上传到 ASR 服务 → 获取转写文本 → 删除录音 → 继续处理

时间约束:
- 录音完成到上传：< 1s
- ASR 转写时间：< 5s
- 转写完成到删除：< 1s
- 总耗时：< 10s
```

#### 场景：流程监控
- **当** 监控转写流程时
- **那么** 必须监控：
  - 录音存储数量
  - 平均存储时长
  - 转写成功率
  - 清理及时率
