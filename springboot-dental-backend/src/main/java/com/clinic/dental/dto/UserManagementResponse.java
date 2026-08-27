package com.clinic.dental.dto;

import com.clinic.dental.entity.SysUser;

/** 管理员账户管理页面使用的脱敏账号信息。 */
public record UserManagementResponse(Long id,
                                     String username,
                                     String realName,
                                     String role,
                                     Long doctorId,
                                     Integer status) {

    public static UserManagementResponse from(SysUser user) {
        return new UserManagementResponse(user.getId(), user.getUsername(), user.getRealName(),
                user.getRole(), user.getDoctorId(), user.getStatus());
    }
}
