package com.health.admin;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AdminAuthTestSupport {
    private AdminAuthTestSupport() {
    }

    public static String adminBearerToken(MockMvc mockMvc) throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = response.replaceFirst(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        return "Bearer " + token;
    }
}
