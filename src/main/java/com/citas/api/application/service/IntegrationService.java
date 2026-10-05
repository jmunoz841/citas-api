package com.citas.api.application.service;

import com.citas.api.application.port.in.AppointmentRemindersUseCase;
import com.citas.api.application.port.in.DailySummaryUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.application.port.out.IntegrationQueryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.integration.DailySummary;
import com.citas.api.domain.model.integration.ReminderCandidate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lecturas para las automatizaciones n8n (HU-023, HU-025).
 *
 * <p>Los recordatorios se registran por cita <b>y horario</b>: si la cita se reprograma, el nuevo
 * horario vuelve a ser recordable, y repetir el registro no duplica nada (D-041).</p>
 */
public class IntegrationService implements AppointmentRemindersUseCase, DailySummaryUseCase {

    private final IntegrationQueryPort integration;
    private final AppointmentRepositoryPort appointments;
    private final Clock clock;

    public IntegrationService(IntegrationQueryPort integration, AppointmentRepositoryPort appointments, Clock clock) {
        this.integration = integration;
        this.appointments = appointments;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderCandidate> upcoming(Integer hours) {
        int window = hours == null ? DEFAULT_HOURS : hours;
        if (window < 1 || window > MAX_HOURS) {
            throw new InvalidFieldException("hours", "La ventana debe estar entre 1 y " + MAX_HOURS + " horas");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return integration.findReminderCandidates(now, now.plusHours(window));
    }

    @Override
    @Transactional
    public ReminderMark markSent(Long appointmentId) {
        Appointment appointment = appointments.findById(appointmentId)
                .orElseThrow(ResourceNotFoundException::appointment);
        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw BusinessConflictException.reminderNotApplicable();
        }
        boolean inserted = integration.recordReminderSent(appointmentId, appointment.getStartAt(),
                LocalDateTime.now(clock));
        return new ReminderMark(appointmentId, appointment.getStartAt(), !inserted);
    }

    @Override
    @Transactional(readOnly = true)
    public DailySummary summary(LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        return integration.dailySummary(date == null ? now.toLocalDate() : date, now);
    }
}
