// 患者信息接口
export interface Patient {
  id: string;
  name: string;
  gender: 'male' | 'female' | 'other';
  age: number;
  dateOfBirth?: string;
  medicalRecordNumber: string;
}

// 生命体征
export interface VitalSigns {
  temperature?: number; // 体温 (°C)
  bloodPressure?: {
    systolic: number; // 收缩压
    diastolic: number; // 舒张压
  };
  heartRate?: number; // 心率 (bpm)
  respiratoryRate?: number; // 呼吸频率 (rpm)
  oxygenSaturation?: number; // 血氧饱和度 (%)
}

// 主诉
export interface ChiefComplaint {
  symptom: string;
  duration: string;
  severity?: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10;
  associatedSymptoms?: string[];
}

// 现病史
export interface HistoryOfPresentIllness {
  onset: string;
  progression: string;
  aggravatingFactors?: string[];
  relievingFactors?: string[];
  associatedSymptoms?: string[];
  previousTreatments?: string[];
}

// 诊断建议
export interface DiagnosisSuggestion {
  condition: string;
  likelihood: 'high' | 'medium' | 'low';
  icd10Code?: string;
  supportingEvidence: string[];
  recommendedTests: string[];
  differentialNotes?: string;
}

// 检查建议
export interface RecommendedTest {
  name: string;
  type: 'lab' | 'imaging' | 'procedure' | 'other';
  priority: 'urgent' | 'routine' | 'optional';
  rationale: string;
}

// 治疗方案
export interface TreatmentPlan {
  medications?: PrescribedMedication[];
  procedures?: string[];
  lifestyleRecommendations?: string[];
  followUp?: FollowUpPlan;
}

// 处方药品
export interface PrescribedMedication {
  name: string;
  dosage: string;
  frequency: string;
  route?: 'oral' | 'intravenous' | 'intramuscular' | 'topical' | 'other';
  duration: string;
  instructions?: string;
}

// 随访计划
export interface FollowUpPlan {
  timeframe: string;
  reason: string;
  type: 'in-person' | 'telemedicine' | 'phone' | 'as-needed';
}

// 结构化病历 (SOAP 格式)
export interface SOAPNote {
  subjective: {
    chiefComplaint: ChiefComplaint;
    historyOfPresentIllness: HistoryOfPresentIllness;
    reviewOfSystems?: Record<string, string>;
  };
  objective: {
    vitalSigns: VitalSigns;
    physicalExamFindings: Record<string, string>;
    labResults?: Record<string, unknown>;
  };
  assessment: {
    primaryDiagnosis: DiagnosisSuggestion;
    differentialDiagnoses: DiagnosisSuggestion[];
  };
  plan: {
    diagnosticTests: RecommendedTest[];
    treatment: TreatmentPlan;
    patientEducation?: string[];
  };
}

// 医患对话转写片段
export interface ConversationSegment {
  id: string;
  speaker: 'doctor' | 'patient';
  text: string;
  timestamp: number;
  confidence: number;
}

// 语音识别状态
export interface SpeechRecognitionState {
  isListening: boolean;
  isProcessing: boolean;
  error: string | null;
  segments: ConversationSegment[];
  fullTranscript: string;
}

// LLM 提供商类型
export type LLMProvider = 
  | 'claude' 
  | 'openai' 
  | 'azure' 
  | 'ollama' 
  | 'vllm'
  | 'qwen'
  | 'chatglm';

// LLM 配置
export interface LLMConfig {
  provider: LLMProvider;
  model: string;
  apiKey?: string;
  baseUrl?: string;
  maxTokens?: number;
  temperature?: number;
}

// HIS 通信消息
export interface HISMessage {
  type: 'PATIENT_INFO' | 'SAVE_RECORD' | 'OPEN_RECORD' | 'ERROR';
  payload: {
    patientId?: string;
    recordData?: Partial<SOAPNote>;
    recordId?: string;
    error?: string;
  };
}
