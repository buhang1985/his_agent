package com.hisagent.dto.his;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * HIS 问诊 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HisConsultationDTO {

    private String id;
    private String patientId;
    private String doctorId;
    private String status;
    private String chiefComplaint;
    private String diagnosis;
    private LocalDateTime createdAt;
    private Boolean synced;
}
