package com.citas.api.application.port.out;

import com.citas.api.domain.model.auth.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    /** Busca por hash bloqueando la fila, para que dos refresh concurrentes no roten el mismo token. */
    Optional<RefreshToken> findByHashForUpdate(String tokenHash);
}
