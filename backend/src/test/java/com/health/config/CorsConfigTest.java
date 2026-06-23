package com.health.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cors;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class CorsConfigTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void allowsAdminDevOriginPreflightRequests() throws Exception {
        mockMvc.perform(options("/api/store")
                        .header(ORIGIN, "http://localhost:5174")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5174"))
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }
}
