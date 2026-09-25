package com.citas.api.infrastructure.adapters.in.web.admin;

import com.citas.api.application.port.in.ManageProfessionalsUseCase;
import com.citas.api.application.port.in.ManageProfessionalsUseCase.CreateProfessionalCommand;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.ActiveRequest;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.CreateProfessionalRequest;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.ItemsResponse;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.ProfessionalResponse;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.SitesRequest;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.SpecialtiesRequest;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.SpecialtyAssignmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profesionales y sus asignaciones (HU-008 y HU-009). Exige rol ADMIN por `SecurityConfig`.
 *
 * <p>La respuesta nunca incluye la contraseña temporal ni su hash.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/professionals")
class AdminProfessionalController {

    private final ManageProfessionalsUseCase professionals;

    AdminProfessionalController(ManageProfessionalsUseCase professionals) {
        this.professionals = professionals;
    }

    @GetMapping
    ItemsResponse<ProfessionalResponse> list() {
        return new ItemsResponse<>(professionals.list().stream().map(ProfessionalResponse::from).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProfessionalResponse create(@Valid @RequestBody CreateProfessionalRequest request) {
        return ProfessionalResponse.from(professionals.create(new CreateProfessionalCommand(
                request.firstNames(), request.lastNames(), request.documentType(), request.documentNumber(),
                request.email(), request.phone(), request.temporaryPassword(), request.professionalCode(),
                request.licenseNumber(),
                request.specialties().stream().map(SpecialtyAssignmentRequest::toDomain).toList(),
                request.siteCodes())));
    }

    @PutMapping("/{id}/specialties")
    ProfessionalResponse assignSpecialties(@PathVariable Long id, @Valid @RequestBody SpecialtiesRequest request) {
        return ProfessionalResponse.from(professionals.assignSpecialties(id,
                request.specialties().stream().map(SpecialtyAssignmentRequest::toDomain).toList()));
    }

    @PutMapping("/{id}/sites")
    ProfessionalResponse assignSites(@PathVariable Long id, @Valid @RequestBody SitesRequest request) {
        return ProfessionalResponse.from(professionals.assignSites(id, request.siteCodes()));
    }

    @PatchMapping("/{id}/active")
    ProfessionalResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return ProfessionalResponse.from(professionals.setActive(id, request.active()));
    }
}
