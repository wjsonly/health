package com.health.wechat;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class WechatApiHttpClient implements WechatApiClient {
    private static final String WECHAT_HOST = "api.weixin.qq.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String appSecret;
    private String cachedAccessToken;
    private Instant cachedAccessTokenExpiresAt = Instant.EPOCH;

    @Autowired
    public WechatApiHttpClient(
            @Value("${health.wechat.app-id:}") String appId,
            @Value("${health.wechat.app-secret:}") String appSecret
    ) {
        this(appId, appSecret, RestClient.create());
    }

    WechatApiHttpClient(String appId, String appSecret, RestClient restClient) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public WechatSession fetchSession(String loginCode) {
        assertConfigured();
        Code2SessionResponse response = readWechatResponse(restClient.get()
                .uri(builder -> builder
                        .scheme("https")
                        .host(WECHAT_HOST)
                        .path("/sns/jscode2session")
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .queryParam("js_code", loginCode)
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve(), Code2SessionResponse.class);
        if (response == null || !StringUtils.hasText(response.openId())) {
            throw new WechatApiException(wechatError(response == null ? null : response.errmsg()));
        }
        return new WechatSession(response.openId(), response.sessionKey());
    }

    @Override
    public WechatPhoneNumber fetchPhoneNumber(String phoneCode) {
        assertConfigured();
        PhoneNumberResponse response = readWechatResponse(restClient.post()
                .uri(builder -> builder
                        .scheme("https")
                        .host(WECHAT_HOST)
                        .path("/wxa/business/getuserphonenumber")
                        .queryParam("access_token", accessToken())
                        .build())
                .body(Map.of("code", phoneCode))
                .retrieve(), PhoneNumberResponse.class);
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
        AccessTokenResponse response = readWechatResponse(restClient.get()
                .uri(builder -> builder
                        .scheme("https")
                        .host(WECHAT_HOST)
                        .path("/cgi-bin/token")
                        .queryParam("grant_type", "client_credential")
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .build())
                .retrieve(), AccessTokenResponse.class);
        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new WechatApiException(wechatError(response == null ? null : response.errmsg()));
        }
        cachedAccessToken = response.accessToken();
        cachedAccessTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, response.expiresIn() - 60));
        return cachedAccessToken;
    }

    private <T> T readWechatResponse(RestClient.ResponseSpec responseSpec, Class<T> responseType) {
        String body = responseSpec.body(String.class);
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            return objectMapper.readValue(body, responseType);
        } catch (JsonProcessingException exception) {
            throw new WechatApiException("微信授权失败");
        }
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
