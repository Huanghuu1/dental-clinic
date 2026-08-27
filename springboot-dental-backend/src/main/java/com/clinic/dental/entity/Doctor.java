package com.clinic.dental.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 医生信息
 */
@Data
@Entity
@Table(name = "doctor")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 姓名 */
    private String name;

    /** 职称 */
    private String title;

    /** 从业年限 */
    private Integer years;

    /** 专长，逗号分隔，如：种植牙,口腔外科 */
    private String spec;

    /** 本月接诊量（统计冗余） */
    private Integer work;
}
