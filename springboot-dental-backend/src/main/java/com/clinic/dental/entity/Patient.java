package com.clinic.dental.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 患者档案
 */
@Data
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 姓名 */
    private String name;

    /** 性别：男 / 女 */
    private String gender;

    /** 年龄 */
    private Integer age;

    /** 联系电话 */
    private String phone;

    /** 血型 */
    private String bloodType;

    /** 过敏史 */
    private String allergy;

    /** 居住地址 */
    private String address;

    /** 备注 */
    private String note;

    /** 建档日期 */
    private LocalDate created;
}
