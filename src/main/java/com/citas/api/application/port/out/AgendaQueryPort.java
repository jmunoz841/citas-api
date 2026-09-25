package com.citas.api.application.port.out;

import com.citas.api.domain.model.agenda.AgendaSlot;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Lecturas de agenda para buscar y reservar (HU-012, HU-013).
 */
public interface AgendaQueryPort {

    /**
     * Parejas profesional–especialidad reservables: profesional activo, especialidad activa y
     * asociación activa (RN-08, HU-009 CA-02). Cada filtro nulo se ignora.
     */
    List<ProfessionalOffer> findOffers(Long specialtyId, Boolean general, Long professionalId);

    /** Slots de los profesionales con inicio en [from, to), filtrados opcionalmente por sede. */
    List<AgendaSlot> findSlots(Collection<Long> professionalIds, LocalDateTime from, LocalDateTime to,
                               String siteCode);

    record ProfessionalOffer(Long professionalId, String professionalName, Long specialtyId, String specialtyName,
                             int durationMinutes, boolean general) {
    }
}
