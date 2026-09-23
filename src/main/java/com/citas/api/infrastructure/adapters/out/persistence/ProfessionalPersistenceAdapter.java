package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.ProfessionalRepositoryPort;
import com.citas.api.domain.model.professional.Professional;
import com.citas.api.domain.model.professional.ProfessionalAssignments;
import com.citas.api.domain.model.professional.ProfessionalAssignments.SpecialtyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Profesionales y sus asignaciones (V4).
 *
 * <p>Guardar reemplaza por completo especialidades y sedes: se borran las filas actuales y se
 * insertan las nuevas dentro de la misma transacción. Es lo más simple de razonar y evita
 * estados intermedios en los que un profesional quedaría sin primaria.</p>
 */
@Component
class ProfessionalPersistenceAdapter implements ProfessionalRepositoryPort {

    private final ProfessionalJpaRepository professionals;
    private final ProfessionalSpecialtyJpaRepository specialties;
    private final ProfessionalSiteJpaRepository sites;

    ProfessionalPersistenceAdapter(ProfessionalJpaRepository professionals,
                                   ProfessionalSpecialtyJpaRepository specialties,
                                   ProfessionalSiteJpaRepository sites) {
        this.professionals = professionals;
        this.specialties = specialties;
        this.sites = sites;
    }

    @Override
    public Professional save(Professional professional) {
        Long id = professional.getUserId();
        professionals.save(new ProfessionalJpaEntity(id, professional.getProfessionalCode(),
                professional.getLicenseNumber(), professional.isActive()));

        specialties.deleteByProfessionalId(id);
        sites.deleteByProfessionalId(id);
        // flush implícito al leer después; el borrado debe preceder a la inserción por el
        // índice único de "una sola primaria".
        specialties.flush();
        sites.flush();

        for (SpecialtyAssignment assignment : professional.getAssignments().specialties()) {
            specialties.save(new ProfessionalSpecialtyJpaEntity(id, assignment.specialtyId(),
                    assignment.primary(), true));
        }
        for (String siteCode : professional.getAssignments().siteCodes()) {
            sites.save(new ProfessionalSiteJpaEntity(id, siteCode, true));
        }
        return professional;
    }

    @Override
    public Optional<Professional> findByUserId(Long userId) {
        return userId == null ? Optional.empty() : professionals.findById(userId).map(this::toDomain);
    }

    @Override
    public List<Professional> findAll() {
        return professionals.findAllByOrderByProfessionalCodeAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByProfessionalCode(String professionalCode) {
        return professionals.existsByProfessionalCode(professionalCode);
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        return professionals.existsByLicenseNumber(licenseNumber);
    }

    private Professional toDomain(ProfessionalJpaEntity entity) {
        Long id = entity.getUserId();
        List<SpecialtyAssignment> assignments = specialties.findByProfessionalId(id).stream()
                .map(row -> new SpecialtyAssignment(row.getSpecialtyId(), row.isPrimary()))
                .toList();
        Set<String> siteCodes = new LinkedHashSet<>(sites.findByProfessionalId(id).stream()
                .map(ProfessionalSiteJpaEntity::getSiteCode).toList());
        return Professional.restore(id, entity.getProfessionalCode(), entity.getLicenseNumber(), entity.isActive(),
                new ProfessionalAssignments(assignments, siteCodes));
    }
}

interface ProfessionalJpaRepository extends JpaRepository<ProfessionalJpaEntity, Long> {

    List<ProfessionalJpaEntity> findAllByOrderByProfessionalCodeAsc();

    boolean existsByProfessionalCode(String professionalCode);

    boolean existsByLicenseNumber(String licenseNumber);
}

interface ProfessionalSpecialtyJpaRepository extends JpaRepository<ProfessionalSpecialtyJpaEntity,
        ProfessionalSpecialtyId> {

    List<ProfessionalSpecialtyJpaEntity> findByProfessionalId(Long professionalId);

    void deleteByProfessionalId(Long professionalId);
}

interface ProfessionalSiteJpaRepository extends JpaRepository<ProfessionalSiteJpaEntity, ProfessionalSiteId> {

    List<ProfessionalSiteJpaEntity> findByProfessionalId(Long professionalId);

    void deleteByProfessionalId(Long professionalId);
}
