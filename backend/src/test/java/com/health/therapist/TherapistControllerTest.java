package com.health.therapist;

import java.sql.PreparedStatement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static com.health.admin.AdminAuthTestSupport.adminBearerToken;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:therapist-controllers;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class TherapistControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long inactiveTherapistId;
    Long invisibleTherapistId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from therapists where employee_no like 'TEST-%'");
        inactiveTherapistId = insertTherapist("测试下架技师", "TEST-INACTIVE", "INACTIVE", true);
        invisibleTherapistId = insertTherapist("测试隐藏技师", "TEST-INVISIBLE", "ACTIVE", false);
    }

    @Test
    void publicListDefaultsStoreAndDoesNotExposeMaintenanceFields() throws Exception {
        mockMvc.perform(get("/api/therapists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data[*].name", hasItem("李静")))
                .andExpect(jsonPath("$.data[*].name", hasItem("王明")))
                .andExpect(jsonPath("$.data[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data[0].employeeNo").doesNotExist())
                .andExpect(jsonPath("$.data[0].visible").doesNotExist())
                .andExpect(jsonPath("$.data[0].sortOrder").doesNotExist())
                .andExpect(jsonPath("$.data[0].certificateUrls").doesNotExist());
    }

    @Test
    void publicListExcludesInactiveAndInvisibleTherapists() throws Exception {
        mockMvc.perform(get("/api/therapists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", not(hasItem("测试下架技师"))))
                .andExpect(jsonPath("$.data[*].name", not(hasItem("测试隐藏技师"))));
    }

    @Test
    void publicDetailDoesNotExposeMaintenanceFields() throws Exception {
        mockMvc.perform(get("/api/therapists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("李静"))
                .andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.employeeNo").doesNotExist())
                .andExpect(jsonPath("$.data.visible").doesNotExist())
                .andExpect(jsonPath("$.data.sortOrder").doesNotExist())
                .andExpect(jsonPath("$.data.certificateUrls").doesNotExist());
    }

    @Test
    void publicDetailRejectsInactiveOrInvisibleTherapist() throws Exception {
        mockMvc.perform(get("/api/therapists/{id}", inactiveTherapistId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("技师不存在或已下架"));

        mockMvc.perform(get("/api/therapists/{id}", invisibleTherapistId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("技师不存在或已下架"));
    }

    @Test
    void adminListDefaultsStoreAndExposesMaintenanceFields() throws Exception {
        mockMvc.perform(get("/api/admin/therapists")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].name", hasItem("李静")))
                .andExpect(jsonPath("$.data[0].employeeNo").value(notNullValue()))
                .andExpect(jsonPath("$.data[0].phone").value(notNullValue()));
    }

    @Test
    void adminCreateDuplicateEmployeeNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/therapists")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": 1,
                                  "name": "重复工号",
                                  "avatarUrl": "",
                                  "gender": "FEMALE",
                                  "phone": "13800000003",
                                  "employeeNo": "T001",
                                  "yearsOfExperience": 3,
                                  "level": "NORMAL",
                                  "status": "ACTIVE",
                                  "introduction": "测试",
                                  "specialties": "肩颈",
                                  "serviceTags": "手法稳",
                                  "certificateUrls": "",
                                  "bookable": true,
                                  "visible": true,
                                  "sortOrder": 30
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("同一门店下技师工号不能重复"));
    }

    @Test
    void adminUpdatePreservesEmployeeNumber() throws Exception {
        Long therapistId = insertTherapist("测试更新技师", "TEST-UPDATE", "ACTIVE", true);

        mockMvc.perform(put("/api/admin/therapists/{id}", therapistId)
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试更新后技师",
                                  "avatarUrl": "",
                                  "gender": "FEMALE",
                                  "phone": "13800009999",
                                  "yearsOfExperience": 5,
                                  "level": "SENIOR",
                                  "status": "ACTIVE",
                                  "introduction": "更新介绍",
                                  "specialties": "肩颈,足疗",
                                  "serviceTags": "手法稳",
                                  "certificateUrls": "cert-a",
                                  "bookable": true,
                                  "visible": true,
                                  "sortOrder": 35
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("测试更新后技师"))
                .andExpect(jsonPath("$.data.employeeNo").value("TEST-UPDATE"))
                .andExpect(jsonPath("$.data.phone").value("13800009999"));
    }

    @Test
    void statusPatchRejectsBlankStatusWithValidationEnvelope() throws Exception {
        mockMvc.perform(patch("/api/admin/therapists/1/status")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": ""
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("状态不能为空"));
    }

    private Long insertTherapist(String name, String employeeNo, String status, boolean visible) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into therapists (
                        store_id, name, avatar_url, gender, phone, employee_no, years_of_experience,
                        level, status, introduction, specialties, service_tags, certificate_urls,
                        bookable, visible, sort_order
                    )
                    values (1, ?, '', 'FEMALE', '13800008888', ?, 4, 'NORMAL', ?, '测试介绍',
                            '肩颈', '手法稳', '', true, ?, 5)
                    """, new String[] {"id"});
            statement.setString(1, name);
            statement.setString(2, employeeNo);
            statement.setString(3, status);
            statement.setBoolean(4, visible);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
