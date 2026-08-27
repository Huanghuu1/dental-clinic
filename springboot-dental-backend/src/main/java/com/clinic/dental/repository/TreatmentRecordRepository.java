package com.clinic.dental.repository;

import com.clinic.dental.entity.TreatmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentRecordRepository extends JpaRepository<TreatmentRecord, Long> {

    List<TreatmentRecord> findByPatientId(Long patientId);

    List<TreatmentRecord> findByDoctorId(Long doctorId);
}
