package com.health.wechat;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class WechatApiHttpClient implements WechatApiClient {
    private static final String WECHAT_HOST = "api.weixin.qq.com";

    private final RestClient restClient = RestClient.create();
    private final String appId;
    private final String appSecret;
    private String cachedAccessToken;
    private Instant cachedAccessTokenExpiresAt = Instant.EPOCH;

    public WechatApiHttpClient(
            @Value("${health.wechat.app-id:}") String appId,
            @Value("${health.wechat.app-secret:}") String appSecret
    ) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    public WechatSession fetchSession(String loginCode) {
        assertConfigured();
        Code2SessionResponse response = restClient.get()
                .uri(builder -> builder
                        .scheme("https")
                        .host(WECHAT_HOST)
                        .path("/sns/jscode2session")
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .queryParam("js_code", loginCode)
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve()
                .body(Code2SessionResponse.class);
        if (response == null || !StringUtils.hasText(response.openId())) {
            throw new WechatApiException(wechatError(response == null ? null : response.errmsg()));
        }
        return new WechatSession(response.openId(), response.sessionKey());
    }

    @Override
    public WechatPhoneNumber fetchPhoneNumber(String phoneCode) {
        assertConfigured();
        PhoneNumberResponse response = restClient.post()
                .uri(builder -> builder
                        .scheme("https")
                        .host(WECHAT_HOST)
                        .path("/wxa/business/getuserphonenumber")
                        .queryParam("access_token", accessToken())
                        .build())
                .body(Map.of("code", phoneCode))
                .retrieve()
                .body(PhoneNumberResponse.class);
        if (response == null || response.phoneInfo() == null
                || !StringUtils.hasText(response.phoneInfo().phoneNumber())) {
            throw new WechatApiException(wechatError(response == null ? null : response.errmsg()));
        }
        return new WechatPhoneNumber(response.phoneInfo().phoneNumber());
    }

    private synchronized String accessToken() {
        if (StringUtils.hasText(cachedAccessToken) && Instant.now().isBefore(cachedAccessTokenExpiresAt)) {
            return cachedAccessToken;
        }
        AccessTokenResponse response = restClient.get()
                .uri(builder -> builder
                        .scheme("https")
                        .host(WECHAT_HOST)
                        .path("/cgi-bin/token")
                        .queryParam("grant_type", "client_credential")
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .build())
                .retrieve()
                .body(AccessTokenResponse.class);
        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new WechatApiException(wechatError(response == null ? null : response.errmsg()));
        }
        cachedAccessToken = response.accessToken();
        cachedAccessTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, response.expiresIn() - 60));
        return cachedAccessToken;
    }

    private void assertConfigured() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new WechatApiException("微信小程序配置未完成");
        }
    }

    private String wechatError(String message) {
        return StringUtils.hasText(message) ? "微信授权失败：" + message : "微信授权失败";
    }

    record Code2SessionResponse(
            @JsonProperty("openid") String openId,
            @JsonProperty("session_key") String sessionKey,
            @JsonProperty("errcode") Integer errcode,
            @JsonProperty("errmsg") String errmsg
    ) {
    }

    record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn,
            @JsonProperty("errcode") Integer errcode,
            @JsonProperty("errmsg") String errmsg
    ) {
    }

    record PhoneNumberResponse(
            @JsonProperty("phone_info") PhoneInfo phoneInfo,
            @JsonProperty("errcode") Integer errcode,
            @JsonProperty("errmsg") String errmsg
    ) {
    }

    record PhoneInfo(@JsonProperty("phoneNumber") String phoneNumber) {
    }
}
