package com.clinic.dental.repository;

import com.clinic.dental.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** 按姓名模糊查询 */
    List<Patient> findByNameContaining(String name);

    /** 按电话精确查询 */
    List<Patient> findByPhone(String phone);
}
