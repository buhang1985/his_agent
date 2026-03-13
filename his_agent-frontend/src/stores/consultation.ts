import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface Consultation {
  id: string;
  patientId: string;
  doctorId: string | null;
  status: 'pending' | 'in_progress' | 'completed' | 'cancelled';
  chiefComplaint: string | null;
  diagnosis: string | null;
  createdAt: string;
}

export interface ConsultationFilters {
  status?: string;
  patientId?: string;
  dateFrom?: string;
  dateTo?: string;
}

/**
 * 问诊会话 Store
 * 管理问诊列表、详情、状态等
 */
export const useConsultationStore = defineStore('consultation', () => {
  // State
  const consultations = ref<Consultation[]>([]);
  const currentConsultation = ref<Consultation | null>(null);
  const loading = ref(false);
  const total = ref(0);
  const filters = ref<ConsultationFilters>({});

  // Computed
  const pendingCount = computed(() => 
    consultations.value.filter(c => c.status === 'pending').length
  );

  const inProgressCount = computed(() => 
    consultations.value.filter(c => c.status === 'in_progress').length
  );

  // Actions
  function setConsultations(list: Consultation[], totalCount: number) {
    consultations.value = list;
    total.value = totalCount;
  }

  function setCurrentConsultation(consultation: Consultation | null) {
    currentConsultation.value = consultation;
  }

  function setLoading(value: boolean) {
    loading.value = value;
  }

  function setFilters(newFilters: ConsultationFilters) {
    filters.value = { ...filters.value, ...newFilters };
  }

  function resetFilters() {
    filters.value = {};
  }

  function addConsultation(consultation: Consultation) {
    consultations.value.unshift(consultation);
    total.value++;
  }

  function updateConsultationStatus(id: string, status: Consultation['status']) {
    const consultation = consultations.value.find(c => c.id === id);
    if (consultation) {
      consultation.status = status;
    }
    if (currentConsultation.value?.id === id) {
      currentConsultation.value.status = status;
    }
  }

  return {
    // State
    consultations,
    currentConsultation,
    loading,
    total,
    filters,
    // Computed
    pendingCount,
    inProgressCount,
    // Actions
    setConsultations,
    setCurrentConsultation,
    setLoading,
    setFilters,
    resetFilters,
    addConsultation,
    updateConsultationStatus,
  };
});
