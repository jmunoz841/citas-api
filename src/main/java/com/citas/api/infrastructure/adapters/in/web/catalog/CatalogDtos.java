package com.citas.api.infrastructure.adapters.in.web.catalog;

import com.citas.api.domain.model.affiliation.InsurancePlan;
import com.citas.api.domain.model.catalog.CatalogEntry;
import com.citas.api.domain.model.catalog.Site;
import com.citas.api.domain.model.catalog.StatusEntry;

import java.util.List;

/**
 * DTOs de los catálogos fijos (HU-005). Cada respuesta se envuelve en un objeto con la clave
 * {@code items} para poder añadir metadatos más adelante sin romper el contrato.
 */
final class CatalogDtos {

    private CatalogDtos() {
    }

    record CatalogEntryResponse(String code, String name) {

        static CatalogEntryResponse from(CatalogEntry entry) {
            return new CatalogEntryResponse(entry.code(), entry.name());
        }
    }

    record StatusResponse(String code, String name, boolean terminal) {

        static StatusResponse from(StatusEntry entry) {
            return new StatusResponse(entry.code(), entry.name(), entry.terminal());
        }
    }

    record InsurancePlanResponse(Long id, String name, Long epsId, String epsName) {

        static InsurancePlanResponse from(InsurancePlan plan) {
            return new InsurancePlanResponse(plan.id(), plan.name(), plan.epsId(), plan.epsName());
        }
    }

    record SiteResponse(String code, String name, String address) {

        static SiteResponse from(Site site) {
            return new SiteResponse(site.code(), site.name(), site.address());
        }
    }

    record ItemsResponse<T>(List<T> items) {

        static <D, T> ItemsResponse<T> of(List<D> source, java.util.function.Function<D, T> mapper) {
            return new ItemsResponse<>(source.stream().map(mapper).toList());
        }
    }
}
