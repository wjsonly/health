package com.health.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private static final String ACTIVE = "ACTIVE";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminTokenService tokenService;
    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(
            AdminTokenService tokenService,
            AdminUserRepository adminUserRepository,
            ObjectMapper objectMapper
    ) {
        this.tokenService = tokenService;
        this.adminUserRepository = adminUserRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "请先登录后台");
            return false;
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        AdminUser adminUser = tokenService.parse(token)
                .flatMap(principal -> adminUserRepository.findByIdAndStatus(principal.id(), ACTIVE))
                .orElse(null);
        if (adminUser == null) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }
        request.setAttribute("adminUser", adminUser);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
