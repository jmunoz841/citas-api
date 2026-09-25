package com.citas.api.domain.model.catalog;

/**
 * Entrada de un catálogo fijo identificada por código: roles, tipos de documento y regímenes.
 * Son datos de referencia sembrados por migración y de solo lectura desde la API (HU-005).
 */
public record CatalogEntry(String code, String name) {

    public CatalogEntry {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código del catálogo es obligatorio");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del catálogo es obligatorio");
        }
    }
}
