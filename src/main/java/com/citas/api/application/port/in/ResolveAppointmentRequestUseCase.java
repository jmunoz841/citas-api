package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentSummary;

import java.util.List;

/**
 * Decisión del ADMIN sobre las citas especializadas solicitadas (HU-015). El ADMIN sale del
 * access token y queda como actor en el historial.
 */
public interface ResolveAppointmentRequestUseCase {

    /** Citas {@code REQUESTED}, las más próximas primero. */
    List<AppointmentSummary> listRequested();

    Appointment approve(Long adminUserId, Long appointmentId);

    Appointment reject(Long adminUserId, Long appointmentId, String reason);
}
