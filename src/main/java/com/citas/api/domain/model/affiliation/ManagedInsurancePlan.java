package com.citas.api.domain.model.affiliation;

/** Plan administrable de una EPS (HU-007). */
public record ManagedInsurancePlan(Long id, Long epsId, String name, boolean active) {
    public ManagedInsurancePlan {
        if (epsId == null) throw new IllegalArgumentException("La EPS es obligatoria");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre del plan es obligatorio");
    }
    public ManagedInsurancePlan withName(String value) { return new ManagedInsurancePlan(id, epsId, value, active); }
    public ManagedInsurancePlan withActive(boolean value) { return new ManagedInsurancePlan(id, epsId, name, value); }
}
