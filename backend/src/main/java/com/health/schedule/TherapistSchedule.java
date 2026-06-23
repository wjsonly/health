package com.health.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "therapist_schedules")
public class TherapistSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "therapist_id")
    private Long therapistId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    private String type;

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected TherapistSchedule() {
    }

    public TherapistSchedule(Long id, Long therapistId, Long storeId, LocalDate scheduleDate,
                             LocalTime startTime, LocalTime endTime, String type, String note) {
        this.id = id;
        this.therapistId = therapistId;
        this.storeId = storeId;
        this.scheduleDate = scheduleDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.note = note;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTherapistId() {
        return therapistId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getType() {
        return type;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
