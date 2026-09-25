package com.citas.api.application.service;

import com.citas.api.application.port.in.ResolveAppointmentRequestUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.AppointmentSummary;
import com.citas.api.domain.model.appointment.StatusChange;
import com.citas.api.domain.model.appointment.StatusChange.Source;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aprobación y rechazo de citas especializadas por el ADMIN (HU-015).
 *
 * <p>El dominio comprueba que la cita esté {@code REQUESTED}; el cambio de estado se escribe
 * con la misma condición en la base, así que si dos decisiones llegan a la vez solo una se
 * aplica. Estado, slots e historial cambian en una sola transacción.</p>
 */
public class AppointmentRequestService implements ResolveAppointmentRequestUseCase {

    private final AppointmentRepositoryPort appointments;

    public AppointmentRequestService(AppointmentRepositoryPort appointments) {
        this.appointments = appointments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentSummary> listRequested() {
        return appointments.findByStatus(AppointmentStatus.REQUESTED);
    }

    @Override
    @Transactional
    public Appointment approve(Long adminUserId, Long appointmentId) {
        Appointment approved = require(appointmentId).approve();
        appointments.changeStatus(approved, AppointmentStatus.REQUESTED);
        appointments.recordStatus(new StatusChange(appointmentId, approved.getStatus(), Source.ADMIN, adminUserId,
                null));
        return approved;
    }

    @Override
    @Transactional
    public Appointment reject(Long adminUserId, Long appointmentId, String reason) {
        Appointment rejected = require(appointmentId).reject(reason);
        appointments.changeStatus(rejected, AppointmentStatus.REQUESTED);
        appointments.releaseSlots(appointmentId);
        appointments.recordStatus(new StatusChange(appointmentId, rejected.getStatus(), Source.ADMIN, adminUserId,
                reason.trim()));
        return rejected;
    }

    private Appointment require(Long appointmentId) {
        return appointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("La cita no existe"));
    }
}
