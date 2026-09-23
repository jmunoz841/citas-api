package com.citas.api.application.port.out;

import com.citas.api.domain.model.user.User;

import java.time.Instant;
import java.util.Optional;

/**
 * Emisión y lectura de tokens. Access y refresh usan secretos y tipos distintos: uno no sirve como el otro.
 */
public interface TokenProviderPort {

    IssuedToken issueAccessToken(User user, Instant now);

    IssuedToken issueRefreshToken(Long userId, Instant now);

    /** Devuelve el id de usuario si el token es un refresh token bien firmado y no expirado. */
    Optional<Long> parseRefreshToken(String refreshToken);

    /** Hash SHA-256 en hexadecimal minúsculas, el único valor que se persiste. */
    String hash(String token);

    record IssuedToken(String value, Instant expiresAt) {

        @Override
        public String toString() {
            return "IssuedToken[value=***, expiresAt=" + expiresAt + "]";
        }
    }
}
