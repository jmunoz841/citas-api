package com.citas.api.application.port.out;

import com.citas.api.domain.model.catalog.CatalogEntry;
import com.citas.api.domain.model.catalog.Site;
import com.citas.api.domain.model.catalog.StatusEntry;

import java.util.List;

/**
 * Lectura de los catálogos fijos sembrados por migración (HU-005). No hay operaciones de
 * escritura: los catálogos fijos solo cambian con una migración Flyway.
 */
public interface CatalogRepositoryPort {

    List<Site> findSites();

    List<CatalogEntry> findRegimes();

    List<CatalogEntry> findRoles();

    List<CatalogEntry> findDocumentTypes();

    List<StatusEntry> findAppointmentStatuses();

    List<StatusEntry> findRescheduleStatuses();
}
