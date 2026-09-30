package com.citas.api.application.service;

import com.citas.api.application.port.in.CancelOwnAppointmentUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.StatusChange;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Transicion, liberacion de slots e historial de una cancelacion de USER. */
public class AppointmentCancellationService implements CancelOwnAppointmentUseCase {
    private final AppointmentRepositoryPort appointments;
    private final Clock clock;

    public AppointmentCancellationService(AppointmentRepositoryPort appointments, Clock clock) {
        this.appointments = appointments;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Appointment cancel(Long patientUserId, Long appointmentId) {
        Appointment current = appointments.findById(appointmentId)
                .filter(appointment -> appointment.getPatientUserId().equals(patientUserId))
                .orElseThrow(() -> new ResourceNotFoundException("La cita no existe"));
        if (!current.getStartAt().isAfter(LocalDateTime.now(clock))) {
            throw new InvalidFieldException("appointmentId", "Solo se pueden cancelar citas futuras");
        }
        Appointment cancelled = current.cancel();
        appointments.changeStatus(cancelled, current.getStatus());
        appointments.releaseSlots(appointmentId);
        appointments.recordStatus(new StatusChange(appointmentId, AppointmentStatus.CANCELLED,
                StatusChange.Source.USER, patientUserId, null));
        return cancelled;
    }
}
