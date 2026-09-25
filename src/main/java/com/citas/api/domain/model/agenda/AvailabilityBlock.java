package com.citas.api.domain.model.agenda;

import com.citas.api.domain.exception.InvalidFieldException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Franja de atención publicada por un profesional en una sede y un día.
 *
 * <p>Los límites se alinean a slots de 30 minutos y el bloque no cruza la medianoche, porque
 * la agenda entera se razona en slots de 30 (HU-010).</p>
 */
public final class AvailabilityBlock {

    public static final int SLOT_MINUTES = 30;

    private final Long id;
    private final Long professionalId;
    private final String siteCode;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;

    private AvailabilityBlock(Long id, Long professionalId, String siteCode, LocalDateTime startAt,
                              LocalDateTime endAt) {
        this.id = id;
        this.professionalId = professionalId;
        if (siteCode == null || siteCode.isBlank()) {
            throw new InvalidFieldException("siteCode", "La sede es obligatoria");
        }
        this.siteCode = siteCode.trim().toUpperCase();
        this.startAt = requireAligned("startTime", startAt);
        this.endAt = requireAligned("endTime", endAt);
        if (!this.endAt.isAfter(this.startAt)) {
            throw new InvalidFieldException("endTime", "La hora de fin debe ser posterior a la de inicio");
        }
        if (!this.endAt.toLocalDate().equals(this.startAt.toLocalDate())
                && !this.endAt.equals(this.startAt.toLocalDate().plusDays(1).atStartOfDay())) {
            throw new InvalidFieldException("endTime", "El bloque no puede cruzar la medianoche");
        }
    }

    public static AvailabilityBlock createNew(Long professionalId, String siteCode, LocalDateTime startAt,
                                              LocalDateTime endAt) {
        return new AvailabilityBlock(null, professionalId, siteCode, startAt, endAt);
    }

    public static AvailabilityBlock restore(Long id, Long professionalId, String siteCode, LocalDateTime startAt,
                                            LocalDateTime endAt) {
        return new AvailabilityBlock(id, professionalId, siteCode, startAt, endAt);
    }

    public AvailabilityBlock withId(Long newId) {
        return new AvailabilityBlock(newId, professionalId, siteCode, startAt, endAt);
    }

    public AvailabilityBlock withSchedule(String newSiteCode, LocalDateTime newStart, LocalDateTime newEnd) {
        return new AvailabilityBlock(id, professionalId, newSiteCode, newStart, newEnd);
    }

    /** Inicios de cada slot de 30 minutos: 08:00–12:00 produce 8. */
    public List<LocalDateTime> slotStarts() {
        List<LocalDateTime> starts = new ArrayList<>();
        for (LocalDateTime cursor = startAt; cursor.isBefore(endAt); cursor = cursor.plusMinutes(SLOT_MINUTES)) {
            starts.add(cursor);
        }
        return starts;
    }

    /** Un bloque en el pasado no sirve a nadie: no se puede reservar (RN-06). */
    public boolean startsBefore(LocalDateTime instant) {
        return startAt.isBefore(instant);
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startAt.isBefore(otherEnd) && otherStart.isBefore(endAt);
    }

    private static LocalDateTime requireAligned(String field, LocalDateTime value) {
        if (value == null) {
            throw new InvalidFieldException(field, "El campo es obligatorio");
        }
        if (value.getSecond() != 0 || value.getNano() != 0
                || (value.getMinute() != 0 && value.getMinute() != 30)) {
            throw new InvalidFieldException(field, "La hora debe caer en punto o y media");
        }
        return value;
    }

    public Long getId() { return id; }
    public Long getProfessionalId() { return professionalId; }
    public String getSiteCode() { return siteCode; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
}
