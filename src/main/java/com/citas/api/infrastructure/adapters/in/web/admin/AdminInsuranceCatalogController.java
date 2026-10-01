package com.citas.api.infrastructure.adapters.in.web.admin;

import com.citas.api.application.port.in.ManageInsuranceCatalogUseCase;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.ActiveRequest;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.InsurancePlanResponse;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.InsuranceProviderResponse;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.ItemsResponse;
import com.citas.api.infrastructure.adapters.in.web.admin.AdminDtos.NameRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Administración de EPS y planes; no expone DELETE, solo baja lógica. */
@RestController @RequestMapping("/api/v1/admin/eps")
class AdminInsuranceCatalogController {
    private final ManageInsuranceCatalogUseCase catalogs;
    AdminInsuranceCatalogController(ManageInsuranceCatalogUseCase catalogs) { this.catalogs = catalogs; }
    @GetMapping ItemsResponse<InsuranceProviderResponse> list(@RequestParam(defaultValue = "false") boolean onlyActive) { return new ItemsResponse<>(catalogs.listProviders(onlyActive).stream().map(InsuranceProviderResponse::from).toList()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) InsuranceProviderResponse create(@Valid @RequestBody NameRequest request) { return InsuranceProviderResponse.from(catalogs.createProvider(request.name())); }
    @PatchMapping("/{id}") InsuranceProviderResponse update(@PathVariable Long id, @Valid @RequestBody NameRequest request) { return InsuranceProviderResponse.from(catalogs.updateProvider(id, request.name())); }
    @PatchMapping("/{id}/active") InsuranceProviderResponse active(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) { return InsuranceProviderResponse.from(catalogs.setProviderActive(id, request.active())); }
    @GetMapping("/{id}/plans") ItemsResponse<InsurancePlanResponse> plans(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean onlyActive) { return new ItemsResponse<>(catalogs.listPlans(id, onlyActive).stream().map(InsurancePlanResponse::from).toList()); }
    @PostMapping("/{id}/plans") @ResponseStatus(HttpStatus.CREATED) InsurancePlanResponse createPlan(@PathVariable Long id, @Valid @RequestBody NameRequest request) { return InsurancePlanResponse.from(catalogs.createPlan(id, request.name())); }
    @PatchMapping("/plans/{planId}") InsurancePlanResponse updatePlan(@PathVariable Long planId, @Valid @RequestBody NameRequest request) { return InsurancePlanResponse.from(catalogs.updatePlan(planId, request.name())); }
    @PatchMapping("/plans/{planId}/active") InsurancePlanResponse activePlan(@PathVariable Long planId, @Valid @RequestBody ActiveRequest request) { return InsurancePlanResponse.from(catalogs.setPlanActive(planId, request.active())); }
}
