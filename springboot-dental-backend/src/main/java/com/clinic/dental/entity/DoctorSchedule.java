package com.clinic.dental.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 医生半小时时间片排班
 */
@Data
@Entity
@Table(
        name = "doctor_schedule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_doctor_schedule_slot",
                columnNames = {"doctor_id", "work_date", "time_slot"}
        )
)
public class DoctorSchedule {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_BOOKED = "BOOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联医生 */
    @Column(nullable = false)
    private Long doctorId;

    /** 出诊日期 */
    @Column(nullable = false)
    private LocalDate workDate;

    /** 半小时时间片，如 09:00 */
    @Column(nullable = false, length = 5)
    private String timeSlot;

    /** 该时间片最大号源数 */
    @Column(nullable = false)
    private Integer maxQuota = 1;

    /** 该时间片剩余号源数 */
    @Column(nullable = false)
    private Integer remainingQuota = 1;

    /** AVAILABLE / BOOKED */
    @Column(nullable = false, length = 20)
    private String status = STATUS_AVAILABLE;

    /** 乐观锁版本号 */
    @Version
    @Column(nullable = false)
    private Integer version;
}
