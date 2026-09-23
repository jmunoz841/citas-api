package com.citas.api.application.port.in;

import com.citas.api.domain.model.agenda.AvailabilityBlock;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Bloques de disponibilidad del profesional autenticado (HU-010). Todas las operaciones
 * reciben el identificador del profesional: nadie toca los bloques de otro (CA-06).
 */
public interface ManageAvailabilityUseCase {

    AvailabilityBlock create(Long professionalId, BlockCommand command);

    AvailabilityBlock update(Long professionalId, Long blockId, BlockCommand command);

    void delete(Long professionalId, Long blockId);

    List<AvailabilityBlock> listOwn(Long professionalId, LocalDate date, String siteCode);

    /** Fecha y horas locales de Bogotá; las horas deben caer en punto o y media. */
    record BlockCommand(LocalDate date, LocalTime startTime, LocalTime endTime, String siteCode) {
    }
}
