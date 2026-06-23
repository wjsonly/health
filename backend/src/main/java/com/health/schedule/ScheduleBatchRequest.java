package com.health.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ScheduleBatchRequest(
        @NotEmpty(message = "请选择技师") List<Long> therapistIds,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty(message = "请选择排班星期") List<Integer> weekdays,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank String type,
        String note
) {
}
