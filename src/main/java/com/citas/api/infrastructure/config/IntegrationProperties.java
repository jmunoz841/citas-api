package com.citas.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Integración con n8n (D-042, D-043), desde {@code INTEGRATION_API_KEY}, {@code N8N_WEBHOOK_URL} y
 * {@code N8N_WEBHOOK_SECRET}. Vacías = integración deshabilitada. Nunca se registran en logs.
 *
 * @param apiKey        clave que n8n envía en {@code X-Api-Key} para {@code /api/v1/integrations/**}
 * @param webhookUrl    URL del webhook de WF-002 al que la API notifica cambios de estado
 * @param webhookSecret secreto con el que la API firma el cuerpo de cada notificación (HMAC-SHA256)
 */
@ConfigurationProperties(prefix = "app.integration")
public record IntegrationProperties(String apiKey, String webhookUrl, String webhookSecret) {

    private static final int MIN_SECRET_BYTES = 32;

    public IntegrationProperties {
        apiKey = apiKey == null ? "" : apiKey.trim();
        webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        if (!apiKey.isEmpty() && apiKey.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("INTEGRATION_API_KEY debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        if (!webhookUrl.isEmpty() && webhookSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("N8N_WEBHOOK_SECRET debe tener al menos " + MIN_SECRET_BYTES
                    + " bytes cuando N8N_WEBHOOK_URL está definida");
        }
    }

    public boolean apiKeyEnabled() {
        return !apiKey.isEmpty();
    }

    public boolean webhookEnabled() {
        return !webhookUrl.isEmpty();
    }

    @Override
    public String toString() {
        return "IntegrationProperties[apiKeyEnabled=" + apiKeyEnabled() + ", webhookEnabled=" + webhookEnabled()
                + ", secrets=***]";
    }
}
