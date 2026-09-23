package com.citas.api.infrastructure.adapters.in.web.admin;

import com.citas.api.application.port.in.ManageSpecialtiesUseCase;
import com.citas.api.application.port.in.ManageSpecialtiesUseCase.CreateSpecialtyCommand;
import com.citas.api.application.port.in.ManageSpecialtiesUseCase.UpdateSpecialtyCommand;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.ActiveRequest;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.ItemsResponse;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.SpecialtyRequest;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.SpecialtyResponse;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.SpecialtyUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Especialidades (HU-006). Todo bajo `/api/v1/admin/**` exige rol ADMIN; lo aplica
 * {@code SecurityConfig}, así que aquí no hay comprobaciones de rol.
 *
 * <p>No hay DELETE a propósito: una especialidad referenciada se desactiva (CA-04).</p>
 */
@RestController
@RequestMapping("/api/v1/admin/specialties")
class AdminSpecialtyController {

    private final ManageSpecialtiesUseCase specialties;

    AdminSpecialtyController(ManageSpecialtiesUseCase specialties) {
        this.specialties = specialties;
    }

    @GetMapping
    ItemsResponse<SpecialtyResponse> list(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return new ItemsResponse<>(specialties.list(onlyActive).stream().map(SpecialtyResponse::from).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SpecialtyResponse create(@Valid @RequestBody SpecialtyRequest request) {
        return SpecialtyResponse.from(
                specialties.create(new CreateSpecialtyCommand(request.name(), request.durationMinutes())));
    }

    @PatchMapping("/{id}")
    SpecialtyResponse update(@PathVariable Long id, @Valid @RequestBody SpecialtyUpdateRequest request) {
        return SpecialtyResponse.from(
                specialties.update(id, new UpdateSpecialtyCommand(request.name(), request.durationMinutes())));
    }

    @PatchMapping("/{id}/active")
    SpecialtyResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return SpecialtyResponse.from(specialties.setActive(id, request.active()));
    }
}
