package com.citas.api.domain.model.professional;

import com.citas.api.domain.exception.InvalidFieldException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Especialidades y sedes de un profesional.
 *
 * <p>Las reglas "al menos una especialidad", "exactamente una primaria" y "al menos una sede"
 * no se pueden expresar como restricción declarativa de MySQL, así que viven aquí. La base sí
 * impide, con un índice funcional, que haya <em>más</em> de una primaria.</p>
 */
public record ProfessionalAssignments(List<SpecialtyAssignment> specialties, Set<String> siteCodes) {

    public ProfessionalAssignments {
        if (specialties == null || specialties.isEmpty()) {
            throw new InvalidFieldException("specialties", "Asigna al menos una especialidad");
        }
        long primaries = specialties.stream().filter(SpecialtyAssignment::primary).count();
        if (primaries != 1) {
            throw new InvalidFieldException("specialties", "Marca exactamente una especialidad primaria");
        }
        long distinct = specialties.stream().map(SpecialtyAssignment::specialtyId).distinct().count();
        if (distinct != specialties.size()) {
            throw new InvalidFieldException("specialties", "Hay especialidades repetidas");
        }
        if (siteCodes == null || siteCodes.isEmpty()) {
            throw new InvalidFieldException("siteCodes", "Asigna al menos una sede");
        }
        specialties = List.copyOf(specialties);
        siteCodes = new LinkedHashSet<>(siteCodes);
    }

    public Long primarySpecialtyId() {
        return specialties.stream().filter(SpecialtyAssignment::primary).findFirst()
                .map(SpecialtyAssignment::specialtyId).orElseThrow();
    }

    public record SpecialtyAssignment(Long specialtyId, boolean primary) {

        public SpecialtyAssignment {
            if (specialtyId == null) {
                throw new InvalidFieldException("specialties", "Falta el identificador de una especialidad");
            }
        }
    }
}
