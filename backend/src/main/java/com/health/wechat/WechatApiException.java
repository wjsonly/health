package com.health.wechat;

import com.health.common.BadRequestException;

public class WechatApiException extends BadRequestException {
    public WechatApiException(String message) {
        super(message);
    }
}
