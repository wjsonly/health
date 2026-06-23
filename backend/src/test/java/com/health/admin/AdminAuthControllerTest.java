package com.health.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "app.admin.auth.token-secret=test-secret-for-admin-auth"
})
@AutoConfigureMockMvc
class AdminAuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void loginReturnsBearerTokenForSeededAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.data.admin.username").value("admin"))
                .andExpect(jsonPath("$.data.admin.displayName").value("系统管理员"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void adminEndpointsRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/admin/therapists"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请先登录后台"));
    }

    @Test
    void adminEndpointsAcceptBearerToken() throws Exception {
        String token = loginToken();

        mockMvc.perform(get("/api/admin/therapists")
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void publicEndpointsDoNotRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/service-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String loginToken() throws Exception {
        return mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
