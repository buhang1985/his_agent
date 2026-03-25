package com.hisagent.service;

import com.hisagent.cache.CacheKeyBuilder;
import com.hisagent.cache.CacheTTL;
import com.hisagent.cache.CacheUtils;
import com.hisagent.model.Patient;
import com.hisagent.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceExample {

    private final PatientRepository patientRepository;

    @Cacheable(
        value = "patients",
        key = "#patientId",
        unless = "#result == null"
    )
    public Patient getPatientWithCache(String patientId) {
        log.debug("Querying patient from database: {}", patientId);
        
        return patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
    }

    public Optional<Patient> getPatientWithNullCache(String patientId) {
        String cacheKey = CacheKeyBuilder.patientInfo(patientId);
        
        Optional<Patient> patient = patientRepository.findById(patientId);
        
        if (patient.isPresent()) {
            long ttl = CacheUtils.calculateTTLWithJitter(
                CacheTTL.PATIENT_DATA, 
                CacheTTL.PATIENT_DATA_UNIT
            );
            log.debug("Cache miss (using cache manager): {}", patientId);
        } else {
            log.debug("Cache miss (null result): {}", patientId);
        }
        
        return patient;
    }

    @Caching(evict = {
        @CacheEvict(value = "patients", key = "#patient.id"),
        @CacheEvict(value = "patientInfo", key = "#patient.id")
    })
    public Patient updatePatient(Patient patient) {
        log.info("Updating patient: {}", patient.getId());
        
        Patient updated = patientRepository.save(patient);
        
        log.info("Patient updated and cache invalidated: {}", patient.getId());
        
        return updated;
    }

    @CacheEvict(value = "patients", key = "#patientId")
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public void deletePatientWithRetry(String patientId) {
        log.info("Deleting patient: {}", patientId);
        
        patientRepository.deleteById(patientId);
        log.info("Patient deleted: {}", patientId);
    }

    public Optional<Patient> getPatientWithAntiAvalanche(String patientId) {
        Optional<Patient> patient = patientRepository.findById(patientId);
        
        if (patient.isPresent()) {
            log.debug("Cache miss (using cache manager): {}", patientId);
        }
        
        return patient;
    }
}
