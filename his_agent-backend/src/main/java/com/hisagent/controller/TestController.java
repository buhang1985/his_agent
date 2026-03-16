package com.hisagent.controller;

import com.hisagent.dto.ApiResponse;
import com.hisagent.dto.voice.GenerateSoapRequest;
import com.hisagent.dto.voice.GenerateSoapResponse;
import com.hisagent.dto.voice.SoapNoteDTO;
import com.hisagent.model.Patient;
import com.hisagent.repository.PatientRepository;
import com.hisagent.cache.CacheKeyBuilder;
import com.hisagent.cache.CacheUtils;
import com.hisagent.service.llm.SoapNoteGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 测试控制器
 * 用于测试数据库 CRUD、Redis 缓存和前后端通信
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TestController {

    private final PatientRepository patientRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SoapNoteGeneratorService soapNoteGeneratorService;

    /**
     * 测试数据库连接
     */
    @GetMapping("/db/connection")
    public ApiResponse<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "connected");
        result.put("database", "MySQL");
        result.put("count", patientRepository.count());
        result.put("message", "数据库连接成功");
        return ApiResponse.success(result);
    }

    /**
     * 创建测试患者（Create）
     */
    @PostMapping("/db/patient")
    public ApiResponse<Patient> createPatient(@RequestBody Patient patient) {
        log.info("创建患者：{}", patient.getName());
        Patient saved = patientRepository.save(patient);
        return ApiResponse.success(saved);
    }

    /**
     * 查询所有患者（Read）
     */
    @GetMapping("/db/patients")
    public ApiResponse<List<Patient>> getAllPatients() {
        log.info("查询所有患者");
        List<Patient> patients = patientRepository.findAll();
        return ApiResponse.success(patients);
    }

    /**
     * 根据 ID 查询患者（Read）
     */
    @GetMapping("/db/patient/{id}")
    public ApiResponse<Patient> getPatient(@PathVariable String id) {
        log.info("查询患者 ID: {}", id);
        return patientRepository.findById(id)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error(404, "患者不存在"));
    }

    /**
     * 更新患者（Update）
     */
    @PutMapping("/db/patient/{id}")
    public ApiResponse<Patient> updatePatient(
            @PathVariable String id,
            @RequestBody Patient patient) {
        log.info("更新患者 ID: {}", id);
        
        return patientRepository.findById(id)
            .map(existing -> {
                existing.setName(patient.getName());
                existing.setPhone(patient.getPhone());
                existing.setAddress(patient.getAddress());
                Patient updated = patientRepository.save(existing);
                return ApiResponse.success(updated);
            })
            .orElse(ApiResponse.error(404, "患者不存在"));
    }

    /**
     * 删除患者（Delete）
     */
    @DeleteMapping("/db/patient/{id}")
    public ApiResponse<Void> deletePatient(@PathVariable String id) {
        log.info("删除患者 ID: {}", id);
        if (patientRepository.existsById(id)) {
            patientRepository.deleteById(id);
            return ApiResponse.success(null);
        }
        return ApiResponse.error(404, "患者不存在");
    }

    /**
     * 批量插入测试数据
     */
    @PostMapping("/db/batch-insert")
    public ApiResponse<Map<String, Object>> batchInsert() {
        log.info("批量插入测试数据");
        
        List<Patient> patients = Arrays.asList(
            createTestPatient("测试 1", "13800138001", "北京市测试区 1 号"),
            createTestPatient("测试 2", "13800138002", "北京市测试区 2 号"),
            createTestPatient("测试 3", "13800138003", "北京市测试区 3 号"),
            createTestPatient("测试 4", "13800138004", "北京市测试区 4 号"),
            createTestPatient("测试 5", "13800138005", "北京市测试区 5 号")
        );
        
        patientRepository.saveAll(patients);
        
        Map<String, Object> result = new HashMap<>();
        result.put("inserted", patients.size());
        result.put("total", patientRepository.count());
        
        return ApiResponse.success(result);
    }

    private Patient createTestPatient(String name, String phone, String address) {
        return Patient.builder()
            .id(UUID.randomUUID().toString())
            .name(name)
            .idCard("110101199001011234")
            .phone(phone)
            .gender("男")
            .age(30)
            .address(address)
            .build();
    }

    // ==================== Redis 缓存测试 ====================

    /**
     * 测试 Redis 连接
     */
    @GetMapping("/redis/connection")
    public ApiResponse<Map<String, Object>> testRedisConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            redisTemplate.opsForValue().set("test:connection", "OK", 10, TimeUnit.SECONDS);
            String value = (String) redisTemplate.opsForValue().get("test:connection");
            result.put("status", "connected");
            result.put("redis_version", redisTemplate.getConnectionFactory().getConnection().serverCommands().info().get("redis_version"));
            result.put("test_value", value);
            result.put("message", "Redis 连接成功");
            return ApiResponse.success(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Redis 连接失败：" + e.getMessage());
            return ApiResponse.error(500, result.get("message").toString());
        }
    }

    /**
     * 设置缓存（Create）
     */
    @PostMapping("/redis/cache")
    public ApiResponse<String> setCache(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false, defaultValue = "300") long ttl) {
        log.info("设置缓存：{} = {}", key, value);
        redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        return ApiResponse.success("缓存设置成功");
    }

    /**
     * 获取缓存（Read）
     */
    @GetMapping("/redis/cache/{key}")
    public ApiResponse<Object> getCache(@PathVariable String key) {
        log.info("获取缓存：{}", key);
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return ApiResponse.success(value);
        }
        return ApiResponse.error(404, "缓存不存在");
    }

    /**
     * 删除缓存（Delete）
     */
    @DeleteMapping("/redis/cache/{key}")
    public ApiResponse<Void> deleteCache(@PathVariable String key) {
        log.info("删除缓存：{}", key);
        redisTemplate.delete(key);
        return ApiResponse.success(null);
    }

    /**
     * 测试缓存穿透防护
     */
    @GetMapping("/redis/cache-penetration")
    public ApiResponse<Map<String, Object>> testCachePenetration() {
        String key = "test:penetration:" + System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        // 1. 设置空值缓存
        redisTemplate.opsForValue().set(key, CacheUtils.getNullValueMarker(), 60, TimeUnit.SECONDS);
        
        // 2. 读取并验证
        Object value = redisTemplate.opsForValue().get(key);
        boolean isNullValue = CacheUtils.isNullValue(value);
        
        result.put("key", key);
        result.put("value", value);
        result.put("is_null_value", isNullValue);
        result.put("message", isNullValue ? "缓存穿透防护生效" : "正常缓存");
        
        return ApiResponse.success(result);
    }

    /**
     * 测试缓存雪崩防护（随机 TTL）
     */
    @GetMapping("/redis/cache-avalanche")
    public ApiResponse<Map<String, Object>> testCacheAvalanche() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> caches = new ArrayList<>();
        
        long baseTTL = 300; // 5 分钟
        
        for (int i = 0; i < 5; i++) {
            String key = "test:avalanche:" + i;
            String value = "value_" + i;
            long jitterTTL = CacheUtils.calculateTTLWithJitter(baseTTL, TimeUnit.SECONDS, 0.2);
            
            redisTemplate.opsForValue().set(key, value, jitterTTL, TimeUnit.SECONDS);
            
            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("key", key);
            cacheInfo.put("value", value);
            cacheInfo.put("ttl_seconds", jitterTTL);
            caches.add(cacheInfo);
        }
        
        result.put("caches", caches);
        result.put("base_ttl", baseTTL);
        result.put("jitter_range", "±20%");
        result.put("message", "缓存雪崩防护生效 - 随机 TTL");
        
        return ApiResponse.success(result);
    }

    /**
     * 清空所有测试缓存
     */
    @DeleteMapping("/redis/cache-clear")
    public ApiResponse<Void> clearTestCache() {
        log.info("清空所有测试缓存");
        Set<String> keys = redisTemplate.keys("test:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        return ApiResponse.success(null);
    }

    /**
     * 获取所有测试缓存
     */
    @GetMapping("/redis/cache-list")
    public ApiResponse<Map<String, Object>> getTestCacheList() {
        Set<String> keys = redisTemplate.keys("test:*");
        Map<String, Object> result = new HashMap<>();
        
        if (keys != null) {
            List<String> keyList = new ArrayList<>(keys);
            result.put("count", keyList.size());
            result.put("keys", keyList);
        } else {
            result.put("count", 0);
            result.put("keys", new ArrayList<>());
        }
        
        return ApiResponse.success(result);
    }

    // ==================== 前后端通信测试 ====================

    /**
     * 测试前后端通信
     */
    @GetMapping("/communication/ping")
    public ApiResponse<Map<String, Object>> testCommunication() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        result.put("message", "后端响应正常");
        result.put("status", "success");
        return ApiResponse.success(result);
    }

    /**
     * 测试请求响应时间
     */
    @GetMapping("/communication/latency")
    public ApiResponse<Map<String, Object>> testLatency() {
        long startTime = System.currentTimeMillis();
        
        // 模拟一些处理
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        long latency = endTime - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("start_time", startTime);
        result.put("end_time", endTime);
        result.put("latency_ms", latency);
        result.put("message", "请求响应时间测试");
        
        return ApiResponse.success(result);
    }

    /**
     * 综合测试（数据库 + 缓存）
     */
    @GetMapping("/comprehensive")
    public ApiResponse<Map<String, Object>> comprehensiveTest() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 测试数据库
            long dbStart = System.currentTimeMillis();
            long patientCount = patientRepository.count();
            long dbTime = System.currentTimeMillis() - dbStart;
            
            Map<String, Object> dbResult = new HashMap<>();
            dbResult.put("status", "success");
            dbResult.put("count", patientCount);
            dbResult.put("time_ms", dbTime);
            
            // 2. 测试 Redis
            long redisStart = System.currentTimeMillis();
            String cacheKey = "test:comprehensive:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(cacheKey, "OK", 60, TimeUnit.SECONDS);
            Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
            long redisTime = System.currentTimeMillis() - redisStart;
            
            Map<String, Object> redisResult = new HashMap<>();
            redisResult.put("status", "success");
            redisResult.put("value", cacheValue);
            redisResult.put("time_ms", redisTime);
            
            // 3. 汇总结果
            long totalTime = System.currentTimeMillis() - startTime;
            
            result.put("database", dbResult);
            result.put("redis", redisResult);
            result.put("total_time_ms", totalTime);
            result.put("timestamp", System.currentTimeMillis());
            result.put("message", "综合测试成功");
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("综合测试失败", e);
            return ApiResponse.error(500, "综合测试失败：" + e.getMessage());
        }
    }

    // ==================== 语音测试接口 ====================

    /**
     * 生成 SOAP 病历
     */
    @PostMapping("/voice/generate")
    public ApiResponse<GenerateSoapResponse> generateSoapNote(@RequestBody GenerateSoapRequest request) {
        log.info("生成 SOAP 病历，转写长度：{}", request.getTranscript().length());

        try {
            SoapNoteDTO soapNote = soapNoteGeneratorService.generate(request.getTranscript());

            GenerateSoapResponse response = GenerateSoapResponse.builder()
                .recordId(UUID.randomUUID().toString())
                .soap(soapNote)
                .confidence(0.85)
                .lowConfidenceFields(new ArrayList<>())
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("生成 SOAP 病历失败", e);
            return ApiResponse.error(500, "生成失败：" + e.getMessage());
        }
    }
}
