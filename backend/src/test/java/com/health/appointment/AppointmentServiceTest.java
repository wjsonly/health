package com.health.appointment;

import com.health.common.BadRequestException;
import com.health.schedule.TherapistSchedule;
import com.health.schedule.TherapistScheduleRepository;
import com.health.user.User;
import com.health.user.UserRepository;
import com.health.wechat.MiniProgramTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.health.admin.AdminAuthTestSupport.adminBearerToken;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
class AppointmentServiceTest {
    @Autowired
    AppointmentService appointmentService;

    @Autowired
    TherapistScheduleRepository scheduleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MiniProgramTokenService miniProgramTokenService;

    @Autowired
    MockMvc mockMvc;

    private String bearerToken(User user) {
        return "Bearer " + miniProgramTokenService.issue(user).token();
    }

    @Test
    void createsAppointmentWhenSlotIsAvailable() {
        User user = userRepository.save(new User(null, "openid-1", "测试用户", "", "13900000000", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(11);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));

        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", "肩颈酸"
        ));

        assertThat(appointment.getStatus()).isEqualTo("BOOKED");
        assertThat(appointment.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(appointment.getEndTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void rejectsOverlappingAppointment() {
        User user = userRepository.save(new User(null, "openid-2", "测试用户2", "", "13900000001", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(12);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));

        AppointmentCreateRequest first = new AppointmentCreateRequest(user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", "");
        AppointmentCreateRequest second = new AppointmentCreateRequest(user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 30), "张三", "13900000000", "");

        appointmentService.create(first);

        assertThatThrownBy(() -> appointmentService.create(second))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("该时间段已被预约");
    }

    @Test
    void rejectsTherapistUnsupportedServiceItem() {
        User user = userRepository.save(new User(null, "openid-unsupported-service", "测试用户", "", "13900000006", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(17);
        scheduleRepository.save(new TherapistSchedule(null, 2L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                user.getId(), 1L, 3L, 2L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        );

        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("技师不存在或已下架");
    }

    @Test
    void appliesLegalStatusTransitions() {
        User user = userRepository.save(new User(null, "openid-3", "测试用户3", "", "13900000002", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(13);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        appointmentService.arrive(appointment.getId(), "arrived");
        appointmentService.start(appointment.getId(), "started");
        Appointment completed = appointmentService.complete(appointment.getId(), "completed");

        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getArrivedAt()).isNotNull();
        assertThat(completed.getServiceStartedAt()).isNotNull();
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getAdminNote()).isEqualTo("completed");
    }

    @Test
    void rejectsIllegalStatusTransition() {
        User user = userRepository.save(new User(null, "openid-4", "测试用户4", "", "13900000003", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(14);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        assertThatThrownBy(() -> appointmentService.start(appointment.getId(), "too soon"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("当前订单状态不允许此操作");
    }

    @Test
    void cancelledAppointmentReleasesAvailability() {
        User user = userRepository.save(new User(null, "openid-5", "测试用户5", "", "13900000004", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(15);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        appointmentService.cancelByUser(appointment.getId(), user.getId());
        Appointment replacement = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        assertThat(replacement.getStatus()).isEqualTo("BOOKED");
    }

    @Test
    void createEndpointReturnsApiResponseEnvelope() throws Exception {
        User user = userRepository.save(new User(null, "openid-6", "测试用户6", "", "13900000005", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(16);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));

        mockMvc.perform(post("/api/appointments")
                        .header(AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": 1,
                                  "serviceItemId": 1,
                                  "therapistId": 1,
                                  "appointmentDate": "%s",
                                  "startTime": "10:00:00",
                                  "contactName": "张三",
                                  "contactPhone": "13900000000",
                                  "userNote": "肩颈酸"
                                }
                                """.formatted(date)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.status").value("BOOKED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"));
    }

    @Test
    void publicListUsesAuthenticatedMiniProgramUser() throws Exception {
        User user = userRepository.save(new User(null, "openid-list-owner", "预约用户", "", "13900000110", "ACTIVE"));
        User other = userRepository.save(new User(null, "openid-list-other", "其他用户", "", "13900000111", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(22);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        mockMvc.perform(get("/api/appointments")
                        .header(AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/appointments")
                        .header(AUTHORIZATION, bearerToken(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void publicDetailRejectsAppointmentOwnedByAnotherUser() throws Exception {
        User owner = userRepository.save(new User(null, "openid-owner-detail", "预约用户", "", "13900000100", "ACTIVE"));
        User other = userRepository.save(new User(null, "openid-other-detail", "其他用户", "", "13900000101", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(18);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                owner.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        mockMvc.perform(get("/api/appointments/{id}", appointment.getId())
                        .header(AUTHORIZATION, bearerToken(other)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("预约不存在"));
    }

    @Test
    void publicCancelRejectsAppointmentOwnedByAnotherUser() throws Exception {
        User owner = userRepository.save(new User(null, "openid-owner-cancel", "预约用户", "", "13900000102", "ACTIVE"));
        User other = userRepository.save(new User(null, "openid-other-cancel", "其他用户", "", "13900000103", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(19);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                owner.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        mockMvc.perform(patch("/api/appointments/{id}/cancel", appointment.getId())
                        .header(AUTHORIZATION, bearerToken(other)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("预约不存在"));

        assertThat(appointmentService.get(appointment.getId()).getStatus()).isEqualTo("BOOKED");
    }

    @Test
    void publicResponseOmitsAdminOnlyFieldsButAdminResponseIncludesThem() throws Exception {
        User user = userRepository.save(new User(null, "openid-response-fields", "测试用户", "", "13900000104", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(20);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));
        appointmentService.arrive(appointment.getId(), "到店备注");

        mockMvc.perform(get("/api/appointments/{id}", appointment.getId())
                        .header(AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactName").value("张三"))
                .andExpect(jsonPath("$.data.adminNote").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.arrivedAt").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.serviceStartedAt").doesNotHaveJsonPath());

        mockMvc.perform(get("/api/admin/appointments")
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].adminNote").value("到店备注"))
                .andExpect(jsonPath("$.data[0].arrivedAt").exists());
    }

    @Test
    void adminTransitionRejectsConflictingStatusBody() throws Exception {
        User user = userRepository.save(new User(null, "openid-conflicting-status", "测试用户", "", "13900000105", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(21);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));
        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", ""
        ));

        mockMvc.perform(patch("/api/admin/appointments/{id}/arrive", appointment.getId())
                        .header(AUTHORIZATION, adminBearerToken(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED",
                                  "adminNote": "wrong action"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("状态参数与操作不一致"));
    }
}
