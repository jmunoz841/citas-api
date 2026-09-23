package com.citas.api.application.service;

import com.citas.api.application.port.in.ManageProfessionalsUseCase;
import com.citas.api.application.port.out.CatalogRepositoryPort;
import com.citas.api.application.port.out.PasswordHasherPort;
import com.citas.api.application.port.out.ProfessionalRepositoryPort;
import com.citas.api.application.port.out.SpecialtyRepositoryPort;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.domain.exception.DocumentAlreadyRegisteredException;
import com.citas.api.domain.exception.DuplicateValueException;
import com.citas.api.domain.exception.EmailAlreadyRegisteredException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.catalog.Site;
import com.citas.api.domain.model.professional.Professional;
import com.citas.api.domain.model.professional.ProfessionalAssignments;
import com.citas.api.domain.model.professional.ProfessionalAssignments.SpecialtyAssignment;
import com.citas.api.domain.model.professional.Specialty;
import com.citas.api.domain.model.user.DocumentType;
import com.citas.api.domain.model.user.Email;
import com.citas.api.domain.model.user.IdentityDocument;
import com.citas.api.domain.model.user.PasswordPolicy;
import com.citas.api.domain.model.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alta y administración de profesionales (HU-008 y HU-009).
 *
 * <p>Crear un profesional crea también su cuenta: comparten identidad, así que las dos
 * escrituras van en la misma transacción.</p>
 */
public class ProfessionalService implements ManageProfessionalsUseCase {

    private final UserRepositoryPort users;
    private final ProfessionalRepositoryPort professionals;
    private final SpecialtyRepositoryPort specialties;
    private final CatalogRepositoryPort catalogs;
    private final PasswordHasherPort passwordHasher;

    public ProfessionalService(UserRepositoryPort users, ProfessionalRepositoryPort professionals,
                               SpecialtyRepositoryPort specialties, CatalogRepositoryPort catalogs,
                               PasswordHasherPort passwordHasher) {
        this.users = users;
        this.professionals = professionals;
        this.specialties = specialties;
        this.catalogs = catalogs;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public ProfessionalView create(CreateProfessionalCommand command) {
        PasswordPolicy.validate(command.temporaryPassword());
        Email email = new Email(command.email());
        IdentityDocument document = new IdentityDocument(DocumentType.fromCode(command.documentType()),
                command.documentNumber());
        // Construir las asignaciones valida las cotas mínimas y la primaria única.
        ProfessionalAssignments assignments = new ProfessionalAssignments(command.specialties(), command.siteCodes());
        validateAssignments(assignments);

        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        if (users.existsByDocument(document)) {
            throw new DocumentAlreadyRegisteredException();
        }
        Professional professional = Professional.createNew(command.professionalCode(), command.licenseNumber(),
                assignments);
        requireAvailableCodes(professional);

        User user = users.save(User.createProfessional(command.firstNames(), command.lastNames(), document, email,
                command.phone(), passwordHasher.hash(command.temporaryPassword())));
        Professional saved = professionals.save(professional.withUserId(user.getId()));
        return toView(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalView> list() {
        return professionals.findAll().stream().map(this::toViewLoadingUser).toList();
    }

    @Override
    @Transactional
    public ProfessionalView assignSpecialties(Long professionalId, List<SpecialtyAssignment> specialtyAssignments) {
        Professional current = require(professionalId);
        ProfessionalAssignments assignments = new ProfessionalAssignments(specialtyAssignments,
                current.getAssignments().siteCodes());
        validateAssignments(assignments);
        return toViewLoadingUser(professionals.save(current.withAssignments(assignments)));
    }

    @Override
    @Transactional
    public ProfessionalView assignSites(Long professionalId, Set<String> siteCodes) {
        Professional current = require(professionalId);
        ProfessionalAssignments assignments = new ProfessionalAssignments(current.getAssignments().specialties(),
                siteCodes);
        validateAssignments(assignments);
        return toViewLoadingUser(professionals.save(current.withAssignments(assignments)));
    }

    /** Desactivar conserva datos y asignaciones; solo excluye al profesional de la oferta (HU-009). */
    @Override
    @Transactional
    public ProfessionalView setActive(Long professionalId, boolean active) {
        Professional current = require(professionalId);
        return toViewLoadingUser(professionals.save(current.withActive(active)));
    }

    private Professional require(Long professionalId) {
        return professionals.findByUserId(professionalId).orElseThrow(ResourceNotFoundException::professional);
    }

    private void requireAvailableCodes(Professional professional) {
        if (professionals.existsByProfessionalCode(professional.getProfessionalCode())) {
            throw DuplicateValueException.professionalCode();
        }
        if (professionals.existsByLicenseNumber(professional.getLicenseNumber())) {
            throw DuplicateValueException.licenseNumber();
        }
    }

    /** Las especialidades deben existir y estar activas; las sedes deben existir en el catálogo. */
    private void validateAssignments(ProfessionalAssignments assignments) {
        for (SpecialtyAssignment assignment : assignments.specialties()) {
            Specialty specialty = specialties.findById(assignment.specialtyId())
                    .orElseThrow(() -> new InvalidFieldException("specialties",
                            "La especialidad " + assignment.specialtyId() + " no existe"));
            if (!specialty.isActive()) {
                throw new InvalidFieldException("specialties",
                        "La especialidad " + specialty.getName() + " está inactiva");
            }
        }
        Set<String> known = catalogs.findSites().stream().map(Site::code).collect(Collectors.toSet());
        for (String code : assignments.siteCodes()) {
            if (!known.contains(code)) {
                throw new InvalidFieldException("siteCodes", "La sede " + code + " no existe");
            }
        }
    }

    private ProfessionalView toViewLoadingUser(Professional professional) {
        User user = users.findById(professional.getUserId()).orElseThrow(ResourceNotFoundException::professional);
        return toView(professional, user);
    }

    private ProfessionalView toView(Professional professional, User user) {
        return new ProfessionalView(professional, user.getFirstNames(), user.getLastNames(), user.getEmail().value());
    }
}
