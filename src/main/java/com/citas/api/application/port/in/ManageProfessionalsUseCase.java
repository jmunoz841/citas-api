package com.citas.api.application.port.in;

import com.citas.api.domain.model.professional.Professional;
import com.citas.api.domain.model.professional.ProfessionalAssignments.SpecialtyAssignment;

import java.util.List;
import java.util.Set;

/**
 * Alta y administración de profesionales por ADMIN (HU-008 y HU-009).
 */
public interface ManageProfessionalsUseCase {

    ProfessionalView create(CreateProfessionalCommand command);

    List<ProfessionalView> list();

    ProfessionalView assignSpecialties(Long professionalId, List<SpecialtyAssignment> specialties);

    ProfessionalView assignSites(Long professionalId, Set<String> siteCodes);

    ProfessionalView setActive(Long professionalId, boolean active);

    /**
     * La contraseña temporal solo viaja en la solicitud: se hashea y nunca se devuelve ni se
     * registra en logs.
     */
    record CreateProfessionalCommand(String firstNames, String lastNames, String documentType,
                                     String documentNumber, String email, String phone, String temporaryPassword,
                                     String professionalCode, String licenseNumber,
                                     List<SpecialtyAssignment> specialties, Set<String> siteCodes) {

        @Override
        public String toString() {
            return "CreateProfessionalCommand[email=" + email + ", code=" + professionalCode
                    + ", temporaryPassword=***]";
        }
    }

    /** Profesional con los datos de su usuario, para las vistas de ADMIN. */
    record ProfessionalView(Professional professional, String firstNames, String lastNames, String email) {
    }
}
