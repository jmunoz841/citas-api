package com.citas.api.domain.model.integration;

import java.time.LocalDateTime;

/** Datos actuales de una cita y su paciente para armar una {@link StatusNotification}. */
public record NotificationContext(Long appointmentId, String status, String patientFirstNames, String patientEmail,
                                  String specialtyName, String professionalName, String siteCode, String siteName,
                                  LocalDateTime startAt, LocalDateTime endAt) {
}
