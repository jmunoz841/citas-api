package com.citas.api.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetTokenJpaEntity t where t.tokenHash = :tokenHash")
    Optional<PasswordResetTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenJpaEntity t set t.revokedAt = :now where t.userId = :userId "
            + "and t.usedAt is null and t.revokedAt is null and t.expiresAt > :now")
    void revokeActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
