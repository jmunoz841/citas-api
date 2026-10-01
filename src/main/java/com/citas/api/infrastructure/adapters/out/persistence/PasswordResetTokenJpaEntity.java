package com.citas.api.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "char(64)") private String tokenHash;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    @Column(name = "revoked_at") private LocalDateTime revokedAt;

    protected PasswordResetTokenJpaEntity() { }
    public PasswordResetTokenJpaEntity(Long id, Long userId, String tokenHash, LocalDateTime createdAt,
                                       LocalDateTime expiresAt, LocalDateTime usedAt, LocalDateTime revokedAt) {
        this.id = id; this.userId = userId; this.tokenHash = tokenHash; this.createdAt = createdAt;
        this.expiresAt = expiresAt; this.usedAt = usedAt; this.revokedAt = revokedAt;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
}
