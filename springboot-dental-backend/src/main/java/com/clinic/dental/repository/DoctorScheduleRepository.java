package com.clinic.dental.repository;

import com.clinic.dental.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdAndWorkDate(Long doctorId, LocalDate workDate);

    Optional<DoctorSchedule> findByDoctorIdAndWorkDateAndTimeSlot(Long doctorId,
                                                                  LocalDate workDate,
                                                                  String timeSlot);
}
