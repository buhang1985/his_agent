import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface Patient {
  id: string;
  name: string;
  idCard?: string;
  phone?: string;
  gender?: string;
  age?: number;
  address?: string;
}

/**
 * 患者 Store
 * 管理患者列表、详情、搜索等
 */
export const usePatientStore = defineStore('patient', () => {
  // State
  const patients = ref<Patient[]>([]);
  const currentPatient = ref<Patient | null>(null);
  const loading = ref(false);
  const searchKeyword = ref('');

  // Actions
  function setPatients(list: Patient[]) {
    patients.value = list;
  }

  function setCurrentPatient(patient: Patient | null) {
    currentPatient.value = patient;
  }

  function setLoading(value: boolean) {
    loading.value = value;
  }

  function setSearchKeyword(keyword: string) {
    searchKeyword.value = keyword;
  }

  function addPatient(patient: Patient) {
    patients.value.push(patient);
  }

  function updatePatient(id: string, updates: Partial<Patient>) {
    const patient = patients.value.find(p => p.id === id);
    if (patient) {
      Object.assign(patient, updates);
    }
    if (currentPatient.value?.id === id) {
      Object.assign(currentPatient.value, updates);
    }
  }

  return {
    // State
    patients,
    currentPatient,
    loading,
    searchKeyword,
    // Actions
    setPatients,
    setCurrentPatient,
    setLoading,
    setSearchKeyword,
    addPatient,
    updatePatient,
  };
});
