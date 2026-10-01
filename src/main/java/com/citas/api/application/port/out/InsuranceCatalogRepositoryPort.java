package com.citas.api.application.port.out;

import com.citas.api.domain.model.affiliation.InsuranceProvider;
import com.citas.api.domain.model.affiliation.ManagedInsurancePlan;
import java.util.List;
import java.util.Optional;

public interface InsuranceCatalogRepositoryPort {
    List<InsuranceProvider> findProviders(boolean onlyActive);
    Optional<InsuranceProvider> findProviderById(Long id);
    boolean providerNameExists(String name, Long excludingId);
    InsuranceProvider saveProvider(InsuranceProvider provider);
    List<ManagedInsurancePlan> findPlans(Long providerId, boolean onlyActive);
    Optional<ManagedInsurancePlan> findPlanById(Long id);
    boolean planNameExists(Long providerId, String name, Long excludingId);
    ManagedInsurancePlan savePlan(ManagedInsurancePlan plan);
}
