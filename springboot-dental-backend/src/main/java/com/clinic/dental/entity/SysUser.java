package com.clinic.dental.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 系统登录账号；角色由 RBAC 模型限定为 ADMIN、DOCTOR、STAFF。
 */
@Data
@Entity
@Table(name = "sys_user", uniqueConstraints = @UniqueConstraint(name = "uk_sys_user_username", columnNames = "username"))
public class SysUser {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_STAFF = "STAFF";

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    /** BCrypt 密文，绝不直接返回给前端。 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 64)
    private String realName;

    @Column(nullable = false, length = 16)
    private String role;

    /** 医生账号绑定的医生资料 ID；其他角色为空。 */
    private Long doctorId;

    @Column(nullable = false)
    private Integer status = STATUS_ENABLED;
}
