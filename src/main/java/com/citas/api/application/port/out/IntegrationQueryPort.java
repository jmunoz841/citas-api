package com.citas.api.application.port.out;

import com.citas.api.domain.model.integration.DailySummary;
import com.citas.api.domain.model.integration.NotificationContext;
import com.citas.api.domain.model.integration.ReminderCandidate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Lecturas y registros que necesitan las automatizaciones n8n (HU-023, HU-024, HU-025; V10). */
public interface IntegrationQueryPort {

    /** Citas {@code APPROVED} con inicio en ({@code from}, {@code to}] sin recordatorio para ese inicio. */
    List<ReminderCandidate> findReminderCandidates(LocalDateTime from, LocalDateTime to);

    /**
     * Registra el recordatorio de la cita para el inicio dado. Devuelve {@code false} si ya estaba
     * registrado (idempotente).
     */
    boolean recordReminderSent(Long appointmentId, LocalDateTime startAt, LocalDateTime sentAt);

    DailySummary dailySummary(LocalDate date, LocalDateTime now);

    Optional<NotificationContext> findNotificationContext(Long appointmentId);
}
