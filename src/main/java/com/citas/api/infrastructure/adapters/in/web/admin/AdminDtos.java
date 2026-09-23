package com.citas.api.infrastructure.adapters.in.web.admin;

import com.citas.api.application.port.in.ManageProfessionalsUseCase.ProfessionalView;
import com.citas.api.domain.model.professional.ProfessionalAssignments.SpecialtyAssignment;
import com.citas.api.domain.model.professional.Specialty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/**
 * DTOs de administración (HU-006, HU-008, HU-009). La contraseña temporal del profesional solo
 * entra: nunca aparece en una respuesta ni en {@code toString}.
 */
final class AdminDtos {

    private AdminDtos() {
    }

    record SpecialtyRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull Integer durationMinutes) {
    }

    /** Campos nulos significan "no cambiar". */
    record SpecialtyUpdateRequest(@Size(max = 120) String name, Integer durationMinutes) {
    }

    record ActiveRequest(@NotNull Boolean active) {
    }

    record SpecialtyResponse(Long id, String name, int durationMinutes, boolean general, boolean active) {

        static SpecialtyResponse from(Specialty specialty) {
            return new SpecialtyResponse(specialty.getId(), specialty.getName(), specialty.getDurationMinutes(),
                    specialty.isGeneral(), specialty.isActive());
        }
    }

    record SpecialtyAssignmentRequest(@NotNull Long specialtyId, boolean primary) {

        SpecialtyAssignment toDomain() {
            return new SpecialtyAssignment(specialtyId, primary);
        }
    }

    record CreateProfessionalRequest(
            @NotBlank @Size(max = 100) String firstNames,
            @NotBlank @Size(max = 100) String lastNames,
            @NotBlank @Size(max = 10) String documentType,
            @NotBlank @Size(max = 30) String documentNumber,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[0-9+()\\- ]{7,20}$",
                    message = "debe tener entre 7 y 20 dígitos o símbolos + ( ) -") String phone,
            @NotBlank String temporaryPassword,
            @NotBlank @Size(max = 30) String professionalCode,
            @NotBlank @Size(max = 30) String licenseNumber,
            @NotEmpty List<@Valid SpecialtyAssignmentRequest> specialties,
            @NotEmpty Set<String> siteCodes) {

        @Override
        public String toString() {
            return "CreateProfessionalRequest[email=" + email + ", professionalCode=" + professionalCode
                    + ", temporaryPassword=***]";
        }
    }

    record SpecialtiesRequest(@NotEmpty List<@Valid SpecialtyAssignmentRequest> specialties) {
    }

    record SitesRequest(@NotEmpty Set<String> siteCodes) {
    }

    record AssignedSpecialtyResponse(Long specialtyId, boolean primary) {
    }

    record ProfessionalResponse(Long id, String firstNames, String lastNames, String email, String professionalCode,
                                String licenseNumber, boolean active,
                                List<AssignedSpecialtyResponse> specialties, Set<String> siteCodes) {

        static ProfessionalResponse from(ProfessionalView view) {
            return new ProfessionalResponse(
                    view.professional().getUserId(),
                    view.firstNames(),
                    view.lastNames(),
                    view.email(),
                    view.professional().getProfessionalCode(),
                    view.professional().getLicenseNumber(),
                    view.professional().isActive(),
                    view.professional().getAssignments().specialties().stream()
                            .map(a -> new AssignedSpecialtyResponse(a.specialtyId(), a.primary())).toList(),
                    view.professional().getAssignments().siteCodes());
        }
    }

    record ItemsResponse<T>(List<T> items) {
    }
}
