package com.citas.api.domain.model.affiliation;

/**
 * Plan de una EPS. El nombre de la EPS viaja junto al plan solo para mostrarlo: lo que se
 * almacena en la afiliación es {@code id}, y la EPS se deriva de él (3FN).
 */
public record InsurancePlan(Long id, String name, Long epsId, String epsName) {

    public InsurancePlan {
        if (id == null) {
            throw new IllegalArgumentException("El identificador del plan es obligatorio");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del plan es obligatorio");
        }
    }
}
