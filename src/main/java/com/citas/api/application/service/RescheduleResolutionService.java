package com.citas.api.application.service;

import com.citas.api.application.port.in.ResolveRescheduleUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.application.port.out.RescheduleRepositoryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.InboxFilter;
import com.citas.api.domain.model.appointment.RescheduleRequest;
import com.citas.api.domain.model.appointment.RescheduleSummary;
import com.citas.api.domain.model.appointment.StatusChange;
import com.citas.api.domain.model.appointment.StatusChange.Source;
import com.citas.api.domain.model.integration.StatusNotification.Type;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aprobación y rechazo de reprogramaciones por el ADMIN (HU-019) y su bandeja (HU-022).
 *
 * <p>Aprobar es atómico: se liberan los slots de la cita, se le pasan los retenidos por la
 * solicitud y se mueve su horario en una sola transacción, así que nunca quedan ambas franjas
 * asignadas ni ninguna. Cada escritura va condicionada al estado esperado: si dos decisiones
 * llegan a la vez, solo una se aplica.</p>
 */
public class RescheduleResolutionService implements ResolveRescheduleUseCase {

    private final RescheduleRepositoryPort reschedules;
    private final AppointmentRepositoryPort appointments;
    private final StatusNotifier notifier;
    private final Clock clock;

    public RescheduleResolutionService(RescheduleRepositoryPort reschedules, AppointmentRepositoryPort appointments,
                                       StatusNotifier notifier, Clock clock) {
        this.reschedules = reschedules;
        this.appointments = appointments;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<RescheduleSummary> listPending(InboxFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new InvalidFieldException("to", "La fecha final debe ser igual o posterior a la inicial");
        }
        expireOverdue();
        return reschedules.findPending(filter);
    }

    @Override
    @Transactional
    public RescheduleRequest approve(Long adminUserId, Long requestId) {
        RescheduleRequest current = require(requestId);
        RescheduleRequest approved = current.approve(adminUserId, LocalDateTime.now(clock));
        Appointment appointment = appointments.findById(current.appointmentId())
                .orElseThrow(ResourceNotFoundException::appointment);
        Appointment moved = appointment.reschedule(current.requestedStartAt(), current.requestedSiteCode());

        reschedules.decide(approved);
        appointments.releaseSlots(appointment.getId());
        reschedules.transferHeldSlots(requestId, appointment.getId());
        appointments.updateSchedule(moved);
        appointments.recordStatus(new StatusChange(appointment.getId(), AppointmentStatus.APPROVED, Source.ADMIN,
                adminUserId, "Reprogramada del " + describe(current.originalStartAt(), current.originalSiteCode())
                + " al " + describe(current.requestedStartAt(), current.requestedSiteCode()), requestId));
        notifier.notify(Type.RESCHEDULE_APPROVED, appointment.getId(), null, null, null,
                current.originalStartAt(), current.originalSiteCode());
        return approved;
    }

    @Override
    @Transactional
    public RescheduleRequest reject(Long adminUserId, Long requestId, String reason) {
        RescheduleRequest rejected = require(requestId).reject(adminUserId, reason, LocalDateTime.now(clock));
        reschedules.decide(rejected);
        reschedules.releaseHeldSlots(requestId);
        notifier.notify(Type.RESCHEDULE_REJECTED, rejected.appointmentId(), rejected.decisionReason(),
                rejected.requestedStartAt(), rejected.requestedSiteCode(), null, null);
        return rejected;
    }

    @Override
    @Transactional
    public int expireOverdue() {
        LocalDateTime now = LocalDateTime.now(clock);
        int expired = 0;
        for (RescheduleRequest overdue : reschedules.findPendingExpiredAt(now)) {
            try {
                reschedules.decide(overdue.cancel(now));
            } catch (BusinessConflictException alreadyResolved) {
                // Otra transacción la resolvió entre la lectura y la escritura: no hay nada que cerrar.
                continue;
            }
            reschedules.releaseHeldSlots(overdue.id());
            expired++;
        }
        return expired;
    }

    private RescheduleRequest require(Long requestId) {
        return reschedules.findById(requestId).orElseThrow(ResourceNotFoundException::rescheduleRequest);
    }

    private static String describe(LocalDateTime startAt, String siteCode) {
        return startAt.toLocalDate() + " " + startAt.toLocalTime() + " (" + siteCode + ")";
    }
}
