package com.health.appointment;

import java.time.LocalTime;

public record AvailableSlot(LocalTime startTime, LocalTime endTime) {
}
