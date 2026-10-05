package com.citas.api.domain.model.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Resumen operativo de un día (HU-025): solo conteos agregados, sin datos personales.
 *
 * @param byStatus           citas del día por estado
 * @param sites              citas del día por sede y estado
 * @param specialties        citas del día por especialidad
 * @param pendingRequests    citas especializadas {@code REQUESTED} futuras, de cualquier día
 * @param pendingReschedules reprogramaciones {@code PENDING}, de cualquier día
 */
public record DailySummary(LocalDate date, long total, Map<String, Long> byStatus, List<SiteCount> sites,
                           List<SpecialtyCount> specialties, long pendingRequests, long pendingReschedules) {

    public record SiteCount(String siteCode, String siteName, long total, Map<String, Long> byStatus) {
    }

    public record SpecialtyCount(String specialtyName, long total) {
    }
}
