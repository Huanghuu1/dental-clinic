package com.clinic.dental.controller;

import com.clinic.dental.dto.LoginRequest;
import com.clinic.dental.dto.LoginResponse;
import com.clinic.dental.dto.UserResponse;
import com.clinic.dental.entity.SysUser;
import com.clinic.dental.repository.SysUserRepository;
import com.clinic.dental.security.AuthorizationService;
import com.clinic.dental.security.CurrentUser;
import com.clinic.dental.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthorizationService authorizationService;

    public AuthController(SysUserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw unauthorized();
        }
        SysUser user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(this::unauthorized);
        if (!Integer.valueOf(SysUser.STATUS_ENABLED).equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw unauthorized();
        }
        return new LoginResponse(tokenService.generate(user), UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me() {
        CurrentUser currentUser = authorizationService.currentUser();
        SysUser user = userRepository.findById(currentUser.id())
                .orElseThrow(this::unauthorized);
        return UserResponse.from(user);
    }

    /** 登录页展示的演示账号，密码不通过接口传输。 */
    @GetMapping("/init")
    public Map<String, Object> init() {
        List<Map<String, String>> accounts = List.of(
                account("admin", "系统管理员", "ADMIN"),
                account("doctor1", "张医生", "DOCTOR"),
                account("doctor2", "李医生", "DOCTOR"),
                account("staff1", "前台接待", "STAFF")
        );
        return Map.of("accounts", accounts);
    }

    private Map<String, String> account(String username, String realName, String role) {
        return Map.of("username", username, "realName", realName, "role", role);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，或账号已被停用");
    }
}
