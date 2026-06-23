package com.health.admin;

import java.time.Instant;

import com.health.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthController {
    private static final String ACTIVE = "ACTIVE";

    private final AdminUserRepository adminUserRepository;
    private final AdminTokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthController(AdminUserRepository adminUserRepository, AdminTokenService tokenService) {
        this.adminUserRepository = adminUserRepository;
        this.tokenService = tokenService;
    }

    @PostMapping("/api/admin/auth/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminUser adminUser = adminUserRepository.findByUsername(request.username())
                .filter(user -> ACTIVE.equals(user.getStatus()))
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElse(null);
        if (adminUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("用户名或密码错误"));
        }
        AdminTokenService.IssuedAdminToken token = tokenService.issue(adminUser);
        return ResponseEntity.ok(ApiResponse.ok(new AdminLoginResponse(
                token.token(),
                token.expiresAt(),
                AdminInfo.from(adminUser)
        )));
    }

    @GetMapping("/api/admin/auth/me")
    public ApiResponse<AdminInfo> me(@RequestAttribute("adminUser") AdminUser adminUser) {
        return ApiResponse.ok(AdminInfo.from(adminUser));
    }

    record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record AdminLoginResponse(String token, Instant expiresAt, AdminInfo admin) {
    }

    record AdminInfo(Long id, String username, String displayName) {
        static AdminInfo from(AdminUser adminUser) {
            return new AdminInfo(adminUser.getId(), adminUser.getUsername(), adminUser.getDisplayName());
        }
    }
}
