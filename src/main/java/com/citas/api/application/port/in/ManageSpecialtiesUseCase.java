package com.citas.api.application.port.in;

import com.citas.api.domain.model.professional.Specialty;

import java.util.List;

/**
 * Gestión de especialidades por ADMIN (HU-006). No hay borrado: se desactiva.
 */
public interface ManageSpecialtiesUseCase {

    Specialty create(CreateSpecialtyCommand command);

    Specialty update(Long id, UpdateSpecialtyCommand command);

    Specialty setActive(Long id, boolean active);

    List<Specialty> list(boolean onlyActive);

    record CreateSpecialtyCommand(String name, Integer durationMinutes) {
    }

    /** Campos nulos significan "no cambiar". */
    record UpdateSpecialtyCommand(String name, Integer durationMinutes) {
    }
}
