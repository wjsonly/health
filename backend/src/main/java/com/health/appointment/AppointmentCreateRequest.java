package com.health.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppointmentCreateRequest(
        Long userId,
        @NotNull Long storeId,
        @NotNull Long serviceItemId,
        Long therapistId,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime startTime,
        @NotBlank String contactName,
        @NotBlank String contactPhone,
        String userNote
) {
    public AppointmentCreateRequest withUserId(Long userId) {
        return new AppointmentCreateRequest(
                userId,
                storeId,
                serviceItemId,
                therapistId,
                appointmentDate,
                startTime,
                contactName,
                contactPhone,
                userNote
        );
    }
}
