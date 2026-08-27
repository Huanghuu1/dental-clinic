package com.clinic.dental.config;

import com.clinic.dental.entity.SysUser;
import com.clinic.dental.repository.SysUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** 首次启动时初始化用于演示的三类 RBAC 账号。 */
@Component
@Order(2)
public class SysUserInitializer implements CommandLineRunner {

    private final SysUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public SysUserInitializer(SysUserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.saveAll(List.of(
                user("admin", "系统管理员", SysUser.ROLE_ADMIN, null),
                user("doctor1", "张医生", SysUser.ROLE_DOCTOR, 1L),
                user("doctor2", "李医生", SysUser.ROLE_DOCTOR, 2L),
                user("staff1", "前台接待", SysUser.ROLE_STAFF, null)
        ));
    }

    private SysUser user(String username, String realName, String role, Long doctorId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRealName(realName);
        user.setRole(role);
        user.setDoctorId(doctorId);
        user.setStatus(SysUser.STATUS_ENABLED);
        return user;
    }
}
