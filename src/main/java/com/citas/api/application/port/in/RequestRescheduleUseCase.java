package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.RescheduleRequest;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud de reprogramación de una cita propia por el USER autenticado (HU-018). Profesional y
 * especialidad son los de la cita; el paciente elige sede, fecha y hora de una franja libre.
 */
public interface RequestRescheduleUseCase {

    RescheduleRequest request(Long patientUserId, Long appointmentId, RescheduleCommand command);

    /** Fecha y hora de inicio locales de Bogotá, tal como las devolvió la búsqueda. */
    record RescheduleCommand(String siteCode, LocalDate date, LocalTime startTime) {
    }
}
