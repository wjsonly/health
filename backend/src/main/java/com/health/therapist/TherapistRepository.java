package com.health.therapist;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TherapistRepository extends JpaRepository<Therapist, Long> {
    List<Therapist> findByStoreIdAndStatusAndVisibleOrderBySortOrderAsc(Long storeId, String status, boolean visible);

    List<Therapist> findByStoreIdOrderBySortOrderAsc(Long storeId);

    Optional<Therapist> findByIdAndStatusAndVisible(Long id, String status, boolean visible);

    boolean existsByStoreIdAndEmployeeNo(Long storeId, String employeeNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select therapist from Therapist therapist where therapist.id = :id")
    Optional<Therapist> lockById(Long id);

    @Query(value = """
            select count(*)
            from therapist_service_items
            where therapist_id = :therapistId
              and service_item_id = :serviceItemId
            """, nativeQuery = true)
    long countServiceAssignments(Long therapistId, Long serviceItemId);
}
