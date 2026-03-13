package com.hisagent.dto.his;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HIS 患者 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HisPatientDTO {

    private String id;
    private String name;
    private String idCard;
    private String phone;
    private String gender;
    private Integer age;
    private String address;
    private Boolean synced;
}
