package com.hisagent.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成病历请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSoapRequest {
    private String transcript;
    private String patientId;
    private String department;
}
