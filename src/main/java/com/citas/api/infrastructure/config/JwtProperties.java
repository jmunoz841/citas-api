package com.citas.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Configuración JWT desde variables de entorno ({@code JWT_*}). Nunca se registra en logs.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String accessSecret, String refreshSecret, long accessMinutes, long refreshDays) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        requireStrongSecret("JWT_ACCESS_SECRET", accessSecret);
        requireStrongSecret("JWT_REFRESH_SECRET", refreshSecret);
        if (accessSecret.equals(refreshSecret)) {
            throw new IllegalStateException("JWT_ACCESS_SECRET y JWT_REFRESH_SECRET deben ser distintos");
        }
        if (accessMinutes <= 0 || refreshDays <= 0) {
            throw new IllegalStateException("Las duraciones de los tokens deben ser positivas");
        }
    }

    private static void requireStrongSecret(String name, String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(name + " debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
    }

    @Override
    public String toString() {
        return "JwtProperties[accessMinutes=" + accessMinutes + ", refreshDays=" + refreshDays + ", secrets=***]";
    }
}
