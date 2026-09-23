package com.citas.api.domain.model.professional;

import com.citas.api.domain.exception.InvalidFieldException;

import java.util.Set;

/**
 * Especialidad ofertada. La duración de la cita la fija la especialidad, no el profesional,
 * y solo admite 30 o 60 minutos porque la agenda se discretiza en slots de 30 (HU-006).
 */
public final class Specialty {

    public static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 60);
    private static final int MAX_NAME_LENGTH = 120;

    private final Long id;
    private final String name;
    private final int durationMinutes;
    private final boolean general;
    private final boolean active;

    private Specialty(Long id, String name, int durationMinutes, boolean general, boolean active) {
        this.id = id;
        this.name = requireName(name);
        this.durationMinutes = requireDuration(durationMinutes);
        this.general = general;
        this.active = active;
    }

    /** Especialidad nueva: siempre activa y no general (la general la siembra la migración). */
    public static Specialty createNew(String name, int durationMinutes) {
        return new Specialty(null, name, durationMinutes, false, true);
    }

    public static Specialty restore(Long id, String name, int durationMinutes, boolean general, boolean active) {
        return new Specialty(id, name, durationMinutes, general, active);
    }

    public Specialty withName(String newName) {
        return new Specialty(id, newName, durationMinutes, general, active);
    }

    public Specialty withDuration(int newDuration) {
        return new Specialty(id, name, newDuration, general, active);
    }

    /**
     * Activa o desactiva. No hay borrado físico: una especialidad referenciada por profesionales
     * o citas se desactiva para que el historial siga siendo legible (HU-006, CA-04).
     */
    public Specialty withActive(boolean newActive) {
        return new Specialty(id, name, durationMinutes, general, newActive);
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("name", "El nombre de la especialidad es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidFieldException("name", "El nombre supera " + MAX_NAME_LENGTH + " caracteres");
        }
        return trimmed;
    }

    private static int requireDuration(int value) {
        if (!ALLOWED_DURATIONS.contains(value)) {
            throw new InvalidFieldException("durationMinutes", "La duración debe ser 30 o 60 minutos");
        }
        return value;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public boolean isGeneral() { return general; }
    public boolean isActive() { return active; }
}
