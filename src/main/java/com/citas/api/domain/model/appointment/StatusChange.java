package com.citas.api.domain.model.appointment;

import com.citas.api.domain.exception.InvalidFieldException;

/**
 * Registro del historial de estados de una cita (RF-19). El historial solo crece: nunca se
 * edita ni se borra.
 */
public record StatusChange(Long appointmentId, AppointmentStatus status, Source source, Long actorUserId,
                           String reason) {

    /** Quién provocó el cambio. {@code SYSTEM} es el único que puede no tener actor. */
    public enum Source {
        SYSTEM,
        USER,
        ADMIN
    }

    public StatusChange {
        if (source != Source.SYSTEM && actorUserId == null) {
            throw new IllegalArgumentException("Un cambio de estado de USER o ADMIN exige actor");
        }
        if (status == AppointmentStatus.REJECTED && (reason == null || reason.isBlank())) {
            throw new InvalidFieldException("reason", "El motivo del rechazo es obligatorio");
        }
    }

    public static StatusChange initial(Appointment appointment) {
        return new StatusChange(appointment.getId(), appointment.getStatus(), Source.USER,
                appointment.getPatientUserId(), null);
    }
}
