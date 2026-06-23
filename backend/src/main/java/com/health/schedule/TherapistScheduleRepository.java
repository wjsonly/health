package com.health.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TherapistScheduleRepository extends JpaRepository<TherapistSchedule, Long> {
    List<TherapistSchedule> findByTherapistIdAndScheduleDateOrderByStartTimeAsc(
            Long therapistId,
            LocalDate scheduleDate
    );

    List<TherapistSchedule> findByStoreIdAndScheduleDateOrderByStartTimeAsc(
            Long storeId,
            LocalDate scheduleDate
    );

    List<TherapistSchedule> findByScheduleDateOrderByStartTimeAsc(LocalDate scheduleDate);

    @Query("""
            select case when count(schedule) > 0 then true else false end
            from TherapistSchedule schedule
            where schedule.therapistId = :therapistId
              and schedule.scheduleDate = :scheduleDate
              and schedule.startTime < :endTime
              and schedule.endTime > :startTime
            """)
    boolean existsOverlap(Long therapistId, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime);
}
