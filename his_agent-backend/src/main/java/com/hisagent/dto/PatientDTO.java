package com.hisagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 患者 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {

    private String id;
    private String name;
    private String idCard;
    private String phone;
    private String gender;
    private Integer age;
    private String address;
}
