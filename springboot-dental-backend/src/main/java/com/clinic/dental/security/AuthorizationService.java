package com.clinic.dental.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 集中提供当前账号与角色边界判断。 */
@Component
public class AuthorizationService {

    public CurrentUser currentUser() {
        CurrentUser user = UserContext.get();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态无效或已过期");
        }
        return user;
    }

    public void requireAdmin() {
        if (!currentUser().isAdmin()) {
            throw forbidden();
        }
    }

    public void requireAdminOrStaff() {
        CurrentUser user = currentUser();
        if (!user.isAdmin() && !user.isStaff()) {
            throw forbidden();
        }
    }

    public Long requireDoctorId() {
        CurrentUser user = currentUser();
        if (!user.isDoctor() || user.doctorId() == null) {
            throw forbidden();
        }
        return user.doctorId();
    }

    public void requireDoctorOwns(Long doctorId) {
        CurrentUser user = currentUser();
        if (user.isDoctor() && (doctorId == null || !requireDoctorId().equals(doctorId))) {
            throw forbidden();
        }
    }

    public void requireAppointmentOperations() {
        CurrentUser user = currentUser();
        if (!user.isAdmin() && !user.isStaff() && !user.isDoctor()) {
            throw forbidden();
        }
    }

    public ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权执行此操作");
    }
}
