package com.clinic.dental.service;

import com.clinic.dental.entity.DoctorSchedule;
import com.clinic.dental.repository.AppointmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 独立事务补全排班，便于并发查询发生唯一键竞争后安全重读。
 */
@Service
public class DoctorScheduleCreationService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleInsertService insertService;

    public DoctorScheduleCreationService(AppointmentRepository appointmentRepository,
                                         DoctorScheduleInsertService insertService) {
        this.appointmentRepository = appointmentRepository;
        this.insertService = insertService;
    }

    public void createMissingSlots(Long doctorId, LocalDate workDate, List<String> timeSlots) {
        for (String timeSlot : timeSlots) {
            try {
                insertService.insert(List.of(newReconciledSchedule(doctorId, workDate, timeSlot)));
            } catch (DataIntegrityViolationException exception) {
                if (!isDuplicateKey(exception)) {
                    throw exception;
                }
                // 另一请求已先行补全该时间片，唯一键确保数据库中不会重复。
            }
        }
    }

    private boolean isDuplicateKey(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uk_doctor_schedule_slot")
                    || message.contains("Duplicate entry"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private DoctorSchedule newReconciledSchedule(Long doctorId, LocalDate workDate, String timeSlot) {
        long activeAppointments = appointmentRepository
                .countByDoctorIdAndDateAndTimeAndStatusNot(
                        doctorId, workDate, timeSlot, AppointmentService.STATUS_CANCELLED);
        if (activeAppointments > 1) {
            throw new IllegalStateException("医生 " + doctorId + " 在 " + workDate + " "
                    + timeSlot + " 存在重复有效预约，请先清理冲突数据");
        }
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorId(doctorId);
        schedule.setWorkDate(workDate);
        schedule.setTimeSlot(timeSlot);
        schedule.setMaxQuota(1);
        schedule.setRemainingQuota(activeAppointments == 0 ? 1 : 0);
        schedule.setStatus(activeAppointments == 0
                ? DoctorSchedule.STATUS_AVAILABLE
                : DoctorSchedule.STATUS_BOOKED);
        return schedule;
    }
}
