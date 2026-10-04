package com.citas.api.domain.model.appointment;

/**
 * Estados de una solicitud de reprogramación. Coinciden con el catálogo fijo
 * {@code reschedule_statuses} (V2).
 */
public enum RescheduleStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
