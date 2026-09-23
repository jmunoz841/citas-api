package com.citas.api.domain.model.auth;

import java.time.LocalDateTime;

/**
 * Refresh token persistido. Solo se guarda el hash (D-003); el valor en claro lo recibe únicamente el cliente.
 */
public final class RefreshToken {

    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private Long replacedByTokenId;

    private RefreshToken(Long id, Long userId, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt,
                         LocalDateTime revokedAt, Long replacedByTokenId) {
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt debe ser posterior a issuedAt");
        }
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    public static RefreshToken issue(Long userId, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        return new RefreshToken(null, userId, tokenHash, issuedAt, expiresAt, null, null);
    }

    public static RefreshToken restore(Long id, Long userId, String tokenHash, LocalDateTime issuedAt,
                                       LocalDateTime expiresAt, LocalDateTime revokedAt, Long replacedByTokenId) {
        return new RefreshToken(id, userId, tokenHash, issuedAt, expiresAt, revokedAt, replacedByTokenId);
    }

    public boolean isUsableAt(LocalDateTime now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(LocalDateTime now) {
        if (revokedAt == null) {
            revokedAt = now.isBefore(issuedAt) ? issuedAt : now;
        }
    }

    /** Rotación: el token usado queda revocado y enlazado a su reemplazo. */
    public void rotateTo(Long newTokenId, LocalDateTime now) {
        revoke(now);
        replacedByTokenId = newTokenId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public Long getReplacedByTokenId() { return replacedByTokenId; }
}
