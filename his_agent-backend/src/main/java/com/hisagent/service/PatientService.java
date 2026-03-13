package com.hisagent.service;

import com.hisagent.dto.PatientDTO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 患者服务
 */
@Service
public class PatientService {

    /**
     * 获取所有患者（模拟数据）
     */
    public List<PatientDTO> getAllPatients() {
        return getMockPatients();
    }

    /**
     * 根据 ID 获取患者
     */
    public PatientDTO getPatientById(String id) {
        return getMockPatients().stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Patient not found: " + id));
    }

    /**
     * 模拟患者数据
     */
    private List<PatientDTO> getMockPatients() {
        return Arrays.asList(
            PatientDTO.builder()
                .id("P001")
                .name("张三")
                .idCard("110101199001011234")
                .phone("138****5678")
                .gender("男")
                .age(34)
                .address("北京市朝阳区")
                .build(),
            PatientDTO.builder()
                .id("P002")
                .name("李四")
                .idCard("110101198502022345")
                .phone("139****9999")
                .gender("女")
                .age(39)
                .address("北京市海淀区")
                .build(),
            PatientDTO.builder()
                .id("P003")
                .name("王五")
                .idCard("110101199503033456")
                .phone("137****8888")
                .gender("男")
                .age(29)
                .address("北京市西城区")
                .build(),
            PatientDTO.builder()
                .id("P004")
                .name("赵六")
                .idCard("110101200004044567")
                .phone("136****7777")
                .gender("女")
                .age(24)
                .address("北京市东城区")
                .build(),
            PatientDTO.builder()
                .id("P005")
                .name("孙七")
                .idCard("110101198805055678")
                .phone("135****6666")
                .gender("男")
                .age(36)
                .address("北京市丰台区")
                .build()
        );
    }
}
