package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.ProfessionalRepositoryPort;
import com.citas.api.domain.model.professional.Professional;
import com.citas.api.domain.model.professional.ProfessionalAssignments;
import com.citas.api.domain.model.professional.ProfessionalAssignments.SpecialtyAssignment;
import com.citas.api.domain.exception.BusinessConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Profesionales y sus asignaciones (V4).
 *
 * <p>Guardar sincroniza especialidades y sedes por diferencia, dentro de la transacción del caso
 * de uso. No se puede borrar todo y volver a insertar: {@code availability_blocks} y
 * {@code appointments} referencian esas filas con FK RESTRICT, y un profesional con agenda no
 * podría ni siquiera desactivarse (HU-009).</p>
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
        syncSpecialties(id, professional.getAssignments().specialties());
        syncSites(id, professional.getAssignments().siteCodes());
        return professional;
    }

    /**
     * Borra las que sobran, inserta las nuevas y conserva las que siguen. Las filas que se
     * conservan no se tocan porque bloques y citas las referencian con FK RESTRICT.
     */
    private void syncSpecialties(Long id, List<SpecialtyAssignment> wanted) {
        Map<Long, ProfessionalSpecialtyJpaEntity> current = specialties.findByProfessionalId(id).stream()
                .collect(Collectors.toMap(ProfessionalSpecialtyJpaEntity::getSpecialtyId, Function.identity()));
        Map<Long, Boolean> wantedPrimary = wanted.stream()
                .collect(Collectors.toMap(SpecialtyAssignment::specialtyId, SpecialtyAssignment::primary));

        // Primero se retira la primaria anterior: el índice único admite una sola por profesional.
        current.values().forEach(row -> {
            Boolean primary = wantedPrimary.get(row.getSpecialtyId());
            if (primary == null) {
                specialties.delete(row);
            } else if (row.isPrimary() && !primary) {
                row.setPrimary(false);
            }
        });
        flushOrExplain(specialties);

        wanted.forEach(assignment -> {
            ProfessionalSpecialtyJpaEntity row = current.get(assignment.specialtyId());
            if (row == null) {
                specialties.save(new ProfessionalSpecialtyJpaEntity(id, assignment.specialtyId(),
                        assignment.primary(), true));
            } else if (assignment.primary()) {
                row.setPrimary(true);
            }
        });
        specialties.flush();
    }

    private void syncSites(Long id, Set<String> wanted) {
        Set<String> kept = new HashSet<>();
        for (ProfessionalSiteJpaEntity row : sites.findByProfessionalId(id)) {
            if (wanted.contains(row.getSiteCode())) {
                kept.add(row.getSiteCode());
            } else {
                sites.delete(row);
            }
        }
        flushOrExplain(sites);
        wanted.stream().filter(code -> !kept.contains(code))
                .forEach(code -> sites.save(new ProfessionalSiteJpaEntity(id, code, true)));
        sites.flush();
    }

    /**
     * D-027: la FK RESTRICT de bloques o citas impide borrar una asignación en uso. Se traduce a un
     * 409 que dice qué se intentó quitar, en lugar de dejar escapar un 500.
     */
    private static void flushOrExplain(JpaRepository<?, ?> repository) {
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            String detail = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (detail.contains("fk_appt_professional_specialty")) {
                throw BusinessConflictException.specialtyInUse();
            }
            if (detail.contains("fk_blocks_professional_site") || detail.contains("fk_appt_professional_site")) {
                throw BusinessConflictException.siteInUse();
            }
            throw e;
        }
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
}

interface ProfessionalSiteJpaRepository extends JpaRepository<ProfessionalSiteJpaEntity, ProfessionalSiteId> {

    List<ProfessionalSiteJpaEntity> findByProfessionalId(Long professionalId);
}
