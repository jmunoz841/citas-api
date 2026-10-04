package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.InsuranceCatalogRepositoryPort;
import com.citas.api.domain.model.affiliation.InsuranceProvider;
import com.citas.api.domain.model.affiliation.ManagedInsurancePlan;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
class InsuranceCatalogPersistenceAdapter implements InsuranceCatalogRepositoryPort {
    private final EpsJpaRepository providers; private final EpsPlanJpaRepository plans;
    InsuranceCatalogPersistenceAdapter(EpsJpaRepository providers, EpsPlanJpaRepository plans) { this.providers = providers; this.plans = plans; }
    @Override public List<InsuranceProvider> findProviders(boolean active) { return (active ? providers.findAllByActiveTrueOrderByNameAsc() : providers.findAllByOrderByNameAsc()).stream().map(this::provider).toList(); }
    @Override public Optional<InsuranceProvider> findProviderById(Long id) { return providers.findById(id).map(this::provider); }
    @Override public boolean providerNameExists(String name, Long id) { return id == null ? providers.existsByNameIgnoreCase(name) : providers.existsByNameIgnoreCaseAndIdNot(name, id); }
    @Override public InsuranceProvider saveProvider(InsuranceProvider value) { EpsJpaEntity entity = value.id() == null ? new EpsJpaEntity(value.name(), value.active()) : providers.findById(value.id()).orElseThrow(); entity.setName(value.name()); entity.setActive(value.active()); return provider(providers.save(entity)); }
    @Override public List<ManagedInsurancePlan> findPlans(Long id, boolean active) { return (active ? plans.findAllByEpsIdAndActiveTrueOrderByNameAsc(id) : plans.findAllByEpsIdOrderByNameAsc(id)).stream().map(this::plan).toList(); }
    @Override public Optional<ManagedInsurancePlan> findPlanById(Long id) { return plans.findById(id).map(this::plan); }
    @Override public boolean planNameExists(Long eps, String name, Long id) { return id == null ? plans.existsByEpsIdAndNameIgnoreCase(eps, name) : plans.existsByEpsIdAndNameIgnoreCaseAndIdNot(eps, name, id); }
    @Override public ManagedInsurancePlan savePlan(ManagedInsurancePlan value) { EpsPlanJpaEntity entity = value.id() == null ? new EpsPlanJpaEntity(value.epsId(), value.name(), value.active()) : plans.findById(value.id()).orElseThrow(); entity.setName(value.name()); entity.setActive(value.active()); return plan(plans.save(entity)); }
    private InsuranceProvider provider(EpsJpaEntity e) { return new InsuranceProvider(e.getId(), e.getName(), e.isActive()); }
    private ManagedInsurancePlan plan(EpsPlanJpaEntity p) { return new ManagedInsurancePlan(p.getId(), p.getEpsId(), p.getName(), p.isActive()); }
}
