package com.clinic.dental.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 就诊预约
 */
@Data
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 预约编号，如 AP20260101 */
    private String code;

    /** 关联患者 */
    private Long patientId;

    /** 关联医生 */
    private Long doctorId;

    /** 预约项目，如：根管治疗 */
    private String type;

    /** 预约日期 */
    private LocalDate date;

    /** 就诊时段，如 09:30 */
    private String time;

    /** 状态：待就诊 / 已完成 / 已取消 */
    private String status;

    /** 初诊 / 复诊 */
    private String visitType;

    /** 备注 */
    private String note;

    /** 预约状态变更乐观锁，防止取消、完成与改期互相覆盖 */
    @Version
    private Integer version;
}
