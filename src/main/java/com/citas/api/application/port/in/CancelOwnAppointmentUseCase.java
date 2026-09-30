package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.Appointment;

/** Cancelacion de una cita propia futura (HU-017). */
public interface CancelOwnAppointmentUseCase {
    Appointment cancel(Long patientUserId, Long appointmentId);
}
