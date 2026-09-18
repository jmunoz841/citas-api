package com.citas.api.application.port.in;

/**
 * Par de tokens emitido en login y refresh. Duraciones en segundos.
 */
public record AuthTokens(String accessToken, long accessExpiresInSeconds,
                         String refreshToken, long refreshExpiresInSeconds) {

    @Override
    public String toString() {
        return "AuthTokens[accessToken=***, refreshToken=***]";
    }
}
