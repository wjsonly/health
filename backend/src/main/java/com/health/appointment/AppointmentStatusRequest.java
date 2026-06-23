package com.health.appointment;

import jakarta.validation.constraints.NotBlank;

public record AppointmentStatusRequest(
        @NotBlank String status,
        String adminNote
) {
}
