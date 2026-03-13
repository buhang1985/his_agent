package com.hisagent.controller;

import com.hisagent.dto.ApiResponse;
import com.hisagent.dto.PatientDTO;
import com.hisagent.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者管理控制器
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PatientController {

    private final PatientService patientService;

    /**
     * 获取患者列表
     */
    @GetMapping
    public ApiResponse<List<PatientDTO>> getPatients() {
        List<PatientDTO> patients = patientService.getAllPatients();
        return ApiResponse.success(patients);
    }

    /**
     * 根据 ID 获取患者
     */
    @GetMapping("/{id}")
    public ApiResponse<PatientDTO> getPatient(@PathVariable String id) {
        PatientDTO patient = patientService.getPatientById(id);
        return ApiResponse.success(patient);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("HIS Agent Backend is running!");
    }
}
