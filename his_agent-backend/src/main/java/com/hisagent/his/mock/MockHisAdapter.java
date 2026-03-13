package com.hisagent.his.mock;

import com.hisagent.dto.his.HisPatientDTO;
import com.hisagent.dto.his.HisConsultationDTO;
import com.hisagent.his.adapter.BaseHisAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock HIS 测试服务
 * 用于开发和测试环境，模拟 HIS 接口响应
 */
@Profile({"dev", "test"})
@Component
public class MockHisAdapter extends BaseHisAdapter {

    private final ConcurrentHashMap<String, HisPatientDTO> patientStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HisConsultationDTO> consultationStore = new ConcurrentHashMap<>();

    public MockHisAdapter() {
        super("MOCK", "mock://localhost", 5000);

        // 初始化一些测试数据
        initTestData();
    }

    private void initTestData() {
        // 测试患者
        patientStore.put("MOCK_001", HisPatientDTO.builder()
            .id("MOCK_001")
            .name("张三")
            .idCard("110101199001011234")
            .phone("13800138000")
            .gender("男")
            .age(30)
            .address("北京市朝阳区")
            .synced(true)
            .build());

        patientStore.put("MOCK_002", HisPatientDTO.builder()
            .id("MOCK_002")
            .name("李四")
            .idCard("110101199002022345")
            .phone("13900139000")
            .gender("女")
            .age(25)
            .address("北京市海淀区")
            .synced(true)
            .build());

        // 测试问诊记录
        consultationStore.put("MOCK_C001", HisConsultationDTO.builder()
            .id("MOCK_C001")
            .patientId("MOCK_001")
            .doctorId("DOCTOR_001")
            .status("completed")
            .chiefComplaint("头痛")
            .diagnosis("感冒")
            .synced(true)
            .build());
    }

    @Override
    protected HisPatientDTO doGetPatient(String patientId) {
        simulateDelay(100);
        HisPatientDTO patient = patientStore.get(patientId);
        if (patient == null) {
            throw new RuntimeException("Patient not found: " + patientId);
        }
        return patient;
    }

    @Override
    protected HisPatientDTO doGetPatientByIdCard(String idCard) {
        simulateDelay(100);
        return patientStore.values().stream()
            .filter(p -> idCard.equals(p.getIdCard()))
            .findFirst()
            .orElse(null);
    }

    @Override
    protected List<HisPatientDTO> doSearchPatients(String keyword) {
        simulateDelay(200);
        return patientStore.values().stream()
            .filter(p -> p.getName().contains(keyword) ||
                        (p.getIdCard() != null && p.getIdCard().contains(keyword)))
            .limit(10)
            .toList();
    }

    @Override
    protected HisConsultationDTO doCreateConsultation(HisConsultationDTO consultation) {
        simulateDelay(150);
        consultation.setId("MOCK_C" + System.currentTimeMillis());
        consultation.setSynced(true);
        consultationStore.put(consultation.getId(), consultation);
        return consultation;
    }

    @Override
    protected HisConsultationDTO doUpdateConsultation(HisConsultationDTO consultation) {
        simulateDelay(150);
        consultation.setSynced(true);
        consultationStore.put(consultation.getId(), consultation);
        return consultation;
    }

    @Override
    protected HisConsultationDTO doGetConsultation(String consultationId) {
        simulateDelay(100);
        return consultationStore.get(consultationId);
    }

    @Override
    protected boolean doTestConnection() {
        simulateDelay(50);
        return true;
    }

    /**
     * 模拟网络延迟
     */
    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
