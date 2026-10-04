package com.citas.api.application.service;

import com.citas.api.application.port.out.RescheduleRepositoryPort;

import java.time.LocalDateTime;

/**
 * Cuando una cita deja de estar {@code APPROVED} (cancelada o cerrada), su reprogramación
 * pendiente ya no tiene sentido: se cierra como {@code CANCELLED} y libera la franja retenida
 * (D-033). Se ejecuta dentro de la transacción de quien cambia la cita.
 */
final class PendingReschedules {

    private PendingReschedules() {
    }

    static void closeFor(RescheduleRepositoryPort reschedules, Long appointmentId, LocalDateTime now) {
        reschedules.findPendingByAppointment(appointmentId).ifPresent(pending -> {
            reschedules.decide(pending.cancel(now));
            reschedules.releaseHeldSlots(pending.id());
        });
    }
}
