package com.clinic.dental.security;

/** Token 中承载、经服务端校验后的当前登录用户。 */
public record CurrentUser(Long id,
                          String username,
                          String role,
                          Long doctorId) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isDoctor() {
        return "DOCTOR".equals(role);
    }

    public boolean isStaff() {
        return "STAFF".equals(role);
    }
}
