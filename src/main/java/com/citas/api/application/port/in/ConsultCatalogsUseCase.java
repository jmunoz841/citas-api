package com.citas.api.application.port.in;

import com.citas.api.domain.model.affiliation.InsurancePlan;
import com.citas.api.domain.model.catalog.CatalogEntry;
import com.citas.api.domain.model.catalog.Site;
import com.citas.api.domain.model.catalog.StatusEntry;
import com.citas.api.domain.model.professional.Specialty;

import java.util.List;

/**
 * Consulta de los catálogos fijos del sistema (HU-005).
 */
public interface ConsultCatalogsUseCase {

    List<Site> sites();

    List<CatalogEntry> regimes();

    List<CatalogEntry> roles();

    List<CatalogEntry> documentTypes();

    List<StatusEntry> appointmentStatuses();

    List<StatusEntry> rescheduleStatuses();

    /** Planes de EPS seleccionables: plan activo y EPS activa (HU-004). */
    List<InsurancePlan> insurancePlans();

    /** Especialidades activas, por nombre, para buscar y reservar citas (HU-012). */
    List<Specialty> activeSpecialties();
}
