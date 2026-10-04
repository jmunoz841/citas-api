package com.citas.api.application.service;

import com.citas.api.application.port.in.CancelOwnAppointmentUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.application.port.out.RescheduleRepositoryPort;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.StatusChange;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Transicion, liberacion de slots e historial de una cancelacion de USER. Si la cita tenia una
 * reprogramacion pendiente, tambien se cancela y libera su franja retenida (D-033).
 */
public class AppointmentCancellationService implements CancelOwnAppointmentUseCase {
    private final AppointmentRepositoryPort appointments;
    private final RescheduleRepositoryPort reschedules;
    private final Clock clock;

    public AppointmentCancellationService(AppointmentRepositoryPort appointments,
                                          RescheduleRepositoryPort reschedules, Clock clock) {
        this.appointments = appointments;
        this.reschedules = reschedules;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Appointment cancel(Long patientUserId, Long appointmentId) {
        Appointment current = appointments.findById(appointmentId)
                .filter(appointment -> appointment.getPatientUserId().equals(patientUserId))
                .orElseThrow(() -> new ResourceNotFoundException("La cita no existe"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!current.getStartAt().isAfter(now)) {
            throw new InvalidFieldException("appointmentId", "Solo se pueden cancelar citas futuras");
        }
        Appointment cancelled = current.cancel();
        // Primero la reprogramación y luego la cita: el mismo orden de bloqueo que al aprobarla,
        // para que una cancelación y una aprobación simultáneas no se interbloqueen.
        PendingReschedules.closeFor(reschedules, appointmentId, now);
        appointments.changeStatus(cancelled, current.getStatus());
        appointments.releaseSlots(appointmentId);
        appointments.recordStatus(new StatusChange(appointmentId, AppointmentStatus.CANCELLED,
                StatusChange.Source.USER, patientUserId, null));
        return cancelled;
    }
}
