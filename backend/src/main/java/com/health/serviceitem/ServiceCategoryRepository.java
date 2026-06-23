package com.health.serviceitem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findAllByOrderBySortOrderAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
