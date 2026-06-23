package com.health.store;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String phone;

    @Column(name = "business_start")
    private LocalTime businessStart;

    @Column(name = "business_end")
    private LocalTime businessEnd;

    private String announcement;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Store() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getPhone() {
        return phone;
    }

    public LocalTime getBusinessStart() {
        return businessStart;
    }

    public LocalTime getBusinessEnd() {
        return businessEnd;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
