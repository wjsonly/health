package com.health.wechat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.common.ApiResponse;
import com.health.user.User;
import com.health.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MiniProgramAuthInterceptor implements HandlerInterceptor {
    public static final String PRINCIPAL_ATTRIBUTE = "miniProgramPrincipal";
    private static final String ACTIVE = "ACTIVE";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AVAILABLE_SLOTS_PATH = "/api/appointments/available-slots";
    private static final String APPOINTMENTS_PATH = "/api/appointments";

    private final MiniProgramTokenService tokenService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public MiniProgramAuthInterceptor(
            MiniProgramTokenService tokenService,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())
                || AVAILABLE_SLOTS_PATH.equals(request.getRequestURI())) {
            return true;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isGuestBookingRequest(request) && (authorization == null || !authorization.startsWith(BEARER_PREFIX))) {
            return true;
        }
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "请先完成微信授权登录");
            return false;
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        MiniProgramPrincipal principal = tokenService.parse(token)
                .flatMap(parsed -> userRepository.findByIdAndStatus(parsed.id(), ACTIVE)
                        .filter(user -> parsed.openId().equals(user.getOpenId()))
                        .map(User::getId)
                        .map(id -> parsed))
                .orElse(null);
        if (principal == null) {
            writeUnauthorized(response, "登录已失效，请重新授权");
            return false;
        }
        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        return true;
    }

    private boolean isGuestBookingRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && APPOINTMENTS_PATH.equals(request.getRequestURI());
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
