package com.citas.api.application.port.out;

import com.citas.api.domain.model.professional.Specialty;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepositoryPort {

    Specialty save(Specialty specialty);

    Optional<Specialty> findById(Long id);

    List<Specialty> findAll(boolean onlyActive);

    /** Unicidad de nombre, ignorando una especialidad concreta al editarla. */
    boolean existsByNameIgnoringId(String name, Long ignoredId);
}
