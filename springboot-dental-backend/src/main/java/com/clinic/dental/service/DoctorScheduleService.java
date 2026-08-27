package com.clinic.dental.service;

import com.clinic.dental.entity.DoctorSchedule;
import com.clinic.dental.repository.DoctorRepository;
import com.clinic.dental.repository.DoctorScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class DoctorScheduleService {

    public static final List<String> DEFAULT_TIME_SLOTS = List.of(
            "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30"
    );

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleCreationService creationService;

    public DoctorScheduleService(DoctorScheduleRepository scheduleRepository,
                                 DoctorRepository doctorRepository,
                                 DoctorScheduleCreationService creationService) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.creationService = creationService;
    }

    /**
     * 查询当天排班；若缺少某些默认时间片，则通过独立事务动态补全。
     */
    public List<DoctorSchedule> getOrCreateDailySlots(Long doctorId, LocalDate workDate) {
        if (workDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "排班日期不能为空");
        }
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "医生不存在");
        }

        List<DoctorSchedule> existing = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
        List<String> existingTimes = existing.stream().map(DoctorSchedule::getTimeSlot).toList();
        List<String> missingTimes = DEFAULT_TIME_SLOTS.stream()
                .filter(timeSlot -> !existingTimes.contains(timeSlot))
                .toList();

        if (!missingTimes.isEmpty()) {
            creationService.createMissingSlots(doctorId, workDate, missingTimes);
            existing = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
        }

        return existing.stream()
                .sorted(Comparator.comparingInt(slot -> DEFAULT_TIME_SLOTS.indexOf(slot.getTimeSlot())))
                .toList();
    }

    /**
     * 为指定医生与日期范围补齐默认时间片，已存在的时间片不会重复插入。
     */
    public void initializeSchedules(List<Long> doctorIds, LocalDate startDate, int days) {
        for (Long doctorId : doctorIds) {
            for (int day = 0; day < days; day++) {
                LocalDate workDate = startDate.plusDays(day);
                List<String> existingTimes = scheduleRepository
                        .findByDoctorIdAndWorkDate(doctorId, workDate)
                        .stream()
                        .map(DoctorSchedule::getTimeSlot)
                        .toList();
                List<String> missingTimes = DEFAULT_TIME_SLOTS.stream()
                        .filter(timeSlot -> !existingTimes.contains(timeSlot))
                        .toList();
                if (!missingTimes.isEmpty()) {
                    creationService.createMissingSlots(doctorId, workDate, missingTimes);
                }
            }
        }
    }
}
