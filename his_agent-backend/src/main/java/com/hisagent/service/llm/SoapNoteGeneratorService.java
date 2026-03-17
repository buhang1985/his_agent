package com.hisagent.service.llm;

import com.hisagent.dto.voice.SoapNoteDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * SOAP 病历生成服务（简化测试版）
 * TODO: 生产环境需要集成真实的 LLM 服务
 */
@Slf4j
@Service
public class SoapNoteGeneratorService {

    public SoapNoteDTO generate(String transcript) {
        log.info("开始生成 SOAP 病历，转写长度：{}", transcript.length());

        try {
            // 简化实现：基于关键词生成基础病历
            // 生产环境应该调用 LLM API
            return generateFromKeywords(transcript);
            
        } catch (Exception e) {
            log.error("SOAP 病历生成失败", e);
            return createDefaultSoapNote(transcript);
        }
    }

    private SoapNoteDTO generateFromKeywords(String transcript) {
        String chiefComplaint = "待补充";
        String hpi = "待补充";
        String diagnosis = "待诊断";
        String physicalExam = "待检查";
        String treatment = "对症治疗";
        String advice = "如有不适随诊";

        // 简单关键词匹配
        if (transcript.contains("头痛") || transcript.contains("发烧")) {
            chiefComplaint = "头痛、发热";
            hpi = transcript;
            diagnosis = "上呼吸道感染";
            physicalExam = "咽部充血，扁桃体无肿大";
            treatment = "退热治疗，对症治疗，休息";
            advice = "多饮水，如有呼吸困难及时就医";
        }

        if (transcript.contains("咳嗽")) {
            chiefComplaint = chiefComplaint + "、咳嗽";
            physicalExam = physicalExam + "，双肺呼吸音粗，未闻及干湿啰音";
        }

        if (transcript.contains("腹痛") || transcript.contains("肚子痛")) {
            chiefComplaint = "腹痛";
            hpi = transcript;
            diagnosis = "急性胃肠炎？";
            physicalExam = "腹软，脐周压痛，无反跳痛";
            treatment = "禁食，补液，对症治疗";
            advice = "清淡饮食，如有加重及时就医";
        }

        if (transcript.contains("胸痛") || transcript.contains("胸闷")) {
            chiefComplaint = "胸痛、胸闷";
            hpi = transcript;
            diagnosis = "冠心病？心绞痛？";
            physicalExam = "心率 80 次/分，律齐，各瓣膜听诊区未闻及杂音";
            treatment = "硝酸甘油舌下含服，休息";
            advice = "避免剧烈运动，如有持续胸痛立即就医";
        }

        return SoapNoteDTO.builder()
            .subjective(SoapNoteDTO.Subjective.builder()
                .chiefComplaint(chiefComplaint)
                .historyOfPresentIllness(hpi)
                .build())
            .objective(SoapNoteDTO.Objective.builder()
                .vitalSigns("待测量")
                .physicalExamFindings(physicalExam)
                .build())
            .assessment(SoapNoteDTO.Assessment.builder()
                .primaryDiagnosis(diagnosis)
                .differentialDiagnoses(Arrays.asList("待鉴别"))
                .build())
            .plan(SoapNoteDTO.Plan.builder()
                .diagnosticTests(Arrays.asList("血常规", "C 反应蛋白"))
                .treatment(treatment)
                .advice(advice)
                .build())
            .build();
    }

    private SoapNoteDTO createDefaultSoapNote(String transcript) {
        return SoapNoteDTO.builder()
            .subjective(SoapNoteDTO.Subjective.builder()
                .chiefComplaint("待补充")
                .historyOfPresentIllness(transcript)
                .build())
            .objective(SoapNoteDTO.Objective.builder()
                .vitalSigns("待测量")
                .physicalExamFindings("待检查")
                .build())
            .assessment(SoapNoteDTO.Assessment.builder()
                .primaryDiagnosis("待诊断")
                .differentialDiagnoses(Arrays.asList("待鉴别"))
                .build())
            .plan(SoapNoteDTO.Plan.builder()
                .diagnosticTests(Arrays.asList("待开具"))
                .treatment("待制定")
                .advice("待提供")
                .build())
            .build();
    }
}
