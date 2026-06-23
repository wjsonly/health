package com.health.wechat;

import com.health.user.User;

public record WechatUserResponse(Long id, String phone) {
    static WechatUserResponse from(User user) {
        return new WechatUserResponse(user.getId(), user.getPhone());
    }
}
