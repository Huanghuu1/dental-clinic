package com.clinic.dental.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收费账单
 */
@Data
@Entity
@Table(name = "bill")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 账单编号，如 BL20260002 */
    private String code;

    /** 关联患者 */
    private Long patientId;

    /** 收费项目 */
    private String service;

    /** 金额 */
    private BigDecimal amount;

    /** 支付方式：微信支付 / 支付宝 / 现金 / 银行卡 / 原路退回 */
    private String method;

    /** 开单日期 */
    private LocalDate date;

    /** 状态：已支付 / 待支付 / 已退款 */
    private String status;
}
