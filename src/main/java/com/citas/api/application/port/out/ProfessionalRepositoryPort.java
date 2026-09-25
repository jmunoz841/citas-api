package com.citas.api.application.port.out;

import com.citas.api.domain.model.professional.Professional;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepositoryPort {

    /** Guarda el profesional y reemplaza por completo sus especialidades y sedes. */
    Professional save(Professional professional);

    Optional<Professional> findByUserId(Long userId);

    List<Professional> findAll();

    boolean existsByProfessionalCode(String professionalCode);

    boolean existsByLicenseNumber(String licenseNumber);
}
