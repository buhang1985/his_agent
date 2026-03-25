package com.hisagent.service.llm;

import com.hisagent.dto.voice.SoapNoteDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * SOAP 病历生成服务（LLM 集成版）
 * 使用 Qwen3.5-plus 大模型生成结构化病历
 */
@Slf4j
@Service
public class SoapNoteGeneratorService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        你是一位经验丰富的临床医生，擅长根据医患对话生成结构化病历（SOAP 格式）。
        
        【要求】
        1. 严格按照 SOAP 格式组织内容
        2. 使用专业医学术语
        3. 保持客观、准确
        4. 不确定的信息标注"待确认"
        5. 识别医学术语和症状
        6. 提供合理的鉴别诊断建议
        
        【输出格式】
        必须返回标准 JSON 格式，不要包含 markdown 或其他说明文字：
        {
          "subjective": {
            "chiefComplaint": "主诉内容",
            "historyOfPresentIllness": "现病史详细描述"
          },
          "objective": {
            "vitalSigns": {
              "体温": "36.5°C",
              "脉搏": "80 次/分",
              "血压": "120/80 mmHg"
            },
            "physicalExamFindings": "体格检查发现"
          },
          "assessment": {
            "primaryDiagnosis": "初步诊断",
            "differentialDiagnoses": ["鉴别诊断 1", "鉴别诊断 2"]
          },
          "plan": {
            "diagnosticTests": ["检查检验项目"],
            "treatment": "治疗方案",
            "advice": "医嘱建议"
          }
        }
        
        如果某些信息在对话中没有提到，可以填写"待确认"或"待检查"。
        """;

    public SoapNoteDTO generate(String transcript) {
        log.info("开始生成 SOAP 病历，转写长度：{}，使用模型：{}", transcript.length(), model);

        try {
            // 调用 LLM API
            String llmResponse = callLLM(transcript);
            
            // 解析 JSON 响应
            SoapNoteDTO soapNote = parseLLMResponse(llmResponse);
            
            log.info("✅ SOAP 病历生成成功");
            return soapNote;
            
        } catch (Exception e) {
            log.error("SOAP 病历生成失败，降级到关键词匹配", e);
            return generateFromKeywords(transcript);
        }
    }

    private String callLLM(String transcript) {
        String url = baseUrl + "/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 系统提示词
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.add(systemMsg);
        
        // 用户对话内容
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "请根据以下医患对话生成 SOAP 病历：\n\n" + transcript);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        log.debug("调用 LLM API: {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode choices = rootNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    return choices.get(0).get("message").get("content").asText();
                }
            } catch (Exception e) {
                log.error("解析 LLM 响应失败", e);
            }
        }
        
        throw new RuntimeException("LLM API 调用失败：" + response.getStatusCode());
    }

    private SoapNoteDTO parseLLMResponse(String llmResponse) throws Exception {
        log.debug("LLM 原始响应：{}", llmResponse);
        
        // 清理响应（可能包含 markdown 代码块标记）
        String cleanJson = llmResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();
        
        JsonNode root = objectMapper.readTree(cleanJson);
        
        // 解析 subjective
        JsonNode subjectiveNode = root.get("subjective");
        SoapNoteDTO.Subjective subjective = null;
        if (subjectiveNode != null) {
            subjective = SoapNoteDTO.Subjective.builder()
                .chiefComplaint(getText(subjectiveNode.get("chiefComplaint")))
                .historyOfPresentIllness(getText(subjectiveNode.get("historyOfPresentIllness")))
                .build();
        }
        
        // 解析 objective
        JsonNode objectiveNode = root.get("objective");
        SoapNoteDTO.Objective objective = null;
        if (objectiveNode != null) {
            String vitalSignsStr = "待测量";
            JsonNode vitalSignsNode = objectiveNode.get("vitalSigns");
            if (vitalSignsNode != null && vitalSignsNode.isObject()) {
                Map<String, String> vitalsMap = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = vitalSignsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    vitalsMap.put(field.getKey(), field.getValue().asText());
                }
                if (!vitalsMap.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    vitalsMap.forEach((k, v) -> sb.append(k).append(": ").append(v).append(", "));
                    vitalSignsStr = sb.substring(0, sb.length() - 2);
                }
            }
            
            objective = SoapNoteDTO.Objective.builder()
                .vitalSigns(vitalSignsStr)
                .physicalExamFindings(getText(objectiveNode.get("physicalExamFindings")))
                .build();
        }
        
        // 解析 assessment
        JsonNode assessmentNode = root.get("assessment");
        SoapNoteDTO.Assessment assessment = null;
        if (assessmentNode != null) {
            List<String> differentialDiagnoses = new ArrayList<>();
            JsonNode diffDiagNode = assessmentNode.get("differentialDiagnoses");
            if (diffDiagNode != null && diffDiagNode.isArray()) {
                for (JsonNode node : diffDiagNode) {
                    differentialDiagnoses.add(node.asText());
                }
            }
            
            assessment = SoapNoteDTO.Assessment.builder()
                .primaryDiagnosis(getText(assessmentNode.get("primaryDiagnosis")))
                .differentialDiagnoses(differentialDiagnoses.isEmpty() ? Arrays.asList("待鉴别") : differentialDiagnoses)
                .build();
        }
        
        // 解析 plan
        JsonNode planNode = root.get("plan");
        SoapNoteDTO.Plan plan = null;
        if (planNode != null) {
            List<String> diagnosticTests = new ArrayList<>();
            JsonNode testsNode = planNode.get("diagnosticTests");
            if (testsNode != null && testsNode.isArray()) {
                for (JsonNode node : testsNode) {
                    diagnosticTests.add(node.asText());
                }
            }
            
            plan = SoapNoteDTO.Plan.builder()
                .diagnosticTests(diagnosticTests.isEmpty() ? Arrays.asList("待开具") : diagnosticTests)
                .treatment(getText(planNode.get("treatment")))
                .advice(getText(planNode.get("advice")))
                .build();
        }
        
        return SoapNoteDTO.builder()
            .subjective(subjective != null ? subjective : createDefaultSubjective())
            .objective(objective != null ? objective : createDefaultObjective())
            .assessment(assessment != null ? assessment : createDefaultAssessment())
            .plan(plan != null ? plan : createDefaultPlan())
            .build();
    }

    private String getText(JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : "待确认";
    }

    // 降级方法：基于关键词匹配
    private SoapNoteDTO generateFromKeywords(String transcript) {
        log.warn("降级到关键词匹配模式");
        return createDefaultSoapNote(transcript);
    }

    private SoapNoteDTO createDefaultSoapNote(String transcript) {
        return SoapNoteDTO.builder()
            .subjective(createDefaultSubjective())
            .objective(createDefaultObjective())
            .assessment(createDefaultAssessment())
            .plan(createDefaultPlan())
            .build();
    }

    private SoapNoteDTO.Subjective createDefaultSubjective() {
        return SoapNoteDTO.Subjective.builder()
            .chiefComplaint("待补充")
            .historyOfPresentIllness("待补充")
            .build();
    }

    private SoapNoteDTO.Objective createDefaultObjective() {
        return SoapNoteDTO.Objective.builder()
            .vitalSigns("待测量")
            .physicalExamFindings("待检查")
            .build();
    }

    private SoapNoteDTO.Assessment createDefaultAssessment() {
        return SoapNoteDTO.Assessment.builder()
            .primaryDiagnosis("待诊断")
            .differentialDiagnoses(Arrays.asList("待鉴别"))
            .build();
    }

    private SoapNoteDTO.Plan createDefaultPlan() {
        return SoapNoteDTO.Plan.builder()
            .diagnosticTests(Arrays.asList("待开具"))
            .treatment("待制定")
            .advice("待提供")
            .build();
    }
}
