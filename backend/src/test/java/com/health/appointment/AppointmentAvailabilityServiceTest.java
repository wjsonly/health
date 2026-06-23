package com.health.appointment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.health.schedule.TherapistSchedule;
import com.health.schedule.TherapistScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:availability;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Import(AppointmentAvailabilityServiceTest.FixedClockConfiguration.class)
class AppointmentAvailabilityServiceTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

    @Autowired
    AppointmentAvailabilityService availabilityService;

    @Autowired
    TherapistScheduleRepository scheduleRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void returnsThirtyMinuteSlotsThatCanFitTheServiceDuration() {
        LocalDate date = TODAY.plusDays(1);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0),
                LocalTime.of(12, 0), "WORK", "morning"));

        List<AvailableSlot> slots = availabilityService.availableSlots(1L, 1L, date);

        assertThat(slots).extracting(AvailableSlot::startTime)
                .contains(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0));
        assertThat(slots).extracting(AvailableSlot::startTime)
                .doesNotContain(LocalTime.of(11, 30));
    }

    @Test
    void removesSlotsThatOverlapRestBlockedOrLeaveSchedules() {
        LocalDate date = TODAY.plusDays(2);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0),
                LocalTime.of(15, 0), "WORK", "day"));
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(11, 0),
                LocalTime.of(12, 0), "REST", "rest"));
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(13, 0),
                LocalTime.of(13, 30), "BLOCKED", "blocked"));
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(14, 0),
                LocalTime.of(15, 0), "LEAVE", "leave"));

        List<AvailableSlot> slots = availabilityService.availableSlots(1L, 1L, date);

        assertThat(slots).extracting(AvailableSlot::startTime)
                .containsExactly(LocalTime.of(10, 0), LocalTime.of(12, 0));
    }

    @Test
    void removesSlotsThatOverlapBlockingAppointments() {
        LocalDate date = TODAY.plusDays(3);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0),
                LocalTime.of(13, 0), "WORK", "day"));
        insertUser(100L, "availability-user");
        insertAppointment(100L, date, LocalTime.of(10, 30), LocalTime.of(11, 30), "PAID");

        List<AvailableSlot> slots = availabilityService.availableSlots(1L, 1L, date);

        assertThat(slots).extracting(AvailableSlot::startTime)
                .containsExactly(LocalTime.of(11, 30), LocalTime.of(12, 0));
    }

    @Test
    void bookedAppointmentsBlockOverlappingSlotsButRespectHalfOpenBoundaries() {
        LocalDate date = TODAY.plusDays(5);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0),
                LocalTime.of(14, 0), "WORK", "day"));
        insertUser(101L, "availability-booked-user");
        insertAppointment(101L, date, LocalTime.of(11, 0), LocalTime.of(12, 0), "BOOKED");

        List<AvailableSlot> slots = availabilityService.availableSlots(1L, 1L, date);

        assertThat(slots).extracting(AvailableSlot::startTime)
                .containsExactly(LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0));
    }

    @Test
    void serviceDurationControlsHowLateSlotsCanStart() {
        LocalDate date = TODAY.plusDays(4);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0),
                LocalTime.of(12, 0), "WORK", "day"));

        List<AvailableSlot> slots = availabilityService.availableSlots(1L, 3L, date);

        assertThat(slots)
                .containsExactly(
                        new AvailableSlot(LocalTime.of(10, 0), LocalTime.of(10, 45)),
                        new AvailableSlot(LocalTime.of(10, 30), LocalTime.of(11, 15)),
                        new AvailableSlot(LocalTime.of(11, 0), LocalTime.of(11, 45))
                );
    }

    @Test
    void rejectsPastDates() {
        LocalDate date = TODAY.minusDays(1);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0),
                LocalTime.of(14, 0), "WORK", "past"));

        assertThatThrownBy(() -> availabilityService.availableSlots(1L, 1L, date))
                .hasMessage("不能预约已过日期");
    }

    @Test
    void removesTodaySlotsThatHaveAlreadyStarted() {
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, TODAY, LocalTime.of(10, 0),
                LocalTime.of(15, 0), "WORK", "today"));

        List<AvailableSlot> slots = availabilityService.availableSlots(1L, 1L, TODAY);

        assertThat(slots).extracting(AvailableSlot::startTime)
                .containsExactly(LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0),
                        LocalTime.of(13, 30), LocalTime.of(14, 0));
    }

    private void insertUser(Long id, String openId) {
        jdbcTemplate.update("""
                        insert into users (id, open_id, nickname, phone, status)
                        values (?, ?, '测试用户', '13800000000', 'ACTIVE')
                        """,
                id, openId);
    }

    private void insertAppointment(Long userId, LocalDate date, LocalTime startTime, LocalTime endTime, String status) {
        jdbcTemplate.update("""
                        insert into appointments (
                          user_id, store_id, service_item_id, therapist_id, appointment_date,
                          start_time, end_time, item_amount, discount_amount, paid_amount,
                          payment_status, status, contact_name, contact_phone
                        )
                        values (?, 1, 1, 1, ?, ?, ?, 168.00, 0, 168.00, 'PAID', ?, '测试用户', '13800000000')
                        """,
                userId, date, startTime, endTime, status);
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(Instant.parse("2030-01-15T04:00:00Z"), BUSINESS_ZONE);
        }
    }
}
