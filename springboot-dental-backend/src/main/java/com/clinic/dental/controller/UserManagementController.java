package com.clinic.dental.controller;

import com.clinic.dental.dto.UserManagementResponse;
import com.clinic.dental.dto.UserStatusRequest;
import com.clinic.dental.entity.SysUser;
import com.clinic.dental.repository.SysUserRepository;
import com.clinic.dental.security.AuthorizationService;
import com.clinic.dental.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final SysUserRepository userRepository;
    private final AuthorizationService authorizationService;

    public UserManagementController(SysUserRepository userRepository,
                                    AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<UserManagementResponse> list() {
        authorizationService.requireAdmin();
        return userRepository.findAll().stream().map(UserManagementResponse::from).toList();
    }

    @PutMapping("/{id}/status")
    public UserManagementResponse updateStatus(@PathVariable Long id, @RequestBody UserStatusRequest request) {
        authorizationService.requireAdmin();
        if (request == null || (request.status() != SysUser.STATUS_ENABLED && request.status() != SysUser.STATUS_DISABLED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号状态只能为 1（启用）或 0（停用）");
        }
        CurrentUser currentUser = authorizationService.currentUser();
        if (currentUser.id().equals(id) && request.status() == SysUser.STATUS_DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能停用当前登录账号");
        }
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在"));
        user.setStatus(request.status());
        return UserManagementResponse.from(userRepository.save(user));
    }
}
