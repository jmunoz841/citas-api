package com.citas.api.domain.model.appointment;

import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;

import java.time.LocalDateTime;

/**
 * Solicitud de reprogramación de una cita (RF-15, HU-018, HU-019).
 *
 * <p>Mientras está {@code PENDING} la cita conserva su franja y la solicitud retiene la nueva
 * (RN-10). La franja original y la solicitada son copias: al aprobar, la cita cambia de horario
 * y la solicitud conserva de dónde venía.</p>
 */
public record RescheduleRequest(Long id, Long appointmentId, LocalDateTime originalStartAt,
                                String originalSiteCode, LocalDateTime requestedStartAt,
                                String requestedSiteCode, RescheduleStatus status, Long decidedByUserId,
                                LocalDateTime decidedAt, String decisionReason) {

    /**
     * Nueva solicitud sobre una cita propia {@code APPROVED} y futura (HU-018 CA-01, CA-02). La
     * nueva franja debe ser futura y distinta de la actual.
     */
    public static RescheduleRequest open(Appointment appointment, LocalDateTime requestedStartAt,
                                         String requestedSiteCode, LocalDateTime now) {
        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw BusinessConflictException.appointmentNotReschedulable();
        }
        if (!appointment.getStartAt().isAfter(now)) {
            throw new InvalidFieldException("appointmentId", "Solo se pueden reprogramar citas futuras");
        }
        if (!requestedStartAt.isAfter(now)) {
            throw new InvalidFieldException("startTime", "No se puede reprogramar a un horario pasado");
        }
        if (requestedStartAt.equals(appointment.getStartAt())) {
            throw new InvalidFieldException("startTime", "El nuevo horario debe ser distinto del actual");
        }
        return new RescheduleRequest(null, appointment.getId(), appointment.getStartAt(),
                appointment.getSiteCode(), requestedStartAt, requestedSiteCode, RescheduleStatus.PENDING, null,
                null, null);
    }

    public RescheduleRequest withId(Long newId) {
        return new RescheduleRequest(newId, appointmentId, originalStartAt, originalSiteCode, requestedStartAt,
                requestedSiteCode, status, decidedByUserId, decidedAt, decisionReason);
    }

    /** El ADMIN aprueba: la cita pasa a la nueva franja (HU-019 CA-01). */
    public RescheduleRequest approve(Long adminUserId, LocalDateTime now) {
        requirePending();
        requireNotExpired(now);
        return decide(RescheduleStatus.APPROVED, adminUserId, now, null);
    }

    /** El ADMIN rechaza con motivo obligatorio: la cita conserva su franja (HU-019 CA-02, CA-03). */
    public RescheduleRequest reject(Long adminUserId, String reason, LocalDateTime now) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidFieldException("reason", "El motivo del rechazo es obligatorio");
        }
        if (reason.trim().length() > Appointment.MAX_REASON_LENGTH) {
            throw new InvalidFieldException("reason", "El motivo supera " + Appointment.MAX_REASON_LENGTH
                    + " caracteres");
        }
        requirePending();
        return decide(RescheduleStatus.REJECTED, adminUserId, now, reason.trim());
    }

    /**
     * La solicitud se cierra sin decisión del ADMIN: la cita se canceló o la franja dejó de ser
     * válida antes de decidir (D-033).
     */
    public RescheduleRequest cancel(LocalDateTime now) {
        requirePending();
        return decide(RescheduleStatus.CANCELLED, null, now, null);
    }

    /**
     * D-033: si llega la hora de la cita original con la solicitud pendiente, ya no se puede
     * decidir. Lo mismo si llega la hora solicitada: aprobarla movería la cita al pasado (RN-06).
     */
    public boolean isExpired(LocalDateTime now) {
        return !originalStartAt.isAfter(now) || !requestedStartAt.isAfter(now);
    }

    private void requirePending() {
        if (status != RescheduleStatus.PENDING) {
            throw BusinessConflictException.rescheduleNotPending();
        }
    }

    private void requireNotExpired(LocalDateTime now) {
        if (isExpired(now)) {
            throw BusinessConflictException.rescheduleExpired();
        }
    }

    private RescheduleRequest decide(RescheduleStatus target, Long adminUserId, LocalDateTime now, String reason) {
        return new RescheduleRequest(id, appointmentId, originalStartAt, originalSiteCode, requestedStartAt,
                requestedSiteCode, target, adminUserId, now, reason);
    }
}
