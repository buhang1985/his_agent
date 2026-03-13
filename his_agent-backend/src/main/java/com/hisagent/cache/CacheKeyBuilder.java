package com.hisagent.cache;

import org.springframework.stereotype.Component;

/**
 * 缓存键构建器
 * 统一缓存键命名规范：his_agent:{模块}:{数据类型}:{ID}
 */
@Component
public class CacheKeyBuilder {
    
    private static final String PREFIX = "his_agent";
    
    /**
     * 构建患者信息缓存键
     * @param patientId 患者 ID
     * @return 缓存键
     */
    public static String patientInfo(String patientId) {
        return String.format("%s:patient:info:%s", PREFIX, patientId);
    }
    
    /**
     * 构建问诊会话缓存键
     * @param consultationId 问诊会话 ID
     * @return 缓存键
     */
    public static String consultation(String consultationId) {
        return String.format("%s:consultation:%s", PREFIX, consultationId);
    }
    
    /**
     * 构建用户会话缓存键
     * @param userId 用户 ID
     * @return 缓存键
     */
    public static String userSession(String userId) {
        return String.format("%s:user:session:%s", PREFIX, userId);
    }
    
    /**
     * 构建医学词库缓存键
     * @param termType 词库类型（disease, symptom, drug, test, procedure）
     * @return 缓存键
     */
    public static String medicalTerm(String termType) {
        return String.format("%s:medical:term:%s", PREFIX, termType);
    }
    
    /**
     * 构建配置缓存键
     * @param configKey 配置键
     * @return 缓存键
     */
    public static String config(String configKey) {
        return String.format("%s:config:%s", PREFIX, configKey);
    }
    
    /**
     * 构建 API 响应缓存键
     * @param apiPath API 路径
     * @return 缓存键
     */
    public static String apiResponse(String apiPath) {
        return String.format("%s:api:response:%s", PREFIX, apiPath);
    }
}
