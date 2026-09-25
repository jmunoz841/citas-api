package com.citas.api.application.service;

import com.citas.api.application.port.in.ManageSpecialtiesUseCase;
import com.citas.api.application.port.out.SpecialtyRepositoryPort;
import com.citas.api.domain.exception.DuplicateValueException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.professional.Specialty;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Especialidades (HU-006). Solo ADMIN llega aquí: la autorización se aplica en el adaptador web.
 */
public class SpecialtyService implements ManageSpecialtiesUseCase {

    private final SpecialtyRepositoryPort specialties;

    public SpecialtyService(SpecialtyRepositoryPort specialties) {
        this.specialties = specialties;
    }

    @Override
    @Transactional
    public Specialty create(CreateSpecialtyCommand command) {
        Specialty specialty = Specialty.createNew(command.name(),
                command.durationMinutes() == null ? 0 : command.durationMinutes());
        if (specialties.existsByNameIgnoringId(specialty.getName(), null)) {
            throw DuplicateValueException.specialtyName();
        }
        return specialties.save(specialty);
    }

    @Override
    @Transactional
    public Specialty update(Long id, UpdateSpecialtyCommand command) {
        Specialty current = specialties.findById(id).orElseThrow(ResourceNotFoundException::specialty);

        Specialty updated = current;
        if (command.name() != null) {
            updated = updated.withName(command.name());
        }
        if (command.durationMinutes() != null) {
            updated = updated.withDuration(command.durationMinutes());
        }
        if (!updated.getName().equals(current.getName())
                && specialties.existsByNameIgnoringId(updated.getName(), id)) {
            throw DuplicateValueException.specialtyName();
        }
        return specialties.save(updated);
    }

    /** Baja lógica: no hay borrado físico, porque profesionales y citas la referencian (CA-04). */
    @Override
    @Transactional
    public Specialty setActive(Long id, boolean active) {
        Specialty current = specialties.findById(id).orElseThrow(ResourceNotFoundException::specialty);
        return specialties.save(current.withActive(active));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Specialty> list(boolean onlyActive) {
        return specialties.findAll(onlyActive);
    }
}
