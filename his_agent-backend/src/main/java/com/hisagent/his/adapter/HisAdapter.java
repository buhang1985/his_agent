package com.hisagent.his.adapter;

import com.hisagent.dto.his.HisPatientDTO;
import com.hisagent.dto.his.HisConsultationDTO;

import java.util.List;

/**
 * HIS 适配器接口
 * 所有 HIS 厂商适配器必须实现此接口
 */
public interface HisAdapter {

    /**
     * 获取适配器名称（厂商标识）
     */
    String getName();

    /**
     * 是否支持该 HIS 系统
     */
    boolean supports(String hisVendor);

    /**
     * 查询患者信息
     */
    HisPatientDTO getPatient(String patientId);

    /**
     * 根据身份证号查询患者
     */
    HisPatientDTO getPatientByIdCard(String idCard);

    /**
     * 搜索患者列表
     */
    List<HisPatientDTO> searchPatients(String keyword);

    /**
     * 创建问诊记录
     */
    HisConsultationDTO createConsultation(HisConsultationDTO consultation);

    /**
     * 更新问诊记录
     */
    HisConsultationDTO updateConsultation(HisConsultationDTO consultation);

    /**
     * 查询问诊记录
     */
    HisConsultationDTO getConsultation(String consultationId);

    /**
     * 测试连接
     */
    boolean testConnection();
}
