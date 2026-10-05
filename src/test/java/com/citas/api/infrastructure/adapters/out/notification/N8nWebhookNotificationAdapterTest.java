package com.citas.api.infrastructure.adapters.out.notification;

import com.citas.api.domain.model.integration.StatusNotification;
import com.citas.api.infrastructure.config.IntegrationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Adaptador del webhook de WF-002 sin Spring ni base de datos (HU-024). */
class N8nWebhookNotificationAdapterTest {

    private static final String SECRET = String.join("-", "unit", "webhook", "secret", "0123456789abcdefghij");
    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 1, 15, 8, 0);

    @Test
    void ca02_n8nInalcanzableNoLanzaNingunaExcepcion() {
        // Puerto 1: conexión rechazada de inmediato.
        var adapter = new N8nWebhookNotificationAdapter(
                new IntegrationProperties("", "http://127.0.0.1:1/webhook", SECRET), new ObjectMapper());
        assertThatCode(() -> adapter.publish(evento(StatusNotification.Type.APPOINTMENT_CANCELLED, null)))
                .doesNotThrowAnyException();
        adapter.shutdown();
    }

    @Test
    void sinUrlConfiguradaLaIntegracionQuedaDeshabilitada() {
        var adapter = new N8nWebhookNotificationAdapter(new IntegrationProperties("", "", ""), new ObjectMapper());
        assertThatCode(() -> adapter.publish(evento(StatusNotification.Type.SPECIALIZED_APPROVED, null)))
                .doesNotThrowAnyException();
        adapter.shutdown();
    }

    @Test
    @SuppressWarnings("unchecked")
    void elCuerpoLlevaSoloLoNecesarioYLaFirmaEsHmacSha256() throws Exception {
        Map<String, Object> body = N8nWebhookNotificationAdapter.payload(
                evento(StatusNotification.Type.SPECIALIZED_REJECTED, "Sin cupo"));

        assertThat(body).containsOnlyKeys("eventId", "eventType", "occurredAt", "appointment", "patient", "reason",
                "requested", "previous");
        assertThat(body.get("eventType")).isEqualTo("SPECIALIZED_REJECTED");
        assertThat(body.get("reason")).isEqualTo("Sin cupo");
        assertThat((Map<String, Object>) body.get("patient")).containsOnlyKeys("firstNames", "email");
        assertThat((Map<String, Object>) body.get("appointment")).containsEntry("date", "2030-01-15")
                .containsEntry("startTime", "08:00").containsEntry("endTime", "09:00");
        // Vector de referencia público de HMAC-SHA256 (clave "key"): la firma que verifica n8n es estándar.
        assertThat(N8nWebhookNotificationAdapter.sign(
                "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8), "key"))
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void lasPropiedadesExigenSecretosLargosYNoLosImprimen() {
        assertThatCode(() -> new IntegrationProperties("corta", "", "")).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new IntegrationProperties("", "https://n8n.example/webhook/x", "corto"))
                .isInstanceOf(IllegalStateException.class);
        IntegrationProperties props = new IntegrationProperties(SECRET, "https://n8n.example/webhook/x", SECRET);
        assertThat(props.toString()).doesNotContain(SECRET).doesNotContain("n8n.example");
    }

    private static StatusNotification evento(StatusNotification.Type type, String reason) {
        return new StatusNotification("evt-1", type, INICIO.minusDays(1), 31L, "REJECTED", reason, "Ana",
                "ana@example.test", "Cardiología", "Laura Gómez", "HIC", "Hospital Internacional de Colombia",
                INICIO, INICIO.plusHours(1), null, null, null, null);
    }
}
