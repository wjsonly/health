package com.health.wechat;

import java.time.Instant;

public record WechatLoginResponse(String token, Instant expiresAt, WechatUserResponse user) {
}
