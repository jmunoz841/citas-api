package com.citas.api.domain.exception;

/**
 * La operación choca con el estado actual de la agenda (409): el horario ya lo ocupa otra cita,
 * o un bloque tiene citas y no se puede tocar.
 */
public class BusinessConflictException extends DomainException {

    public BusinessConflictException(String code, String message) {
        super(code, message);
    }

    /** HU-013 CA-02 y CA-03: otra cita ocupó el horario antes de confirmar. */
    public static BusinessConflictException slotUnavailable() {
        return new BusinessConflictException("SLOT_UNAVAILABLE", "El horario ya no está disponible");
    }

    /** HU-015 CA-04: solo una cita REQUESTED se aprueba o se rechaza, y solo una vez. */
    public static BusinessConflictException invalidStatusTransition() {
        return new BusinessConflictException("INVALID_STATUS_TRANSITION",
                "La cita ya no está pendiente de aprobación");
    }

    /** D-027: no se quita a un profesional una especialidad con citas. */
    public static BusinessConflictException specialtyInUse() {
        return new BusinessConflictException("ASSIGNMENT_IN_USE",
                "No se puede quitar la especialidad: el profesional tiene citas en ella");
    }

    /** D-027: no se quita a un profesional una sede con bloques de disponibilidad o citas. */
    public static BusinessConflictException siteInUse() {
        return new BusinessConflictException("ASSIGNMENT_IN_USE",
                "No se puede quitar la sede: el profesional tiene bloques de disponibilidad o citas en ella");
    }

    /** HU-010 CA-05: un bloque con slots reservados o retenidos no se edita ni se elimina. */
    public static BusinessConflictException blockHasAppointments() {
        return new BusinessConflictException("BLOCK_HAS_APPOINTMENTS",
                "El bloque tiene citas reservadas o solicitadas y no se puede modificar");
    }

    /** HU-018 CA-02: solo una cita APPROVED se reprograma. */
    public static BusinessConflictException appointmentNotReschedulable() {
        return new BusinessConflictException("APPOINTMENT_NOT_RESCHEDULABLE",
                "Solo se pueden reprogramar citas aprobadas");
    }

    /** HU-018 CA-04 (D-033): la cita ya tiene una reprogramación pendiente. */
    public static BusinessConflictException reschedulePending() {
        return new BusinessConflictException("RESCHEDULE_ALREADY_PENDING",
                "La cita ya tiene una reprogramación pendiente");
    }

    /** HU-019 CA-04: solo una reprogramación PENDING se aprueba o se rechaza, y solo una vez. */
    public static BusinessConflictException rescheduleNotPending() {
        return new BusinessConflictException("RESCHEDULE_NOT_PENDING",
                "La reprogramación ya no está pendiente");
    }

    /** D-033: llegó la hora de la cita original o de la franja pedida antes de decidir. */
    public static BusinessConflictException rescheduleExpired() {
        return new BusinessConflictException("RESCHEDULE_EXPIRED",
                "La reprogramación venció: llegó la hora de la cita o del nuevo horario");
    }
}
