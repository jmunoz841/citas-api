package com.citas.api.application.service;

import com.citas.api.application.port.in.ManageInsuranceCatalogUseCase;
import com.citas.api.application.port.out.InsuranceCatalogRepositoryPort;
import com.citas.api.domain.exception.DuplicateValueException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.affiliation.InsuranceProvider;
import com.citas.api.domain.model.affiliation.ManagedInsurancePlan;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public class InsuranceCatalogService implements ManageInsuranceCatalogUseCase {
    private final InsuranceCatalogRepositoryPort catalogs;
    public InsuranceCatalogService(InsuranceCatalogRepositoryPort catalogs) { this.catalogs = catalogs; }
    @Override @Transactional(readOnly = true) public List<InsuranceProvider> listProviders(boolean active) { return catalogs.findProviders(active); }
    @Override @Transactional public InsuranceProvider createProvider(String name) {
        if (catalogs.providerNameExists(name, null)) throw DuplicateValueException.epsName();
        return catalogs.saveProvider(new InsuranceProvider(null, name, true));
    }
    @Override @Transactional public InsuranceProvider updateProvider(Long id, String name) {
        InsuranceProvider current = provider(id);
        if (!current.name().equals(name) && catalogs.providerNameExists(name, id)) throw DuplicateValueException.epsName();
        return catalogs.saveProvider(current.withName(name));
    }
    @Override @Transactional public InsuranceProvider setProviderActive(Long id, boolean active) { return catalogs.saveProvider(provider(id).withActive(active)); }
    @Override @Transactional(readOnly = true) public List<ManagedInsurancePlan> listPlans(Long id, boolean active) { provider(id); return catalogs.findPlans(id, active); }
    @Override @Transactional public ManagedInsurancePlan createPlan(Long providerId, String name) {
        provider(providerId); if (catalogs.planNameExists(providerId, name, null)) throw DuplicateValueException.planName();
        return catalogs.savePlan(new ManagedInsurancePlan(null, providerId, name, true));
    }
    @Override @Transactional public ManagedInsurancePlan updatePlan(Long id, String name) {
        ManagedInsurancePlan current = plan(id);
        if (!current.name().equals(name) && catalogs.planNameExists(current.epsId(), name, id)) throw DuplicateValueException.planName();
        return catalogs.savePlan(current.withName(name));
    }
    @Override @Transactional public ManagedInsurancePlan setPlanActive(Long id, boolean active) { return catalogs.savePlan(plan(id).withActive(active)); }
    private InsuranceProvider provider(Long id) { return catalogs.findProviderById(id).orElseThrow(ResourceNotFoundException::eps); }
    private ManagedInsurancePlan plan(Long id) { return catalogs.findPlanById(id).orElseThrow(ResourceNotFoundException::insurancePlan); }
}
