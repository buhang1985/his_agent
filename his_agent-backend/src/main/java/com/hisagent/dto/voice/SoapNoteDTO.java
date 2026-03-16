package com.hisagent.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SOAP 病历 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoapNoteDTO {
    private Subjective subjective;
    private Objective objective;
    private Assessment assessment;
    private Plan plan;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subjective {
        private String chiefComplaint;
        private String historyOfPresentIllness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Objective {
        private String vitalSigns;
        private String physicalExamFindings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assessment {
        private String primaryDiagnosis;
        private List<String> differentialDiagnoses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Plan {
        private List<String> diagnosticTests;
        private String treatment;
        private String advice;
    }
}
