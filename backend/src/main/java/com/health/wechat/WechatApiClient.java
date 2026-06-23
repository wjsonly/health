package com.health.wechat;

public interface WechatApiClient {
    WechatSession fetchSession(String loginCode);

    WechatPhoneNumber fetchPhoneNumber(String phoneCode);
}
