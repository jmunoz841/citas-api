package com.citas.api.application.port.in;

import com.citas.api.domain.model.affiliation.InsuranceProvider;
import com.citas.api.domain.model.affiliation.ManagedInsurancePlan;
import java.util.List;

public interface ManageInsuranceCatalogUseCase {
    List<InsuranceProvider> listProviders(boolean onlyActive);
    InsuranceProvider createProvider(String name);
    InsuranceProvider updateProvider(Long id, String name);
    InsuranceProvider setProviderActive(Long id, boolean active);
    List<ManagedInsurancePlan> listPlans(Long providerId, boolean onlyActive);
    ManagedInsurancePlan createPlan(Long providerId, String name);
    ManagedInsurancePlan updatePlan(Long id, String name);
    ManagedInsurancePlan setPlanActive(Long id, boolean active);
}
