package com.citas.api.application.service;

import com.citas.api.application.port.in.ViewOwnAppointmentsUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.AppointmentView;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Consulta con ownership de las citas del USER (HU-016). */
public class OwnAppointmentsService implements ViewOwnAppointmentsUseCase {

    private final AppointmentRepositoryPort appointments;

    public OwnAppointmentsService(AppointmentRepositoryPort appointments) {
        this.appointments = appointments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentView> list(Long patientUserId, AppointmentStatus status, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidFieldException("to", "La fecha final debe ser igual o posterior a la inicial");
        }
        return appointments.findViewsByPatient(patientUserId, status,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentView get(Long patientUserId, Long appointmentId) {
        return appointments.findViewByIdAndPatient(appointmentId, patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("La cita no existe"));
    }
}
