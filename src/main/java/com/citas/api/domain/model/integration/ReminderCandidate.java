package com.citas.api.domain.model.integration;

import java.time.LocalDateTime;

/**
 * Cita {@code APPROVED} próxima que aún no tiene recordatorio para su horario actual (HU-023). Lleva
 * solo lo necesario para el correo: nombre de pila y email del paciente, y los datos de la cita.
 */
public record ReminderCandidate(Long appointmentId, String patientFirstNames, String patientEmail,
                                String professionalName, String specialtyName, String siteCode, String siteName,
                                String siteAddress, LocalDateTime startAt, LocalDateTime endAt) {
}
