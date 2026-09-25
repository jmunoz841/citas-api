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

    /** HU-010 CA-05: un bloque con slots reservados o retenidos no se edita ni se elimina. */
    public static BusinessConflictException blockHasAppointments() {
        return new BusinessConflictException("BLOCK_HAS_APPOINTMENTS",
                "El bloque tiene citas reservadas o solicitadas y no se puede modificar");
    }
}
