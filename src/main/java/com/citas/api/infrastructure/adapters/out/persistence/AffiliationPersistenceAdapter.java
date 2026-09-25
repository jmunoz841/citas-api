package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.AffiliationRepositoryPort;
import com.citas.api.domain.model.affiliation.Affiliation;
import com.citas.api.domain.model.affiliation.InsurancePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Planes seleccionables y afiliación inicial (HU-004). "Seleccionable" exige plan activo y
 * EPS activa: un plan vigente de una EPS dada de baja no debe ofrecerse.
 */
@Component
class AffiliationPersistenceAdapter implements AffiliationRepositoryPort {

    private final EpsPlanJpaRepository plans;
    private final UserAffiliationJpaRepository affiliations;
    private final RegimeJpaRepository regimes;

    AffiliationPersistenceAdapter(EpsPlanJpaRepository plans, UserAffiliationJpaRepository affiliations,
                                  RegimeJpaRepository regimes) {
        this.plans = plans;
        this.affiliations = affiliations;
        this.regimes = regimes;
    }

    @Override
    public List<InsurancePlan> findSelectablePlans() {
        return plans.findSelectable().stream().map(AffiliationPersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<InsurancePlan> findSelectablePlanById(Long planId) {
        if (planId == null) {
            return Optional.empty();
        }
        return plans.findSelectableById(planId).map(AffiliationPersistenceAdapter::toDomain);
    }

    @Override
    public boolean regimeExists(String regimeCode) {
        return regimeCode != null && regimes.existsById(regimeCode);
    }

    @Override
    public Affiliation save(Affiliation affiliation) {
        UserAffiliationJpaEntity saved = affiliations.save(new UserAffiliationJpaEntity(
                affiliation.userId(), affiliation.planId(), affiliation.regimeCode()));
        return new Affiliation(saved.getId(), saved.getUserId(), saved.getPlanId(), saved.getRegimeCode());
    }

    private static InsurancePlan toDomain(SelectablePlan row) {
        return new InsurancePlan(row.getId(), row.getName(), row.getEpsId(), row.getEpsName());
    }
}

/** Proyección del plan junto al nombre de su EPS, para no exponer entidades fuera del paquete. */
interface SelectablePlan {

    Long getId();

    String getName();

    Long getEpsId();

    String getEpsName();
}

interface EpsPlanJpaRepository extends JpaRepository<EpsPlanJpaEntity, Long> {

    @Query("""
            SELECT p.id AS id, p.name AS name, e.id AS epsId, e.name AS epsName
            FROM EpsPlanJpaEntity p JOIN EpsJpaEntity e ON e.id = p.epsId
            WHERE p.active = true AND e.active = true
            ORDER BY e.name, p.name
            """)
    List<SelectablePlan> findSelectable();

    @Query("""
            SELECT p.id AS id, p.name AS name, e.id AS epsId, e.name AS epsName
            FROM EpsPlanJpaEntity p JOIN EpsJpaEntity e ON e.id = p.epsId
            WHERE p.id = :planId AND p.active = true AND e.active = true
            """)
    Optional<SelectablePlan> findSelectableById(Long planId);
}

interface UserAffiliationJpaRepository extends JpaRepository<UserAffiliationJpaEntity, Long> {
}
