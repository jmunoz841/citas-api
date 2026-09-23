package com.citas.api.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidades de profesionales y sus asignaciones (V4). Las tablas puente usan clave primaria
 * compuesta, mapeada con {@code @IdClass}.
 */
final class ProfessionalJpaEntities {

    private ProfessionalJpaEntities() {
    }
}

@Entity
@Table(name = "professionals")
class ProfessionalJpaEntity {

    /** Es a la vez clave primaria y foránea hacia users: el profesional es un subtipo del usuario. */
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "professional_code", nullable = false, length = 30)
    private String professionalCode;

    @Column(name = "license_number", nullable = false, length = 30)
    private String licenseNumber;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected ProfessionalJpaEntity() {
    }

    ProfessionalJpaEntity(Long userId, String professionalCode, String licenseNumber, boolean active) {
        this.userId = userId;
        this.professionalCode = professionalCode;
        this.licenseNumber = licenseNumber;
        this.active = active;
    }

    Long getUserId() { return userId; }
    String getProfessionalCode() { return professionalCode; }
    String getLicenseNumber() { return licenseNumber; }
    boolean isActive() { return active; }
}

class ProfessionalSpecialtyId implements Serializable {

    private Long professionalId;
    private Long specialtyId;

    protected ProfessionalSpecialtyId() {
    }

    ProfessionalSpecialtyId(Long professionalId, Long specialtyId) {
        this.professionalId = professionalId;
        this.specialtyId = specialtyId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProfessionalSpecialtyId that)) {
            return false;
        }
        return Objects.equals(professionalId, that.professionalId) && Objects.equals(specialtyId, that.specialtyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(professionalId, specialtyId);
    }
}

@Entity
@Table(name = "professional_specialties")
@IdClass(ProfessionalSpecialtyId.class)
class ProfessionalSpecialtyJpaEntity {

    @Id
    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Id
    @Column(name = "specialty_id", nullable = false)
    private Long specialtyId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected ProfessionalSpecialtyJpaEntity() {
    }

    ProfessionalSpecialtyJpaEntity(Long professionalId, Long specialtyId, boolean primary, boolean active) {
        this.professionalId = professionalId;
        this.specialtyId = specialtyId;
        this.primary = primary;
        this.active = active;
    }

    Long getProfessionalId() { return professionalId; }
    Long getSpecialtyId() { return specialtyId; }
    boolean isPrimary() { return primary; }
    boolean isActive() { return active; }
}

class ProfessionalSiteId implements Serializable {

    private Long professionalId;
    private String siteCode;

    protected ProfessionalSiteId() {
    }

    ProfessionalSiteId(Long professionalId, String siteCode) {
        this.professionalId = professionalId;
        this.siteCode = siteCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProfessionalSiteId that)) {
            return false;
        }
        return Objects.equals(professionalId, that.professionalId) && Objects.equals(siteCode, that.siteCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(professionalId, siteCode);
    }
}

@Entity
@Table(name = "professional_sites")
@IdClass(ProfessionalSiteId.class)
class ProfessionalSiteJpaEntity {

    @Id
    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Id
    @Column(name = "site_code", nullable = false, length = 10)
    private String siteCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected ProfessionalSiteJpaEntity() {
    }

    ProfessionalSiteJpaEntity(Long professionalId, String siteCode, boolean active) {
        this.professionalId = professionalId;
        this.siteCode = siteCode;
        this.active = active;
    }

    Long getProfessionalId() { return professionalId; }
    String getSiteCode() { return siteCode; }
    boolean isActive() { return active; }
}
