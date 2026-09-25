package com.citas.api.application.port.in;

import com.citas.api.application.port.in.ManageProfessionalsUseCase.ProfessionalView;
import com.citas.api.domain.model.catalog.Site;
import com.citas.api.domain.model.professional.Specialty;

import java.util.List;

/**
 * Perfil del profesional autenticado (HU-010): sus sedes, su estado y su especialidad principal.
 * "Mi agenda" lo necesita para ofrecer solo las sedes asignadas y avisar si la cuenta está
 * inactiva.
 */
public interface ViewOwnProfessionalProfileUseCase {

    OwnProfile ownProfile(Long professionalId);

    record OwnProfile(ProfessionalView view, Specialty primarySpecialty, List<Site> sites) {
    }
}
