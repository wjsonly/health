package com.health.wechat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WechatApiHttpClientTest {
    @Test
    void rejectsMissingWechatConfigurationBeforeCallingRemoteApi() {
        WechatApiHttpClient client = new WechatApiHttpClient("", "");

        assertThatThrownBy(() -> client.fetchSession("login-code"))
                .isInstanceOf(WechatApiException.class)
                .hasMessage("微信小程序配置未完成");
    }
}
