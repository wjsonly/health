package com.health.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByIdAndStatus(Long id, String status);
}
