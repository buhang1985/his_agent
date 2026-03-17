// 符合中国医疗病历规范的结构

/**
 * 主诉
 */
export interface ChiefComplaint {
  content: string;
  duration?: string;
}

/**
 * 现病史
 */
export interface HistoryOfPresentIllness {
  onset: string;
  symptoms: string;
  progression: string;
  treatment: string;
}

/**
 * 既往史
 */
export interface PastMedicalHistory {
  pastDiseases: string;
  surgeries: string;
  allergies: string;
  medications: string;
}

/**
 * 体格检查（查体）
 */
export interface PhysicalExamination {
  vitalSigns: {
    temperature?: string;
    pulse?: string;
    respiration?: string;
    bloodPressure?: string;
  };
  generalExam: string;
  systemExams: Record<string, string>;
}

/**
 * 辅助检查
 */
export interface AuxiliaryExamination {
  labTests: string[];
  imagingTests: string[];
  otherTests: string[];
  results: string;
}

/**
 * 初步诊断
 */
export interface PreliminaryDiagnosis {
  primaryDiagnosis: string;
  icdCode?: string;
  confidence?: number;
}

/**
 * 鉴别诊断
 */
export interface DifferentialDiagnosis {
  diagnosis: string;
  supportEvidence: string;
  excludeEvidence: string;
  possibility: 'high' | 'medium' | 'low';
}

/**
 * 诊疗计划
 */
export interface TreatmentPlan {
  furtherTests: string[];
  medications: string[];
  nonDrugTreatment: string;
  followup: string;
  advice: string;
}

/**
 * 完整病历结构（符合中国医疗规范）
 */
export interface MedicalRecord {
  // 基本信息
  patientId?: string;
  visitId?: string;
  department?: string;
  doctorName?: string;
  recordTime?: string;

  // 病历内容
  chiefComplaint: ChiefComplaint;
  historyOfPresentIllness: HistoryOfPresentIllness;
  pastMedicalHistory: PastMedicalHistory;
  physicalExamination: PhysicalExamination;
  auxiliaryExamination: AuxiliaryExamination;
  preliminaryDiagnosis: PreliminaryDiagnosis;
  differentialDiagnoses: DifferentialDiagnosis[];
  treatmentPlan: TreatmentPlan;

  // 元数据
  isCompleted: boolean;
  isSigned: boolean;
  lastModified?: string;
}

/**
 * HIS 回写请求
 */
export interface HisWritebackRequest {
  patientId: string;
  visitId: string;
  recordType: 'outpatient' | 'emergency' | 'inpatient';
  section: string;
  content: string;
}

/**
 * HIS 回写响应
 */
export interface HisWritebackResponse {
  success: boolean;
  hisRecordId?: string;
  message: string;
  timestamp: string;
}

/**
 * 病历部分配置
 */
export interface RecordSectionConfig {
  key: keyof MedicalRecord;
  label: string;
  icon: string;
  placeholder: string;
  required: boolean;
  hasWriteback: boolean;
}
