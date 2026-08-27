package com.clinic.dental.security;

import com.clinic.dental.entity.SysUser;
import com.clinic.dental.repository.SysUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 将有效 Bearer Token 解析为请求级 UserContext。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final SysUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(TokenService tokenService,
                           SysUserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "请先登录后再访问系统数据");
            return false;
        }

        Optional<CurrentUser> currentUser = tokenService.parse(header.substring(7).trim());
        if (currentUser.isEmpty()) {
            writeUnauthorized(response, "登录状态无效或已过期，请重新登录");
            return false;
        }

        CurrentUser tokenUser = currentUser.get();
        Optional<SysUser> databaseUser = userRepository.findById(tokenUser.id());
        if (databaseUser.isEmpty() || !Integer.valueOf(SysUser.STATUS_ENABLED).equals(databaseUser.get().getStatus())) {
            writeUnauthorized(response, "账号已被停用或不存在");
            return false;
        }
        SysUser user = databaseUser.get();
        if (!user.getUsername().equals(tokenUser.username())
                || !user.getRole().equals(tokenUser.role())
                || !java.util.Objects.equals(user.getDoctorId(), tokenUser.doctorId())) {
            writeUnauthorized(response, "登录状态已变更，请重新登录");
            return false;
        }

        UserContext.set(tokenUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        UserContext.remove();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        UserContext.remove();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", HttpStatus.UNAUTHORIZED.value());
        body.put("message", message);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
