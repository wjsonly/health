package com.health.appointment;

import java.math.BigDecimal;
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
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "service_item_id")
    private Long serviceItemId;

    @Column(name = "therapist_id")
    private Long therapistId;

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "item_amount")
    private BigDecimal itemAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "payment_status")
    private String paymentStatus;

    private String status;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "user_note")
    private String userNote;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "service_started_at")
    private LocalDateTime serviceStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Appointment() {
    }

    static Appointment create(AppointmentCreateRequest request, LocalTime endTime, BigDecimal itemAmount) {
        Appointment appointment = new Appointment();
        appointment.userId = request.userId();
        appointment.storeId = request.storeId();
        appointment.serviceItemId = request.serviceItemId();
        appointment.therapistId = request.therapistId();
        appointment.appointmentDate = request.appointmentDate();
        appointment.startTime = request.startTime();
        appointment.endTime = endTime;
        appointment.itemAmount = itemAmount;
        appointment.discountAmount = BigDecimal.ZERO;
        appointment.paidAmount = BigDecimal.ZERO;
        appointment.paymentStatus = AppointmentService.PAYMENT_UNPAID;
        appointment.status = AppointmentService.STATUS_BOOKED;
        appointment.contactName = request.contactName();
        appointment.contactPhone = request.contactPhone();
        appointment.userNote = request.userNote();
        return appointment;
    }

    void transitionTo(String nextStatus, String adminNote) {
        status = nextStatus;
        this.adminNote = adminNote;
        LocalDateTime now = LocalDateTime.now();
        if (AppointmentService.STATUS_ARRIVED.equals(nextStatus)) {
            arrivedAt = now;
        } else if (AppointmentService.STATUS_IN_SERVICE.equals(nextStatus)) {
            serviceStartedAt = now;
        } else if (AppointmentService.STATUS_COMPLETED.equals(nextStatus)) {
            completedAt = now;
        } else if (AppointmentService.STATUS_CANCELLED.equals(nextStatus)) {
            cancelledAt = now;
        }
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getServiceItemId() {
        return serviceItemId;
    }

    public Long getTherapistId() {
        return therapistId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public BigDecimal getItemAmount() {
        return itemAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getUserNote() {
        return userNote;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public LocalDateTime getServiceStartedAt() {
        return serviceStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
