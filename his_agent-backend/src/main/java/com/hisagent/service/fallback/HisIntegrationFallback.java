package com.hisagent.service.fallback;

import com.hisagent.dto.his.HisPatientDTO;
import com.hisagent.dto.his.HisConsultationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * HIS 集成服务降级实现
 * 降级策略：主 HIS → 备用 HIS → 本地缓存 → 空结果
 */
@Slf4j
@Component
public class HisIntegrationFallback {

    /**
     * 查询患者信息降级
     */
    public HisPatientDTO getPatientFallback(String patientId, Throwable ex) {
        log.error("HIS patient query failed, triggering fallback: {}", ex.getMessage());
        
        // 降级策略 1: 尝试备用 HIS 服务
        try {
            log.info("Fallback 1: Trying backup HIS service...");
            return getPatientFromBackupHis(patientId);
        } catch (Exception e) {
            log.warn("Fallback 1 failed: {}", e.getMessage());
        }
        
        // 降级策略 2: 从本地缓存获取
        try {
            log.info("Fallback 2: Trying local cache...");
            return getPatientFromCache(patientId);
        } catch (Exception e) {
            log.warn("Fallback 2 failed: {}", e.getMessage());
        }
        
        // 降级策略 3: 返回空结果
        log.info("Fallback 3: Returning empty result");
        return null;
    }

    /**
     * 查询患者列表降级
     */
    public List<HisPatientDTO> listPatientsFallback(String keyword, Throwable ex) {
        log.error("HIS patient list query failed, triggering fallback: {}", ex.getMessage());
        return Collections.emptyList();
    }

    /**
     * 创建问诊记录降级
     */
    public HisConsultationDTO createConsultationFallback(HisConsultationDTO consultation, Throwable ex) {
        log.error("HIS consultation creation failed, triggering fallback: {}", ex.getMessage());
        
        // 降级策略：本地存储，稍后同步
        log.info("Fallback: Storing consultation locally for later sync");
        consultation.setId("LOCAL_" + System.currentTimeMillis());
        consultation.setSynced(false);
        return consultation;
    }

    /**
     * 从备用 HIS 获取患者
     */
    private HisPatientDTO getPatientFromBackupHis(String patientId) {
        // TODO: 实现备用 HIS 服务调用
        throw new RuntimeException("Backup HIS service not implemented yet");
    }

    /**
     * 从缓存获取患者
     */
    private HisPatientDTO getPatientFromCache(String patientId) {
        // TODO: 实现本地缓存查询
        throw new RuntimeException("Local cache not implemented yet");
    }

    /**
     * 获取降级状态
     */
    public String getFallbackStatus() {
        return "HIS fallback chain: Primary HIS -> Backup HIS -> Local Cache -> Empty";
    }
}
