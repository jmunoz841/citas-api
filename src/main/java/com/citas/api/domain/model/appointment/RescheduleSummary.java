package com.citas.api.domain.model.appointment;

import java.time.LocalDateTime;

/**
 * Reprogramación pendiente tal como la ve el ADMIN en la bandeja (HU-019, HU-022): quién la pidió,
 * con quién, y la franja actual frente a la solicitada. Es una vista de solo lectura.
 */
public record RescheduleSummary(Long id, Long appointmentId, RescheduleStatus status, String patientName,
                                String professionalName, String specialtyName, int durationMinutes,
                                LocalDateTime originalStartAt, String originalSiteCode,
                                LocalDateTime requestedStartAt, String requestedSiteCode,
                                LocalDateTime requestedAt) {
}
