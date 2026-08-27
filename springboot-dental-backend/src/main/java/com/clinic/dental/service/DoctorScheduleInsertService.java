package com.clinic.dental.service;

import com.clinic.dental.entity.DoctorSchedule;
import com.clinic.dental.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorScheduleInsertService {

    private final DoctorScheduleRepository scheduleRepository;

    public DoctorScheduleInsertService(DoctorScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public void insert(List<DoctorSchedule> schedules) {
        scheduleRepository.saveAllAndFlush(schedules);
    }
}
