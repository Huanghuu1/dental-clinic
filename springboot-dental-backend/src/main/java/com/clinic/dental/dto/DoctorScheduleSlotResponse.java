package com.clinic.dental.dto;

import com.clinic.dental.entity.DoctorSchedule;

import java.time.LocalDate;

public record DoctorScheduleSlotResponse(
        Long id,
        Long doctorId,
        LocalDate workDate,
        String timeSlot,
        Integer maxQuota,
        Integer remainingQuota,
        String status,
        boolean available
) {
    public static DoctorScheduleSlotResponse from(DoctorSchedule schedule) {
        boolean available = DoctorSchedule.STATUS_AVAILABLE.equals(schedule.getStatus())
                && schedule.getRemainingQuota() != null
                && schedule.getRemainingQuota() > 0;
        return new DoctorScheduleSlotResponse(
                schedule.getId(),
                schedule.getDoctorId(),
                schedule.getWorkDate(),
                schedule.getTimeSlot(),
                schedule.getMaxQuota(),
                schedule.getRemainingQuota(),
                schedule.getStatus(),
                available
        );
    }
}
