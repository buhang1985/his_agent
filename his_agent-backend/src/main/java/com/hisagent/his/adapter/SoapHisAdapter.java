package com.hisagent.his.adapter;

import com.hisagent.dto.his.HisPatientDTO;
import com.hisagent.dto.his.HisConsultationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用 SOAP HIS 适配器
 * 适用于提供 SOAP Web Service 的 HIS 厂商
 */
@Slf4j
@Component
public class SoapHisAdapter extends BaseHisAdapter {

    public SoapHisAdapter() {
        super("SOAP", "http://localhost:8080/his-service", 30000);
    }

    @Override
    protected HisPatientDTO doGetPatient(String patientId) {
        // TODO: 实现 SOAP 调用
        // 示例：使用 JAX-WS 或 Spring Web Services 调用 HIS SOAP 接口
        return HisPatientDTO.builder()
            .id(patientId)
            .name("SOAP 测试患者")
            .idCard("110101199001011234")
            .phone("13800138000")
            .gender("男")
            .age(30)
            .build();
    }

    @Override
    protected HisPatientDTO doGetPatientByIdCard(String idCard) {
        // TODO: 实现 SOAP 根据身份证号查询
        return HisPatientDTO.builder()
            .id("SOAP_" + idCard)
            .name("SOAP 测试患者")
            .idCard(idCard)
            .build();
    }

    @Override
    protected List<HisPatientDTO> doSearchPatients(String keyword) {
        List<HisPatientDTO> patients = new ArrayList<>();
        // TODO: 实现 SOAP 搜索
        patients.add(HisPatientDTO.builder()
            .id("SOAP_1")
            .name("SOAP 张三")
            .build());
        return patients;
    }

    @Override
    protected HisConsultationDTO doCreateConsultation(HisConsultationDTO consultation) {
        // TODO: 实现 SOAP 创建问诊记录
        consultation.setId("SOAP_CONSULTATION_" + System.currentTimeMillis());
        consultation.setSynced(true);
        return consultation;
    }

    @Override
    protected HisConsultationDTO doUpdateConsultation(HisConsultationDTO consultation) {
        // TODO: 实现 SOAP 更新问诊记录
        consultation.setSynced(true);
        return consultation;
    }

    @Override
    protected HisConsultationDTO doGetConsultation(String consultationId) {
        // TODO: 实现 SOAP 查询问诊记录
        return HisConsultationDTO.builder()
            .id(consultationId)
            .patientId("SOAP_PATIENT_1")
            .status("completed")
            .build();
    }

    @Override
    protected boolean doTestConnection() {
        // TODO: 实现 SOAP 连接测试
        // 可以调用 HIS 的 ping 或 echo 服务
        return true;
    }
}
