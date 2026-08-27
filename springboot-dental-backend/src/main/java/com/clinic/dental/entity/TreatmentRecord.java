package com.clinic.dental.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 诊疗记录
 */
@Data
@Entity
@Table(name = "treatment_record")
public class TreatmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 记录编号，如 RC20261001 */
    private String code;

    /** 关联患者 */
    private Long patientId;

    /** 主治医生 */
    private Long doctorId;

    /** 主诉 */
    private String complaint;

    /** 诊断结果 */
    private String diagnosis;

    /** 治疗项目，逗号分隔，如：补牙（树脂）,根管治疗 */
    private String services;

    /** 涉及牙齿，FDI 编号逗号分隔，如：36,37 */
    private String teeth;

    /** 费用合计 */
    private BigDecimal fee;

    /** 就诊日期 */
    private LocalDate date;

    /** 是否已支付 */
    private Boolean paid;

    /** 医嘱 / 备注 */
    private String note;
}
