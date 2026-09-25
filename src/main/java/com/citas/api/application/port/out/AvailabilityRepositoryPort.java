package com.citas.api.application.port.out;

import com.citas.api.domain.model.agenda.AvailabilityBlock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Bloques de disponibilidad y sus slots (HU-010). Guardar un bloque regenera sus slots.
 */
public interface AvailabilityRepositoryPort {

    AvailabilityBlock save(AvailabilityBlock block);

    Optional<AvailabilityBlock> findById(Long blockId);

    /** Bloques del profesional, filtrados opcionalmente por día y sede. */
    List<AvailabilityBlock> findByProfessional(Long professionalId, LocalDate date, String siteCode);

    /**
     * ¿Hay otro bloque del mismo profesional que se cruce con este intervalo? Se comprueba en
     * cualquier sede: un profesional no puede estar en dos sitios a la vez (CA-03).
     */
    boolean overlaps(Long professionalId, AvailabilityBlock candidate, Long excludedBlockId);

    void deleteById(Long blockId);

    int countSlots(Long blockId);

    /** ¿Algún slot del bloque está reservado o retenido por una cita? (HU-010 CA-05). */
    boolean hasOccupiedSlots(Long blockId);
}
