package com.health.admin;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = "|";

    private final String tokenSecret;
    private final long ttlSeconds;

    public AdminTokenService(
            @Value("${app.admin.auth.token-secret:dev-health-admin-token-secret-change-me}") String tokenSecret,
            @Value("${app.admin.auth.token-ttl-hours:8}") long ttlHours
    ) {
        this.tokenSecret = tokenSecret;
        this.ttlSeconds = ttlHours * 3600;
    }

    public IssuedAdminToken issue(AdminUser adminUser) {
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String payload = adminUser.getId() + SEPARATOR + adminUser.getUsername() + SEPARATOR + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return new IssuedAdminToken(encodedPayload + "." + sign(encodedPayload), Instant.ofEpochSecond(expiresAt));
    }

    public Optional<AdminPrincipal> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            return Optional.empty();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split("\\|", 3);
            if (values.length != 3) {
                return Optional.empty();
            }
            long expiresAt = Long.parseLong(values[2]);
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }
            return Optional.of(new AdminPrincipal(Long.parseLong(values[0]), values[1], expiresAt));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("后台 token 签名失败", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < left.length(); index++) {
            result |= left.charAt(index) ^ right.charAt(index);
        }
        return result == 0;
    }

    public record IssuedAdminToken(String token, Instant expiresAt) {
    }
}
