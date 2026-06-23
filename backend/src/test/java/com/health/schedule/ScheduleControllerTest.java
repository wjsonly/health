package com.health.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.health.admin.AdminAuthTestSupport.adminBearerToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schedule-controllers;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class ScheduleControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    TherapistScheduleRepository scheduleRepository;

    @Test
    void createRejectsUnsupportedScheduleType() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistId": 1,
                                  "storeId": 1,
                                  "scheduleDate": "2026-06-13",
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "TRAINING",
                                  "note": "培训"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("排班类型不支持"));
    }

    @Test
    void createRejectsMissingTherapist() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistId": 999999,
                                  "storeId": 1,
                                  "scheduleDate": "2026-06-13",
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK",
                                  "note": "白班"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("技师不存在或已下架"));
    }

    @Test
    void createRejectsMissingStore() throws Exception {
        mockMvc.perform(post("/api/admin/schedules")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistId": 1,
                                  "storeId": 999999,
                                  "scheduleDate": "2026-06-13",
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK",
                                  "note": "白班"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("门店信息未配置"));
    }

    @Test
    void batchCreatesSchedulesForSelectedWeekdaysAndTherapists() throws Exception {
        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [1, 2],
                                  "startDate": "2026-07-06",
                                  "endDate": "2026-07-10",
                                  "weekdays": [1, 3, 5],
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK",
                                  "note": "白班"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.createdCount").value(6))
                .andExpect(jsonPath("$.data.schedules.length()").value(6));

        assertThat(scheduleRepository.findByScheduleDateOrderByStartTimeAsc(LocalDate.parse("2026-07-06")))
                .hasSize(2);
    }

    @Test
    void batchRejectsEmptyWeekdays() throws Exception {
        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [1],
                                  "startDate": "2026-07-13",
                                  "endDate": "2026-07-17",
                                  "weekdays": [],
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择排班星期"));
    }

    @Test
    void batchRejectsUnsupportedScheduleType() throws Exception {
        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [1],
                                  "startDate": "2026-07-13",
                                  "endDate": "2026-07-17",
                                  "weekdays": [1, 2, 3],
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "TRAINING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("排班类型不支持"));
    }

    @Test
    void batchRejectsEndDateBeforeStartDate() throws Exception {
        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [1],
                                  "startDate": "2026-07-17",
                                  "endDate": "2026-07-13",
                                  "weekdays": [1, 2, 3],
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("排班结束日期必须不早于开始日期"));
    }

    @Test
    void batchRejectsEndTimeBeforeStartTime() throws Exception {
        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [1],
                                  "startDate": "2026-07-13",
                                  "endDate": "2026-07-17",
                                  "weekdays": [1, 2, 3],
                                  "startTime": "18:00:00",
                                  "endTime": "10:00:00",
                                  "type": "WORK"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("排班结束时间必须晚于开始时间"));
    }

    @Test
    void batchRejectsMissingTherapist() throws Exception {
        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [999999],
                                  "startDate": "2026-07-13",
                                  "endDate": "2026-07-17",
                                  "weekdays": [1, 2, 3],
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("技师不存在或已下架"));
    }

    @Test
    void batchRejectsDuplicateTherapistRowsBeforeCreatingAnything() throws Exception {
        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [1, 1],
                                  "startDate": "2026-07-27",
                                  "endDate": "2026-07-27",
                                  "weekdays": [1],
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请勿重复选择技师"));

        assertThat(scheduleRepository.findByScheduleDateOrderByStartTimeAsc(LocalDate.parse("2026-07-27")))
                .isEmpty();
    }

    @Test
    void batchRejectsExistingOverlapAndCreatesNothing() throws Exception {
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, LocalDate.parse("2026-07-20"),
                LocalTime.of(12, 0), LocalTime.of(14, 0), "WORK", "已有排班"));

        mockMvc.perform(post("/api/admin/schedules/batch")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "therapistIds": [1],
                                  "startDate": "2026-07-20",
                                  "endDate": "2026-07-20",
                                  "weekdays": [1],
                                  "startTime": "10:00:00",
                                  "endTime": "18:00:00",
                                  "type": "WORK",
                                  "note": "冲突排班"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("批量排班与已有排班冲突：技师1 2026-07-20 10:00:00-18:00:00"));

        assertThat(scheduleRepository.findByScheduleDateOrderByStartTimeAsc(LocalDate.parse("2026-07-20")))
                .hasSize(1);
    }
}
