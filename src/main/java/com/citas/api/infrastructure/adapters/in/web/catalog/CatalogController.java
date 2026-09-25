package com.citas.api.infrastructure.adapters.in.web.catalog;

import com.citas.api.application.port.in.ConsultCatalogsUseCase;
import com.citas.api.infrastructure.adapters.in.web.catalog.CatalogDtos.CatalogEntryResponse;
import com.citas.api.infrastructure.adapters.in.web.catalog.CatalogDtos.InsurancePlanResponse;
import com.citas.api.infrastructure.adapters.in.web.catalog.CatalogDtos.ItemsResponse;
import com.citas.api.infrastructure.adapters.in.web.catalog.CatalogDtos.SiteResponse;
import com.citas.api.infrastructure.adapters.in.web.catalog.CatalogDtos.SpecialtyResponse;
import com.citas.api.infrastructure.adapters.in.web.catalog.CatalogDtos.StatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogos fijos (HU-005). Solo lectura: no se exponen POST, PUT, PATCH ni DELETE, así que
 * cualquier intento de escritura obtiene 405 (CA-03). Los valores solo cambian con una migración.
 *
 * <p>Son públicos a propósito: el formulario de registro necesita los tipos de documento y las
 * sedes antes de que exista una sesión.</p>
 */
@RestController
@RequestMapping("/api/v1/catalogs")
class CatalogController {

    private final ConsultCatalogsUseCase catalogs;

    CatalogController(ConsultCatalogsUseCase catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping("/sites")
    ItemsResponse<SiteResponse> sites() {
        return ItemsResponse.of(catalogs.sites(), SiteResponse::from);
    }

    @GetMapping("/document-types")
    ItemsResponse<CatalogEntryResponse> documentTypes() {
        return ItemsResponse.of(catalogs.documentTypes(), CatalogEntryResponse::from);
    }

    @GetMapping("/regimes")
    ItemsResponse<CatalogEntryResponse> regimes() {
        return ItemsResponse.of(catalogs.regimes(), CatalogEntryResponse::from);
    }

    /** Planes seleccionables para la afiliación opcional del registro (HU-004). */
    @GetMapping("/insurance-plans")
    ItemsResponse<InsurancePlanResponse> insurancePlans() {
        return ItemsResponse.of(catalogs.insurancePlans(), InsurancePlanResponse::from);
    }

    /**
     * Especialidades activas para el filtro de búsqueda y el modal de reserva (HU-012). Es oferta
     * pública, no un catálogo fijo: la administra el ADMIN en {@code /api/v1/admin/specialties}.
     */
    @GetMapping("/specialties")
    ItemsResponse<SpecialtyResponse> specialties() {
        return ItemsResponse.of(catalogs.activeSpecialties(), SpecialtyResponse::from);
    }

    @GetMapping("/roles")
    ItemsResponse<CatalogEntryResponse> roles() {
        return ItemsResponse.of(catalogs.roles(), CatalogEntryResponse::from);
    }

    @GetMapping("/appointment-statuses")
    ItemsResponse<StatusResponse> appointmentStatuses() {
        return ItemsResponse.of(catalogs.appointmentStatuses(), StatusResponse::from);
    }

    @GetMapping("/reschedule-statuses")
    ItemsResponse<StatusResponse> rescheduleStatuses() {
        return ItemsResponse.of(catalogs.rescheduleStatuses(), StatusResponse::from);
    }
}
