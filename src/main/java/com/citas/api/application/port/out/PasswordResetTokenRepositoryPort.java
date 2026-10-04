package com.citas.api.application.port.out;

import com.citas.api.domain.model.auth.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByHashForUpdate(String tokenHash);
    void revokeActiveByUserId(Long userId, LocalDateTime now);
}
