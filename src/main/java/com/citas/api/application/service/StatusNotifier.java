package com.citas.api.application.service;

import com.citas.api.application.port.out.IntegrationQueryPort;
import com.citas.api.application.port.out.StatusNotificationPort;
import com.citas.api.domain.model.integration.StatusNotification;
import com.citas.api.domain.model.integration.StatusNotification.Type;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Arma y publica el evento de cambio de estado para WF-002 (HU-024). Se llama dentro de la
 * transacción que cambió la cita, así que lee su estado ya actualizado; el adaptador lo entrega
 * después del commit y un fallo nunca revierte la transición.
 */
public class StatusNotifier {

    private final IntegrationQueryPort integration;
    private final StatusNotificationPort port;
    private final Clock clock;

    public StatusNotifier(IntegrationQueryPort integration, StatusNotificationPort port, Clock clock) {
        this.integration = integration;
        this.port = port;
        this.clock = clock;
    }

    public void notify(Type type, Long appointmentId, String reason) {
        notify(type, appointmentId, reason, null, null, null, null);
    }

    public void notify(Type type, Long appointmentId, String reason, LocalDateTime requestedStartAt,
                       String requestedSiteCode, LocalDateTime previousStartAt, String previousSiteCode) {
        integration.findNotificationContext(appointmentId).ifPresent(context -> port.publish(new StatusNotification(
                UUID.randomUUID().toString(), type, LocalDateTime.now(clock), context.appointmentId(),
                context.status(), reason, context.patientFirstNames(), context.patientEmail(),
                context.specialtyName(), context.professionalName(), context.siteCode(), context.siteName(),
                context.startAt(), context.endAt(), requestedStartAt, requestedSiteCode, previousStartAt,
                previousSiteCode)));
    }
}
