package com.health.serviceitem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_items")
public class ServiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "suitable_people")
    private String suitablePeople;

    private String highlights;

    private String notice;

    private boolean hot;

    private boolean recommended;

    private String status;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ServiceItem() {
    }

    public static ServiceItem create(ServiceItemRequest request, ServiceCategory category) {
        ServiceItem item = new ServiceItem();
        item.updateFrom(request, category);
        return item;
    }

    public void updateFrom(ServiceItemRequest request, ServiceCategory category) {
        this.category = category;
        name = request.name().trim();
        imageUrl = request.imageUrl();
        durationMinutes = request.durationMinutes();
        originalPrice = request.originalPrice();
        salePrice = request.salePrice();
        suitablePeople = request.suitablePeople();
        highlights = request.highlights();
        notice = request.notice();
        hot = request.hot();
        recommended = request.recommended();
        status = request.status();
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

    public ServiceCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public String getSuitablePeople() {
        return suitablePeople;
    }

    public String getHighlights() {
        return highlights;
    }

    public String getNotice() {
        return notice;
    }

    public boolean isHot() {
        return hot;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public String getStatus() {
        return status;
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
