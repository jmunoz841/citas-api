package com.citas.api.domain.model.appointment;

import java.time.LocalDateTime;

/** Cita aprobada visible para su profesional (HU-011). */
public record ProfessionalAppointmentView(Long id, String patientName, String specialtyName, String siteCode,
                                          LocalDateTime startAt, LocalDateTime endAt, int durationMinutes) { }
