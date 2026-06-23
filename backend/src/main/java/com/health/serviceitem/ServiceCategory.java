package com.health.serviceitem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_categories")
public class ServiceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "sort_order")
    private int sortOrder;

    private boolean enabled;

    protected ServiceCategory() {
    }

    public static ServiceCategory create(ServiceCategoryRequest request) {
        ServiceCategory category = new ServiceCategory();
        category.updateFrom(request);
        return category;
    }

    public void updateFrom(ServiceCategoryRequest request) {
        name = request.name().trim();
        sortOrder = request.sortOrder();
        enabled = request.enabled();
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
