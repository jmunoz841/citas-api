package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.Appointment;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Reserva de una cita por el USER autenticado (HU-013, HU-014). La especialidad decide el
 * flujo: Medicina General queda {@code APPROVED}; cualquier otra, {@code REQUESTED}.
 */
public interface BookAppointmentUseCase {

    Appointment book(Long patientUserId, BookingCommand command);

    /** Fecha y hora de inicio locales de Bogotá, tal como las devolvió la búsqueda. */
    record BookingCommand(Long professionalId, Long specialtyId, String siteCode, LocalDate date,
                          LocalTime startTime) {
    }
}
