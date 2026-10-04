package com.citas.api.domain.model.appointment;

import java.time.LocalDateTime;

/**
 * Cita tal como la ve su paciente (HU-016): con los nombres del profesional, la especialidad y
 * la sede, y el motivo del rechazo cuando existe. {@code reschedule} es la última solicitud de
 * reprogramación, o nulo si nunca se pidió (HU-018). Es una vista de solo lectura.
 */
public record AppointmentView(Long id, AppointmentStatus status, String professionalName, String specialtyName,
                              String siteCode, String siteName, LocalDateTime startAt, LocalDateTime endAt,
                              int durationMinutes, String rejectionReason, Long professionalId, Long specialtyId,
                              RescheduleInfo reschedule) {
}
