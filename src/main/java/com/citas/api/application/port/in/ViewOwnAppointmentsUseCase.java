package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.AppointmentView;

import java.time.LocalDate;
import java.util.List;

/**
 * Consulta de las citas propias del USER (HU-016). El paciente sale siempre del access token:
 * nadie ve las citas de otro.
 */
public interface ViewOwnAppointmentsUseCase {

    /** Filtros opcionales; {@code from} y {@code to} son fechas inclusivas. La más próxima primero. */
    List<AppointmentView> list(Long patientUserId, AppointmentStatus status, LocalDate from, LocalDate to);

    /** Una cita ajena se trata como inexistente: no se revela que existe (CA-04). */
    AppointmentView get(Long patientUserId, Long appointmentId);
}
