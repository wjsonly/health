package com.health.serviceitem;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    @EntityGraph(attributePaths = "category")
    List<ServiceItem> findByStatusOrderBySortOrderAsc(String status);

    @EntityGraph(attributePaths = "category")
    List<ServiceItem> findAllByOrderBySortOrderAsc();

    boolean existsByCategoryIdAndStatus(Long categoryId, String status);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<ServiceItem> findById(Long id);
}
