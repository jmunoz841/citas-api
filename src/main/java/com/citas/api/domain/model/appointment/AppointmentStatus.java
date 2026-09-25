package com.citas.api.domain.model.appointment;

/**
 * Estados de cita. Coinciden con el catálogo fijo {@code appointment_statuses} (V2).
 */
public enum AppointmentStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED,
    NO_SHOW
}
