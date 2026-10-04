package com.citas.api.domain.model.appointment;

import java.time.LocalDateTime;

/**
 * Última solicitud de reprogramación de una cita, tal como la ve su paciente (HU-018, HU-019):
 * el estado, la franja pedida y el motivo cuando fue rechazada.
 */
public record RescheduleInfo(Long id, RescheduleStatus status, LocalDateTime requestedStartAt,
                             String requestedSiteCode, String decisionReason) {
}
