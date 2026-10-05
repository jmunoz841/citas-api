package com.citas.api.domain.model.integration;

import java.time.LocalDateTime;

/**
 * Evento de cambio de estado que la API envía a n8n (HU-024, WF-002). Lleva lo mínimo para el correo:
 * nombre de pila y email del paciente, datos de la cita y motivo cuando aplica. Nunca tokens ni
 * documentos de identidad.
 *
 * @param requestedStartAt  franja pedida en una reprogramación rechazada; nulo en los demás eventos
 * @param previousStartAt   franja anterior en una reprogramación aprobada; nulo en los demás eventos
 */
public record StatusNotification(String eventId, Type type, LocalDateTime occurredAt, Long appointmentId,
                                 String status, String reason, String patientFirstNames, String patientEmail,
                                 String specialtyName, String professionalName, String siteCode, String siteName,
                                 LocalDateTime startAt, LocalDateTime endAt, LocalDateTime requestedStartAt,
                                 String requestedSiteCode, LocalDateTime previousStartAt, String previousSiteCode) {

    /** Eventos soportados (HU-024 § Alcance). */
    public enum Type {
        SPECIALIZED_APPROVED,
        SPECIALIZED_REJECTED,
        RESCHEDULE_APPROVED,
        RESCHEDULE_REJECTED,
        APPOINTMENT_CANCELLED
    }
}
