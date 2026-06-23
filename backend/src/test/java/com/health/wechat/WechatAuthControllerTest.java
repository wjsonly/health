package com.health.wechat;

import com.health.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wechat-auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class WechatAuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @MockBean
    WechatApiClient wechatApiClient;

    @Test
    void loginCreatesUserFromWechatOpenIdAndPhone() throws Exception {
        when(wechatApiClient.fetchSession("login-code"))
                .thenReturn(new WechatSession("openid-login", "session-key"));
        when(wechatApiClient.fetchPhoneNumber("phone-code"))
                .thenReturn(new WechatPhoneNumber("13900001234"));

        mockMvc.perform(post("/api/mp/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "login-code",
                                  "phoneCode": "phone-code",
                                  "nickname": "微信用户",
                                  "avatarUrl": "https://example.test/avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.user.phone").value("13900001234"));

        assertThat(userRepository.findByOpenId("openid-login")).isPresent();
    }

    @Test
    void loginUpdatesPhoneForExistingOpenId() throws Exception {
        userRepository.save(new com.health.user.User(null, "openid-repeat", "旧昵称", "", "13800000000", "ACTIVE"));
        when(wechatApiClient.fetchSession("repeat-login-code"))
                .thenReturn(new WechatSession("openid-repeat", "session-key"));
        when(wechatApiClient.fetchPhoneNumber("repeat-phone-code"))
                .thenReturn(new WechatPhoneNumber("13900005678"));

        mockMvc.perform(post("/api/mp/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "repeat-login-code",
                                  "phoneCode": "repeat-phone-code",
                                  "nickname": "新昵称",
                                  "avatarUrl": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.phone").value("13900005678"));

        assertThat(userRepository.findByOpenId("openid-repeat"))
                .get()
                .extracting(com.health.user.User::getPhone)
                .isEqualTo("13900005678");
    }

    @Test
    void loginRejectsWechatClientFailureWithoutLeakingConfiguration() throws Exception {
        when(wechatApiClient.fetchSession("bad-code"))
                .thenThrow(new WechatApiException("微信登录失败"));

        mockMvc.perform(post("/api/mp/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "bad-code",
                                  "phoneCode": "phone-code"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("微信登录失败"));
    }
}
