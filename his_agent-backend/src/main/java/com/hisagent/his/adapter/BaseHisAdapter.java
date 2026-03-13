package com.hisagent.his.adapter;

import com.hisagent.dto.his.HisPatientDTO;
import com.hisagent.dto.his.HisConsultationDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * HIS 适配器基类
 * 提供通用的实现和错误处理
 */
@Slf4j
public abstract class BaseHisAdapter implements HisAdapter {

    protected final String vendorName;
    protected final String baseUrl;
    protected final int timeout;

    protected BaseHisAdapter(String vendorName, String baseUrl, int timeout) {
        this.vendorName = vendorName;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    @Override
    public String getName() {
        return vendorName;
    }

    @Override
    public boolean supports(String hisVendor) {
        return vendorName.equalsIgnoreCase(hisVendor);
    }

    @Override
    public HisPatientDTO getPatient(String patientId) {
        log.info("{} HIS: Getting patient {}", vendorName, patientId);
        try {
            return doGetPatient(patientId);
        } catch (Exception e) {
            log.error("{} HIS: Failed to get patient {}", vendorName, patientId, e);
            throw new RuntimeException("Failed to get patient from " + vendorName + " HIS", e);
        }
    }

    @Override
    public HisPatientDTO getPatientByIdCard(String idCard) {
        log.info("{} HIS: Getting patient by ID card {}", vendorName, idCard);
        try {
            return doGetPatientByIdCard(idCard);
        } catch (Exception e) {
            log.error("{} HIS: Failed to get patient by ID card {}", vendorName, idCard, e);
            throw new RuntimeException("Failed to get patient by ID card from " + vendorName + " HIS", e);
        }
    }

    @Override
    public List<HisPatientDTO> searchPatients(String keyword) {
        log.info("{} HIS: Searching patients with keyword {}", vendorName, keyword);
        try {
            return doSearchPatients(keyword);
        } catch (Exception e) {
            log.error("{} HIS: Failed to search patients {}", vendorName, keyword, e);
            throw new RuntimeException("Failed to search patients from " + vendorName + " HIS", e);
        }
    }

    @Override
    public HisConsultationDTO createConsultation(HisConsultationDTO consultation) {
        log.info("{} HIS: Creating consultation for patient {}", vendorName, consultation.getPatientId());
        try {
            return doCreateConsultation(consultation);
        } catch (Exception e) {
            log.error("{} HIS: Failed to create consultation", vendorName, e);
            throw new RuntimeException("Failed to create consultation in " + vendorName + " HIS", e);
        }
    }

    @Override
    public HisConsultationDTO updateConsultation(HisConsultationDTO consultation) {
        log.info("{} HIS: Updating consultation {}", vendorName, consultation.getId());
        try {
            return doUpdateConsultation(consultation);
        } catch (Exception e) {
            log.error("{} HIS: Failed to update consultation {}", vendorName, consultation.getId(), e);
            throw new RuntimeException("Failed to update consultation in " + vendorName + " HIS", e);
        }
    }

    @Override
    public HisConsultationDTO getConsultation(String consultationId) {
        log.info("{} HIS: Getting consultation {}", vendorName, consultationId);
        try {
            return doGetConsultation(consultationId);
        } catch (Exception e) {
            log.error("{} HIS: Failed to get consultation {}", vendorName, consultationId, e);
            throw new RuntimeException("Failed to get consultation from " + vendorName + " HIS", e);
        }
    }

    @Override
    public boolean testConnection() {
        log.info("{} HIS: Testing connection to {}", vendorName, baseUrl);
        try {
            return doTestConnection();
        } catch (Exception e) {
            log.error("{} HIS: Connection test failed", vendorName, e);
            return false;
        }
    }

    // 子类必须实现的抽象方法

    protected abstract HisPatientDTO doGetPatient(String patientId);

    protected abstract HisPatientDTO doGetPatientByIdCard(String idCard);

    protected abstract List<HisPatientDTO> doSearchPatients(String keyword);

    protected abstract HisConsultationDTO doCreateConsultation(HisConsultationDTO consultation);

    protected abstract HisConsultationDTO doUpdateConsultation(HisConsultationDTO consultation);

    protected abstract HisConsultationDTO doGetConsultation(String consultationId);

    protected abstract boolean doTestConnection();
}
