package com.health.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOpenId(String openId);

    Optional<User> findByIdAndStatus(Long id, String status);
}
