package com.citas.api.application.port.in;

import com.citas.api.domain.model.integration.ReminderCandidate;

import java.time.LocalDateTime;
import java.util.List;

/** Recordatorios de citas próximas para WF-001 (HU-023, D-041). Solo la credencial de servicio de n8n. */
public interface AppointmentRemindersUseCase {

    int DEFAULT_HOURS = 24;
    int MAX_HOURS = 72;

    /** Citas {@code APPROVED} que empiezan en las próximas {@code hours} horas sin recordatorio todavía. */
    List<ReminderCandidate> upcoming(Integer hours);

    /** Registra el envío para el horario actual de la cita; repetirlo no duplica nada. */
    ReminderMark markSent(Long appointmentId);

    record ReminderMark(Long appointmentId, LocalDateTime startAt, boolean alreadySent) {
    }
}
