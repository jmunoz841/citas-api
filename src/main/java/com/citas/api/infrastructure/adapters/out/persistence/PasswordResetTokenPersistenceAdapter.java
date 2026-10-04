package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.PasswordResetTokenRepositoryPort;
import com.citas.api.domain.model.auth.PasswordResetToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenRepositoryPort {
    private final PasswordResetTokenJpaRepository repository;
    PasswordResetTokenPersistenceAdapter(PasswordResetTokenJpaRepository repository) { this.repository = repository; }
    @Override public PasswordResetToken save(PasswordResetToken token) {
        return toDomain(repository.saveAndFlush(toEntity(token)));
    }
    @Override public Optional<PasswordResetToken> findByHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(PasswordResetTokenPersistenceAdapter::toDomain);
    }
    @Override public void revokeActiveByUserId(Long userId, LocalDateTime now) { repository.revokeActiveByUserId(userId, now); }
    private static PasswordResetTokenJpaEntity toEntity(PasswordResetToken t) {
        return new PasswordResetTokenJpaEntity(t.getId(), t.getUserId(), t.getTokenHash(), t.getCreatedAt(),
                t.getExpiresAt(), t.getUsedAt(), t.getRevokedAt());
    }
    private static PasswordResetToken toDomain(PasswordResetTokenJpaEntity t) {
        return PasswordResetToken.restore(t.getId(), t.getUserId(), t.getTokenHash(), t.getCreatedAt(),
                t.getExpiresAt(), t.getUsedAt(), t.getRevokedAt());
    }
}
