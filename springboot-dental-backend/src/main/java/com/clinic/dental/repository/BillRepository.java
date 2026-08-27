package com.clinic.dental.repository;

import com.clinic.dental.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByStatus(String status);

    List<Bill> findByPatientId(Long patientId);
}
