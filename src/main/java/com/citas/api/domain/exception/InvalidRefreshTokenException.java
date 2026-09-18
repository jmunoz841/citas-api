package com.citas.api.domain.exception;

/**
 * Refresh token inexistente, expirado, revocado, mal firmado o de otro tipo.
 */
public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "Refresh token inválido");
    }
}
