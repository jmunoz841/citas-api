package com.citas.api.domain.model.auth;

import java.time.LocalDateTime;

/** Token de recuperación persistido exclusivamente como hash. */
public final class PasswordResetToken {
    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private final LocalDateTime revokedAt;

    private PasswordResetToken(Long id, Long userId, String tokenHash, LocalDateTime createdAt,
                               LocalDateTime expiresAt, LocalDateTime usedAt, LocalDateTime revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
    }

    public static PasswordResetToken issue(Long userId, String tokenHash, LocalDateTime now, LocalDateTime expiresAt) {
        return new PasswordResetToken(null, userId, tokenHash, now, expiresAt, null, null);
    }

    public static PasswordResetToken restore(Long id, Long userId, String tokenHash, LocalDateTime createdAt,
                                             LocalDateTime expiresAt, LocalDateTime usedAt, LocalDateTime revokedAt) {
        return new PasswordResetToken(id, userId, tokenHash, createdAt, expiresAt, usedAt, revokedAt);
    }

    public boolean isUsableAt(LocalDateTime now) {
        return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
    }

    public void consume(LocalDateTime now) {
        if (!isUsableAt(now)) throw new IllegalStateException("El token no se puede consumir");
        usedAt = now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
}
