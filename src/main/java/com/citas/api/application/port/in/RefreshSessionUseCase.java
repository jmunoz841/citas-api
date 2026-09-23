package com.citas.api.application.port.in;

public interface RefreshSessionUseCase {

    /** Rota el refresh token: emite un par nuevo y revoca el recibido (D-003). */
    AuthTokens refresh(String refreshToken);
}
