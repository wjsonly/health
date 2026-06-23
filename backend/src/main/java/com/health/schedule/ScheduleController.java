package com.health.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.health.appointment.AppointmentAvailabilityService;
import com.health.appointment.AvailableSlot;
import com.health.common.ApiResponse;
import com.health.common.BadRequestException;
import com.health.store.StoreRepository;
import com.health.therapist.Therapist;
import com.health.therapist.TherapistRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ScheduleController {
    private static final Set<String> SUPPORTED_TYPES = Set.of("WORK", "REST", "LEAVE", "BLOCKED");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TherapistScheduleRepository scheduleRepository;
    private final AppointmentAvailabilityService availabilityService;
    private final TherapistRepository therapistRepository;
    private final StoreRepository storeRepository;

    public ScheduleController(
            TherapistScheduleRepository scheduleRepository,
            AppointmentAvailabilityService availabilityService,
            TherapistRepository therapistRepository,
            StoreRepository storeRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.availabilityService = availabilityService;
        this.therapistRepository = therapistRepository;
        this.storeRepository = storeRepository;
    }

    @GetMapping("/api/admin/schedules")
    public ApiResponse<List<ScheduleResponse>> listSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(scheduleRepository.findByScheduleDateOrderByStartTimeAsc(date).stream()
                .map(ScheduleResponse::from)
                .toList());
    }

    @PostMapping("/api/admin/schedules")
    public ApiResponse<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        if (!SUPPORTED_TYPES.contains(request.type())) {
            throw new BadRequestException("排班类型不支持");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("排班结束时间必须晚于开始时间");
        }
        if (!therapistRepository.existsById(request.therapistId())) {
            throw new BadRequestException("技师不存在或已下架");
        }
        if (!storeRepository.existsById(request.storeId())) {
            throw new BadRequestException("门店信息未配置");
        }
        TherapistSchedule schedule = new TherapistSchedule(null, request.therapistId(), request.storeId(),
                request.scheduleDate(), request.startTime(), request.endTime(), request.type(), request.note());
        return ApiResponse.ok(ScheduleResponse.from(scheduleRepository.save(schedule)));
    }

    @Transactional
    @PostMapping("/api/admin/schedules/batch")
    public ApiResponse<ScheduleBatchResponse> createSchedulesBatch(
            @Valid @RequestBody ScheduleBatchRequest request
    ) {
        validateBatchRequest(request);
        Map<Long, Therapist> therapistsById = therapistRepository.findAllById(request.therapistIds()).stream()
                .collect(Collectors.toMap(Therapist::getId, Function.identity()));
        for (Long therapistId : request.therapistIds()) {
            if (!therapistsById.containsKey(therapistId)) {
                throw new BadRequestException("技师不存在或已下架");
            }
        }

        List<TherapistSchedule> schedules = buildBatchSchedules(request, therapistsById);
        validateBatchConflicts(schedules);
        List<ScheduleResponse> created = scheduleRepository.saveAll(schedules).stream()
                .map(ScheduleResponse::from)
                .toList();
        return ApiResponse.ok(new ScheduleBatchResponse(created.size(), created));
    }

    @DeleteMapping("/api/admin/schedules/{id}")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/appointments/available-slots")
    public ApiResponse<List<AvailableSlot>> availableSlots(
            @RequestParam @NotNull Long therapistId,
            @RequestParam @NotNull Long serviceItemId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(availabilityService.availableSlots(therapistId, serviceItemId, date));
    }

    record ScheduleCreateRequest(
            @NotNull Long therapistId,
            @NotNull Long storeId,
            @NotNull LocalDate scheduleDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotBlank String type,
            String note
    ) {
    }

    record ScheduleBatchResponse(int createdCount, List<ScheduleResponse> schedules) {
    }

    record ScheduleResponse(
            Long id,
            Long therapistId,
            Long storeId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            String type,
            String note
    ) {
        static ScheduleResponse from(TherapistSchedule schedule) {
            return new ScheduleResponse(
                    schedule.getId(),
                    schedule.getTherapistId(),
                    schedule.getStoreId(),
                    schedule.getScheduleDate(),
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getType(),
                    schedule.getNote()
            );
        }
    }

    private void validateBatchRequest(ScheduleBatchRequest request) {
        if (!SUPPORTED_TYPES.contains(request.type())) {
            throw new BadRequestException("排班类型不支持");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("排班结束时间必须晚于开始时间");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("排班结束日期必须不早于开始日期");
        }
        if (request.weekdays().isEmpty()) {
            throw new BadRequestException("请选择排班星期");
        }
        if (request.weekdays().stream().anyMatch(weekday -> weekday < 1 || weekday > 7)) {
            throw new BadRequestException("排班星期参数不合法");
        }
        if (new HashSet<>(request.therapistIds()).size() != request.therapistIds().size()) {
            throw new BadRequestException("请勿重复选择技师");
        }
    }

    private List<TherapistSchedule> buildBatchSchedules(
            ScheduleBatchRequest request,
            Map<Long, Therapist> therapistsById
    ) {
        Set<Integer> weekdays = new HashSet<>(request.weekdays());
        List<TherapistSchedule> schedules = new ArrayList<>();
        LocalDate date = request.startDate();
        while (!date.isAfter(request.endDate())) {
            if (weekdays.contains(date.getDayOfWeek().getValue())) {
                for (Long therapistId : request.therapistIds()) {
                    Therapist therapist = therapistsById.get(therapistId);
                    schedules.add(new TherapistSchedule(null, therapistId, therapist.getStoreId(),
                            date, request.startTime(), request.endTime(), request.type(), request.note()));
                }
            }
            date = date.plusDays(1);
        }
        if (schedules.isEmpty()) {
            throw new BadRequestException("所选日期范围内没有匹配的星期");
        }
        return schedules;
    }

    private void validateBatchConflicts(List<TherapistSchedule> schedules) {
        for (TherapistSchedule schedule : schedules) {
            if (scheduleRepository.existsOverlap(
                    schedule.getTherapistId(),
                    schedule.getScheduleDate(),
                    schedule.getStartTime(),
                    schedule.getEndTime()
            )) {
                throw new BadRequestException("批量排班与已有排班冲突：技师"
                        + schedule.getTherapistId()
                        + " "
                        + schedule.getScheduleDate()
                        + " "
                        + schedule.getStartTime().format(TIME_FORMATTER)
                        + "-"
                        + schedule.getEndTime().format(TIME_FORMATTER));
            }
        }
    }
}
