package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.RefreshTokenRepositoryPort;
import com.citas.api.domain.model.auth.RefreshToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return toDomain(repository.saveAndFlush(toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshTokenPersistenceAdapter::toDomain);
    }

    private static RefreshTokenJpaEntity toEntity(RefreshToken token) {
        return new RefreshTokenJpaEntity(token.getId(), token.getUserId(), token.getTokenHash(),
                token.getIssuedAt(), token.getExpiresAt(), token.getRevokedAt(), token.getReplacedByTokenId());
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.restore(entity.getId(), entity.getUserId(), entity.getTokenHash(), entity.getIssuedAt(),
                entity.getExpiresAt(), entity.getRevokedAt(), entity.getReplacedByTokenId());
    }
}
