package com.citas.api.domain.model.agenda;

import java.time.LocalDateTime;

/**
 * Slot de 30 minutos tal como lo ve quien reserva: a qué bloque y sede pertenece y si ya está
 * ocupado por una cita (reservado o retenido).
 */
public record AgendaSlot(Long slotId, Long blockId, Long professionalId, String siteCode, LocalDateTime startAt,
                         boolean taken) {
}
