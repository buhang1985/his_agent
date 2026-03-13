package com.hisagent.his.adapter;

import com.hisagent.dto.his.HisPatientDTO;
import com.hisagent.dto.his.HisConsultationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用 REST HIS 适配器
 * 适用于提供 RESTful API 的 HIS 厂商
 */
@Slf4j
@Component
public class RestHisAdapter extends BaseHisAdapter {

    public RestHisAdapter() {
        super("REST", "http://localhost:8088", 30000);
    }

    @Override
    protected HisPatientDTO doGetPatient(String patientId) {
        // TODO: 实现 REST API 调用
        // 示例：RestTemplate 或 WebClient 调用 HIS REST 接口
        return HisPatientDTO.builder()
            .id(patientId)
            .name("测试患者")
            .idCard("110101199001011234")
            .phone("13800138000")
            .gender("男")
            .age(30)
            .build();
    }

    @Override
    protected HisPatientDTO doGetPatientByIdCard(String idCard) {
        // TODO: 实现根据身份证号查询
        return HisPatientDTO.builder()
            .id("REST_" + idCard)
            .name("测试患者")
            .idCard(idCard)
            .build();
    }

    @Override
    protected List<HisPatientDTO> doSearchPatients(String keyword) {
        List<HisPatientDTO> patients = new ArrayList<>();
        // TODO: 实现搜索逻辑
        patients.add(HisPatientDTO.builder()
            .id("REST_1")
            .name("张三")
            .build());
        return patients;
    }

    @Override
    protected HisConsultationDTO doCreateConsultation(HisConsultationDTO consultation) {
        // TODO: 实现创建问诊记录
        consultation.setId("REST_CONSULTATION_" + System.currentTimeMillis());
        consultation.setSynced(true);
        return consultation;
    }

    @Override
    protected HisConsultationDTO doUpdateConsultation(HisConsultationDTO consultation) {
        // TODO: 实现更新问诊记录
        consultation.setSynced(true);
        return consultation;
    }

    @Override
    protected HisConsultationDTO doGetConsultation(String consultationId) {
        // TODO: 实现查询问诊记录
        return HisConsultationDTO.builder()
            .id(consultationId)
            .patientId("PATIENT_1")
            .status("completed")
            .build();
    }

    @Override
    protected boolean doTestConnection() {
        // TODO: 实现连接测试
        // 可以调用 HIS 的健康检查接口
        return true;
    }
}
