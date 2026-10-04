package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentSummary;
import com.citas.api.domain.model.appointment.InboxFilter;

import java.util.List;

/**
 * Decisión del ADMIN sobre las citas especializadas solicitadas (HU-015). El ADMIN sale del
 * access token y queda como actor en el historial.
 */
public interface ResolveAppointmentRequestUseCase {

    /** Citas {@code REQUESTED} que cumplen los filtros de la bandeja, las más próximas primero (HU-022). */
    List<AppointmentSummary> listRequested(InboxFilter filter);

    Appointment approve(Long adminUserId, Long appointmentId);

    Appointment reject(Long adminUserId, Long appointmentId, String reason);
}
