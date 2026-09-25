package com.citas.api.domain.model.catalog;

/**
 * Sede de atención. El PRD fija dos: HIC e ICV. La dirección se guarda como un único
 * valor atómico (supuesto S-25 del diseño 3FN).
 */
public record Site(String code, String name, String address) {

    public Site {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de la sede es obligatorio");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la sede es obligatorio");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("La dirección de la sede es obligatoria");
        }
    }
}
