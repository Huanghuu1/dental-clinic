package com.clinic.dental.repository;

import com.clinic.dental.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByStatus(String status);

    List<Appointment> findByDate(LocalDate date);

    long countByDoctorIdAndDateAndTimeAndStatusNot(Long doctorId,
                                                   LocalDate date,
                                                   String time,
                                                   String status);
}
