package com.citas.api.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Búsqueda de horarios que un USER puede reservar de verdad (HU-012).
 */
public interface SearchAvailabilityUseCase {

    List<AvailableSlot> search(SearchQuery query);

    enum AppointmentType {
        GENERAL,
        SPECIALIZED
    }

    /** Solo la fecha es obligatoria; cada filtro presente restringe el resultado (CA-03). */
    record SearchQuery(LocalDate date, String siteCode, AppointmentType type, Long specialtyId,
                       Long professionalId) {
    }

    /** Un horario reservable: inicio y fin ya calculados según la duración de la especialidad. */
    record AvailableSlot(Long professionalId, String professionalName, Long specialtyId, String specialtyName,
                         AppointmentType type, int durationMinutes, String siteCode, LocalDateTime startAt,
                         LocalDateTime endAt) {
    }
}
