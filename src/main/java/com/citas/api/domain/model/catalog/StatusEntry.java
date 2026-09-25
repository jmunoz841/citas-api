package com.citas.api.domain.model.catalog;

/**
 * Estado de un flujo (cita o reprogramación). {@code terminal} marca los estados que ya no
 * admiten transición, lo que permite al cliente decidir qué acciones ofrecer.
 */
public record StatusEntry(String code, String name, boolean terminal) {

    public StatusEntry {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código del estado es obligatorio");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del estado es obligatorio");
        }
    }
}
