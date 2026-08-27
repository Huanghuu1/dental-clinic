package com.clinic.dental.config;

import com.clinic.dental.repository.DoctorRepository;
import com.clinic.dental.service.DoctorScheduleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 启动时初始化 6 位医生未来 14 天的基础号源。
 */
@Component
@Order(1)
public class DoctorScheduleInitializer implements CommandLineRunner {

    private final DoctorScheduleService scheduleService;
    private final DoctorRepository doctorRepository;

    public DoctorScheduleInitializer(DoctorScheduleService scheduleService,
                                     DoctorRepository doctorRepository) {
        this.scheduleService = scheduleService;
        this.doctorRepository = doctorRepository;
    }

    @Override
    public void run(String... args) {
        List<Long> doctorIds = doctorRepository.findAll().stream()
                .map(doctor -> doctor.getId())
                .filter(id -> id.longValue() >= 1L && id.longValue() <= 6L)
                .toList();
        scheduleService.initializeSchedules(doctorIds, LocalDate.now(), 14);
    }
}
