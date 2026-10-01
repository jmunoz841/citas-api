package com.citas.api.domain.model.affiliation;

/** EPS administrable; las afiliaciones siguen referenciando sus planes. */
public record InsuranceProvider(Long id, String name, boolean active) {
    public InsuranceProvider {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre de la EPS es obligatorio");
    }
    public InsuranceProvider withName(String value) { return new InsuranceProvider(id, value, active); }
    public InsuranceProvider withActive(boolean value) { return new InsuranceProvider(id, name, value); }
}
