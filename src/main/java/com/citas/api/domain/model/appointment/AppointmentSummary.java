package com.citas.api.domain.model.appointment;

import java.time.LocalDateTime;

/**
 * Cita con los nombres que necesita una bandeja de revisión: quién la pidió, con quién, de qué
 * especialidad y cuándo. Es una vista de solo lectura (HU-015).
 */
public record AppointmentSummary(Long id, AppointmentStatus status, String patientName, String professionalName,
                                 String specialtyName, String siteCode, LocalDateTime startAt,
                                 LocalDateTime endAt, int durationMinutes) {
}
