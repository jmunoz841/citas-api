package com.citas.api.domain.model.appointment;

import java.time.LocalDateTime;

/**
 * Cita tal como la ve su paciente (HU-016): con los nombres del profesional, la especialidad y
 * la sede, y el motivo del rechazo cuando existe. Es una vista de solo lectura.
 */
public record AppointmentView(Long id, AppointmentStatus status, String professionalName, String specialtyName,
                              String siteCode, String siteName, LocalDateTime startAt, LocalDateTime endAt,
                              int durationMinutes, String rejectionReason) {
}
