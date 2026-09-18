package com.citas.api.application.port.in;

public interface LogoutUseCase {

    /** Revoca el refresh token. Idempotente: un token desconocido o ya revocado no produce error. */
    void logout(String refreshToken);
}
