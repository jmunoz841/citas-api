package com.citas.api.application.service;

import com.citas.api.application.port.in.ConsultCatalogsUseCase;
import com.citas.api.application.port.out.CatalogRepositoryPort;
import com.citas.api.domain.model.catalog.CatalogEntry;
import com.citas.api.domain.model.catalog.Site;
import com.citas.api.domain.model.catalog.StatusEntry;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogos fijos de HU-005. Son datos de referencia inmutables desde la API: este servicio
 * solo lee. Cualquier cambio de valores entra por una migración Flyway.
 */
public class CatalogService implements ConsultCatalogsUseCase {

    private final CatalogRepositoryPort catalogs;

    public CatalogService(CatalogRepositoryPort catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Site> sites() {
        return catalogs.findSites();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogEntry> regimes() {
        return catalogs.findRegimes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogEntry> roles() {
        return catalogs.findRoles();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogEntry> documentTypes() {
        return catalogs.findDocumentTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusEntry> appointmentStatuses() {
        return catalogs.findAppointmentStatuses();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusEntry> rescheduleStatuses() {
        return catalogs.findRescheduleStatuses();
    }
}
