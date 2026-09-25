package com.citas.api.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidades de EPS, planes y afiliación (V3). {@code created_at}/{@code updated_at} los
 * gestiona MySQL y no se mapean.
 */
final class AffiliationJpaEntities {

    private AffiliationJpaEntities() {
    }
}

@Entity
@Table(name = "eps")
class EpsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected EpsJpaEntity() {
    }

    Long getId() {
        return id;
    }

    String getName() {
        return name;
    }

    boolean isActive() {
        return active;
    }
}

@Entity
@Table(name = "eps_plans")
class EpsPlanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eps_id", nullable = false)
    private Long epsId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected EpsPlanJpaEntity() {
    }

    Long getId() {
        return id;
    }

    Long getEpsId() {
        return epsId;
    }

    String getName() {
        return name;
    }

    boolean isActive() {
        return active;
    }
}

@Entity
@Table(name = "user_affiliations")
class UserAffiliationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "regime_code", nullable = false, length = 20)
    private String regimeCode;

    protected UserAffiliationJpaEntity() {
    }

    UserAffiliationJpaEntity(Long userId, Long planId, String regimeCode) {
        this.userId = userId;
        this.planId = planId;
        this.regimeCode = regimeCode;
    }

    Long getId() {
        return id;
    }

    Long getUserId() {
        return userId;
    }

    Long getPlanId() {
        return planId;
    }

    String getRegimeCode() {
        return regimeCode;
    }
}
