package com.hisagent.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 生成病历响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSoapResponse {
    private String recordId;
    private SoapNoteDTO soap;
    private Double confidence;
    private List<String> lowConfidenceFields;
    private String generatedAt;
}
