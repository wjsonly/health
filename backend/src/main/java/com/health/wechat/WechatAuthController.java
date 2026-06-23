package com.health.wechat;

import com.health.common.ApiResponse;
import com.health.user.User;
import com.health.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class WechatAuthController {
    private static final String ACTIVE = "ACTIVE";

    private final WechatApiClient wechatApiClient;
    private final UserRepository userRepository;
    private final MiniProgramTokenService tokenService;

    public WechatAuthController(
            WechatApiClient wechatApiClient,
            UserRepository userRepository,
            MiniProgramTokenService tokenService
    ) {
        this.wechatApiClient = wechatApiClient;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    @PostMapping("/api/mp/auth/login")
    public ApiResponse<WechatLoginResponse> login(@Valid @RequestBody WechatLoginRequest request) {
        WechatSession session = wechatApiClient.fetchSession(request.loginCode());
        WechatPhoneNumber phoneNumber = wechatApiClient.fetchPhoneNumber(request.phoneCode());
        User user = userRepository.findByOpenId(session.openId())
                .orElseGet(() -> new User(null, session.openId(), request.nickname(), request.avatarUrl(),
                        phoneNumber.phoneNumber(), ACTIVE));
        user.updateWechatProfile(request.nickname(), request.avatarUrl(), phoneNumber.phoneNumber());
        User savedUser = userRepository.save(user);
        MiniProgramTokenService.IssuedMiniProgramToken token = tokenService.issue(savedUser);
        return ApiResponse.ok(new WechatLoginResponse(
                token.token(),
                token.expiresAt(),
                WechatUserResponse.from(savedUser)
        ));
    }

    record WechatLoginRequest(
            @NotBlank String loginCode,
            @NotBlank String phoneCode,
            String nickname,
            String avatarUrl
    ) {
    }
}
