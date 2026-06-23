package com.health.therapist;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "therapists")
public class Therapist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id")
    private Long storeId;

    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String gender;

    private String phone;

    @Column(name = "employee_no")
    private String employeeNo;

    @Column(name = "years_of_experience")
    private int yearsOfExperience;

    private String level;

    private String status;

    private String introduction;

    private String specialties;

    @Column(name = "service_tags")
    private String serviceTags;

    @Column(name = "certificate_urls")
    private String certificateUrls;

    private boolean bookable;

    private boolean visible;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Therapist() {
    }

    public static Therapist create(TherapistCreateRequest request) {
        Therapist therapist = new Therapist();
        therapist.storeId = request.storeId();
        therapist.employeeNo = request.employeeNo();
        therapist.updateFrom(request);
        return therapist;
    }

    public void updateFrom(TherapistUpdateRequest request) {
        name = request.name();
        avatarUrl = request.avatarUrl();
        gender = request.gender();
        phone = request.phone();
        yearsOfExperience = request.yearsOfExperience();
        level = request.level();
        status = request.status();
        introduction = request.introduction();
        specialties = request.specialties();
        serviceTags = request.serviceTags();
        certificateUrls = request.certificateUrls();
        bookable = request.bookable();
        visible = request.visible();
        sortOrder = request.sortOrder();
    }

    public void updateFrom(TherapistCreateRequest request) {
        name = request.name();
        avatarUrl = request.avatarUrl();
        gender = request.gender();
        phone = request.phone();
        yearsOfExperience = request.yearsOfExperience();
        level = request.level();
        status = request.status();
        introduction = request.introduction();
        specialties = request.specialties();
        serviceTags = request.serviceTags();
        certificateUrls = request.certificateUrls();
        bookable = request.bookable();
        visible = request.visible();
        sortOrder = request.sortOrder();
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getLevel() {
        return level;
    }

    public String getStatus() {
        return status;
    }

    public String getIntroduction() {
        return introduction;
    }

    public String getSpecialties() {
        return specialties;
    }

    public String getServiceTags() {
        return serviceTags;
    }

    public String getCertificateUrls() {
        return certificateUrls;
    }

    public boolean isBookable() {
        return bookable;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
