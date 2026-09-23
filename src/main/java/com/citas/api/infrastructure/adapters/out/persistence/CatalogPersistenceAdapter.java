package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.CatalogRepositoryPort;
import com.citas.api.domain.model.catalog.CatalogEntry;
import com.citas.api.domain.model.catalog.Site;
import com.citas.api.domain.model.catalog.StatusEntry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Traduce las entidades de catálogo a modelo de dominio. Las entidades JPA no salen de este paquete.
 */
@Component
class CatalogPersistenceAdapter implements CatalogRepositoryPort {

    private final SiteJpaRepository sites;
    private final RegimeJpaRepository regimes;
    private final RoleJpaRepository roles;
    private final DocumentTypeJpaRepository documentTypes;
    private final AppointmentStatusJpaRepository appointmentStatuses;
    private final RescheduleStatusJpaRepository rescheduleStatuses;

    CatalogPersistenceAdapter(SiteJpaRepository sites, RegimeJpaRepository regimes, RoleJpaRepository roles,
                              DocumentTypeJpaRepository documentTypes,
                              AppointmentStatusJpaRepository appointmentStatuses,
                              RescheduleStatusJpaRepository rescheduleStatuses) {
        this.sites = sites;
        this.regimes = regimes;
        this.roles = roles;
        this.documentTypes = documentTypes;
        this.appointmentStatuses = appointmentStatuses;
        this.rescheduleStatuses = rescheduleStatuses;
    }

    @Override
    public List<Site> findSites() {
        return sites.findAllByOrderByCodeAsc().stream()
                .map(entity -> new Site(entity.getCode(), entity.getName(), entity.getAddress()))
                .toList();
    }

    @Override
    public List<CatalogEntry> findRegimes() {
        return regimes.findAllByOrderByCodeAsc().stream().map(CatalogPersistenceAdapter::toEntry).toList();
    }

    @Override
    public List<CatalogEntry> findRoles() {
        return roles.findAllByOrderByCodeAsc().stream().map(CatalogPersistenceAdapter::toEntry).toList();
    }

    @Override
    public List<CatalogEntry> findDocumentTypes() {
        return documentTypes.findAllByOrderByCodeAsc().stream().map(CatalogPersistenceAdapter::toEntry).toList();
    }

    @Override
    public List<StatusEntry> findAppointmentStatuses() {
        return appointmentStatuses.findAllByOrderByCodeAsc().stream().map(CatalogPersistenceAdapter::toStatus).toList();
    }

    @Override
    public List<StatusEntry> findRescheduleStatuses() {
        return rescheduleStatuses.findAllByOrderByCodeAsc().stream().map(CatalogPersistenceAdapter::toStatus).toList();
    }

    private static CatalogEntry toEntry(CodeNameJpaEntity entity) {
        return new CatalogEntry(entity.getCode(), entity.getName());
    }

    private static StatusEntry toStatus(StatusJpaEntity entity) {
        return new StatusEntry(entity.getCode(), entity.getName(), entity.isTerminal());
    }
}
