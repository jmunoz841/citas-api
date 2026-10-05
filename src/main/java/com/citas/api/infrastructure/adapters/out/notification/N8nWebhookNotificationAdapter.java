package com.citas.api.infrastructure.adapters.out.notification;

import com.citas.api.application.port.out.StatusNotificationPort;
import com.citas.api.domain.model.integration.StatusNotification;
import com.citas.api.infrastructure.config.IntegrationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entrega los cambios de estado al webhook de WF-002 en n8n (HU-024, D-042).
 *
 * <ul>
 *   <li>Se envía <b>después del commit</b>: si la transacción se deshace, no sale ningún evento.</li>
 *   <li>Se envía en un hilo aparte con timeouts cortos: n8n lento o caído no retrasa ni revierte la
 *       respuesta de la API (CA-02).</li>
 *   <li>El cuerpo va firmado: {@code X-Citas-Signature: sha256=<HMAC-SHA256 hex>} con
 *       {@code N8N_WEBHOOK_SECRET}; WF-002 lo verifica antes de enviar el correo.</li>
 *   <li>Un fallo solo se registra con el id y el tipo del evento, nunca con datos del paciente.</li>
 * </ul>
 * Sin {@code N8N_WEBHOOK_URL} no hace nada.
 */
@Component
class N8nWebhookNotificationAdapter implements StatusNotificationPort {

    static final String SIGNATURE_HEADER = "X-Citas-Signature";
    static final String EVENT_HEADER = "X-Citas-Event";
    static final String EVENT_ID_HEADER = "X-Citas-Event-Id";
    static final String TOKEN_HEADER = "X-Citas-Token";

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookNotificationAdapter.class);

    private final IntegrationProperties properties;
    private final ObjectMapper json;
    private final HttpClient http;
    private final ExecutorService executor;

    N8nWebhookNotificationAdapter(IntegrationProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "n8n-webhook");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void publish(StatusNotification notification) {
        if (!properties.webhookEnabled()) {
            return;
        }
        byte[] body;
        try {
            body = json.writeValueAsBytes(payload(notification));
        } catch (Exception e) {
            log.warn("No se pudo serializar el evento {} ({})", notification.eventId(), notification.type());
            return;
        }
        Runnable send = () -> executor.execute(() -> send(notification, body));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private void send(StatusNotification notification, byte[] body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.webhookUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header(EVENT_HEADER, notification.type().name())
                    .header(EVENT_ID_HEADER, notification.eventId())
                    .header(SIGNATURE_HEADER, "sha256=" + sign(body, properties.webhookSecret()))
                    // n8n autentica el webhook con una credencial Header Auth sobre esta cabecera; así el
                    // secreto vive en la credencial y nunca en el JSON del workflow (HTTPS obligatorio).
                    .header(TOKEN_HEADER, properties.webhookSecret())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                log.warn("El webhook de n8n respondió {} al evento {} ({})", response.statusCode(),
                        notification.eventId(), notification.type());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("No se pudo entregar el evento {} ({}) a n8n: {}", notification.eventId(), notification.type(),
                    e.getClass().getSimpleName());
        }
    }

    static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }

    /** Contrato del cuerpo: `docs/contratos/integraciones.md` § Webhook de cambio de estado. */
    static Map<String, Object> payload(StatusNotification n) {
        Map<String, Object> appointment = new LinkedHashMap<>();
        appointment.put("id", n.appointmentId());
        appointment.put("status", n.status());
        appointment.put("specialtyName", n.specialtyName());
        appointment.put("professionalName", n.professionalName());
        appointment.put("siteCode", n.siteCode());
        appointment.put("siteName", n.siteName());
        appointment.put("date", n.startAt().toLocalDate().toString());
        appointment.put("startTime", n.startAt().toLocalTime().toString());
        appointment.put("endTime", n.endAt().toLocalTime().toString());

        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("firstNames", n.patientFirstNames());
        patient.put("email", n.patientEmail());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", n.eventId());
        body.put("eventType", n.type().name());
        body.put("occurredAt", n.occurredAt().toString());
        body.put("appointment", appointment);
        body.put("patient", patient);
        body.put("reason", n.reason());
        body.put("requested", slot(n.requestedStartAt(), n.requestedSiteCode()));
        body.put("previous", slot(n.previousStartAt(), n.previousSiteCode()));
        return body;
    }

    private static Map<String, Object> slot(LocalDateTime startAt, String siteCode) {
        if (startAt == null) {
            return null;
        }
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("date", startAt.toLocalDate().toString());
        slot.put("startTime", startAt.toLocalTime().toString());
        slot.put("siteCode", siteCode);
        return slot;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
