package com.health.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "open_id")
    private String openId;

    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String phone;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected User() {
    }

    public User(Long id, String openId, String nickname, String avatarUrl, String phone, String status) {
        this.id = id;
        this.openId = openId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.status = status;
    }

    public void updateWechatProfile(String nickname, String avatarUrl, String phone) {
        if (StringUtils.hasText(nickname)) {
            this.nickname = nickname;
        }
        if (StringUtils.hasText(avatarUrl)) {
            this.avatarUrl = avatarUrl;
        }
        if (StringUtils.hasText(phone)) {
            this.phone = phone;
        }
        this.status = "ACTIVE";
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

    public String getOpenId() {
        return openId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPhone() {
        return phone;
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
