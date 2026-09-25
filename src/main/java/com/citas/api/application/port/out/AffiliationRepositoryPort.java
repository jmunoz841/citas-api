package com.citas.api.application.port.out;

import com.citas.api.domain.model.affiliation.Affiliation;
import com.citas.api.domain.model.affiliation.InsurancePlan;

import java.util.List;
import java.util.Optional;

/**
 * Planes seleccionables y afiliación inicial del usuario (HU-004).
 */
public interface AffiliationRepositoryPort {

    /**
     * Planes que un usuario puede elegir: el plan debe estar activo y su EPS también.
     * Un plan activo de una EPS inactiva no es seleccionable.
     */
    List<InsurancePlan> findSelectablePlans();

    /** El mismo criterio que {@link #findSelectablePlans()}, para validar el envío del cliente. */
    Optional<InsurancePlan> findSelectablePlanById(Long planId);

    boolean regimeExists(String regimeCode);

    Affiliation save(Affiliation affiliation);
}
