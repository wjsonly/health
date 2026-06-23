package com.health.appointment;

import java.sql.Date;
import java.sql.Time;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.health.common.BadRequestException;
import com.health.schedule.TherapistSchedule;
import com.health.schedule.TherapistScheduleRepository;
import com.health.serviceitem.ServiceItem;
import com.health.serviceitem.ServiceItemRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AppointmentAvailabilityService {
    private static final String ACTIVE = "ACTIVE";
    private static final String WORK = "WORK";
    private static final Duration SLOT_INCREMENT = Duration.ofMinutes(30);

    private final ServiceItemRepository serviceItemRepository;
    private final TherapistScheduleRepository scheduleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public AppointmentAvailabilityService(
            ServiceItemRepository serviceItemRepository,
            TherapistScheduleRepository scheduleRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.serviceItemRepository = serviceItemRepository;
        this.scheduleRepository = scheduleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public List<AvailableSlot> availableSlots(Long therapistId, Long serviceItemId, LocalDate date) {
        if (therapistId == null || serviceItemId == null || date == null) {
            throw new BadRequestException("预约参数不能为空");
        }
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            throw new BadRequestException("不能预约已过日期");
        }
        ServiceItem serviceItem = serviceItemRepository.findById(serviceItemId)
                .filter(item -> ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new BadRequestException("服务项目不存在或已下架"));

        Duration serviceDuration = Duration.ofMinutes(serviceItem.getDurationMinutes());
        List<TherapistSchedule> schedules = scheduleRepository
                .findByTherapistIdAndScheduleDateOrderByStartTimeAsc(therapistId, date);
        List<TimeRange> blockedRanges = new ArrayList<>(blockedScheduleRanges(schedules));
        blockedRanges.addAll(blockingAppointmentRanges(therapistId, date));

        Map<LocalTime, AvailableSlot> slots = new LinkedHashMap<>();
        schedules.stream()
                .filter(schedule -> WORK.equals(schedule.getType()))
                .forEach(work -> addWorkSlots(slots, work, serviceDuration, blockedRanges));
        return slots.values().stream()
                .filter(slot -> !date.equals(today) || !slot.startTime().isBefore(LocalTime.now(clock)))
                .sorted(Comparator.comparing(AvailableSlot::startTime))
                .toList();
    }

    private void addWorkSlots(Map<LocalTime, AvailableSlot> slots, TherapistSchedule work,
                              Duration serviceDuration, List<TimeRange> blockedRanges) {
        LocalTime start = work.getStartTime();
        while (!start.plus(serviceDuration).isAfter(work.getEndTime())) {
            LocalTime end = start.plus(serviceDuration);
            TimeRange candidate = new TimeRange(start, end);
            if (blockedRanges.stream().noneMatch(candidate::overlaps)) {
                slots.putIfAbsent(start, new AvailableSlot(start, end));
            }
            start = start.plus(SLOT_INCREMENT);
        }
    }

    private List<TimeRange> blockedScheduleRanges(List<TherapistSchedule> schedules) {
        return schedules.stream()
                .filter(schedule -> !WORK.equals(schedule.getType()))
                .map(schedule -> new TimeRange(schedule.getStartTime(), schedule.getEndTime()))
                .toList();
    }

    private List<TimeRange> blockingAppointmentRanges(Long therapistId, LocalDate date) {
        String sql = """
                select start_time, end_time
                from appointments
                where therapist_id = ?
                  and appointment_date = ?
                  and status in (?, ?, ?, ?, ?)
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TimeRange(
                        toLocalTime(rs.getObject("start_time")),
                        toLocalTime(rs.getObject("end_time"))
                ),
                therapistId,
                Date.valueOf(date),
                AppointmentService.STATUS_PAID,
                AppointmentService.STATUS_ARRIVED,
                AppointmentService.STATUS_IN_SERVICE,
                AppointmentService.STATUS_COMPLETED,
                AppointmentService.STATUS_BOOKED);
    }

    private LocalTime toLocalTime(Object value) {
        if (value instanceof LocalTime localTime) {
            return localTime;
        }
        if (value instanceof Time time) {
            return time.toLocalTime();
        }
        throw new BadRequestException("预约时间格式异常");
    }

    private record TimeRange(LocalTime start, LocalTime end) {
        boolean overlaps(TimeRange other) {
            return start.isBefore(other.end) && end.isAfter(other.start);
        }
    }
}
