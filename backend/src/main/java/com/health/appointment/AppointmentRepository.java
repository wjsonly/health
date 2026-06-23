package com.health.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserIdOrderByAppointmentDateDescStartTimeDesc(Long userId);

    Optional<Appointment> findByIdAndUserId(Long id, Long userId);

    List<Appointment> findByAppointmentDateOrderByStartTimeAsc(LocalDate appointmentDate);

    @Query("""
            select count(appointment) > 0
            from Appointment appointment
            where appointment.therapistId = :therapistId
              and appointment.appointmentDate = :appointmentDate
              and appointment.status in :statuses
              and appointment.startTime < :endTime
              and appointment.endTime > :startTime
            """)
    boolean existsBlockingOverlap(Long therapistId, LocalDate appointmentDate, LocalTime startTime,
                                  LocalTime endTime, Collection<String> statuses);
}
