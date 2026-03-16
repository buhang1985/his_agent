// SOAP 病历结构
export interface SOAPNote {
  subjective: {
    chiefComplaint: string;
    historyOfPresentIllness: string;
  };
  objective: {
    vitalSigns: Record<string, any>;
    physicalExamFindings: string;
  };
  assessment: {
    primaryDiagnosis: string;
    differentialDiagnoses: string[];
  };
  plan: {
    diagnosticTests: string[];
    treatment: string;
    advice: string;
  };
}

// 对话片段
export interface ConversationSegment {
  id: string;
  speaker: 'doctor' | 'patient';
  text: string;
  timestamp: number;
  confidence: number;
}

// 语音识别结果
export interface RecognitionResult {
  text: string;
  isFinal: boolean;
  confidence: number;
  startTime: number;
  endTime: number;
}

// 问诊会话状态
export interface ConsultationSession {
  id: string;
  status: 'idle' | 'recording' | 'processing' | 'review' | 'completed';
  startTime: number;
  endTime?: number;
  segments: ConversationSegment[];
  fullTranscript: string;
  soapNote?: SOAPNote;
  error: string | null;
}

// 生成病历请求
export interface GenerateSoapRequest {
  transcript: string;
  patientId?: string;
  department?: string;
}

// 生成病历响应
export interface GenerateSoapResponse {
  recordId: string;
  soap: SOAPNote;
  confidence: number;
  lowConfidenceFields: string[];
  generatedAt: string;
}
