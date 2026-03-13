package com.hisagent.repository;

import com.hisagent.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 患者数据访问接口
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    /**
     * 根据身份证号查询患者
     */
    Optional<Patient> findByIdCard(String idCard);

    /**
     * 根据手机号查询患者
     */
    Optional<Patient> findByPhone(String phone);
}
