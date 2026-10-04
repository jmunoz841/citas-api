package com.citas.api.application.port.out;

import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.appointment.InboxFilter;
import com.citas.api.domain.model.appointment.RescheduleRequest;
import com.citas.api.domain.model.appointment.RescheduleSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Solicitudes de reprogramación y la franja que retienen (V9).
 */
public interface RescheduleRepositoryPort {

    /**
     * Inserta una solicitud {@code PENDING}. Si la cita ya tiene una, el índice
     * {@code uk_rr_one_pending_per_appointment} lo impide y se lanza
     * {@link com.citas.api.domain.exception.BusinessConflictException#reschedulePending()}.
     */
    RescheduleRequest save(RescheduleRequest request);

    /**
     * Retiene los slots a nombre de la solicitud. Si alguno está ocupado o retenido, la clave
     * primaria de {@code slot_reservations} lo impide y se lanza
     * {@link com.citas.api.domain.exception.BusinessConflictException#slotUnavailable()}.
     */
    void holdSlots(RescheduleRequest request, Long professionalId, List<AgendaSlot> slots);

    Optional<RescheduleRequest> findById(Long requestId);

    Optional<RescheduleRequest> findPendingByAppointment(Long appointmentId);

    /**
     * Cambia estado y decisión solo si la solicitud sigue {@code PENDING}. Si otra transacción la
     * resolvió antes, no cambia nada y lanza
     * {@link com.citas.api.domain.exception.BusinessConflictException#rescheduleNotPending()}.
     */
    void decide(RescheduleRequest decided);

    /** Libera la franja retenida por la solicitud (rechazo o cancelación). */
    void releaseHeldSlots(Long requestId);

    /**
     * Pasa a la cita los slots retenidos por la solicitud. Los slots anteriores de la cita deben
     * haberse liberado antes en la misma transacción (HU-019 CA-01).
     */
    void transferHeldSlots(Long requestId, Long appointmentId);

    /** Solicitudes {@code PENDING} filtradas, la franja pedida más próxima primero (HU-022). */
    List<RescheduleSummary> findPending(InboxFilter filter);

    /** Solicitudes {@code PENDING} cuya franja original o pedida ya empezó en {@code now} (D-033). */
    List<RescheduleRequest> findPendingExpiredAt(LocalDateTime now);
}
