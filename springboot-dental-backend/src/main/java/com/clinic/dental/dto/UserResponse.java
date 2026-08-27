package com.clinic.dental.dto;

import com.clinic.dental.entity.SysUser;

/** 安全的用户公开信息，不包含密码或账号启用状态。 */
public record UserResponse(Long id,
                           String username,
                           String realName,
                           String role,
                           Long doctorId) {

    public static UserResponse from(SysUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRealName(),
                user.getRole(), user.getDoctorId());
    }
}
