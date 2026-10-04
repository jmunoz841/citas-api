package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.domain.model.appointment.InboxFilter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Parámetros SQL de los filtros de la bandeja (HU-022): sede normalizada y rango de fechas
 * inclusivo convertido a [inicio del primer día, inicio del día siguiente al último).
 */
final class InboxParameters {

    private InboxParameters() {
    }

    static MapSqlParameterSource of(InboxFilter filter) {
        String siteCode = filter.siteCode() == null || filter.siteCode().isBlank()
                ? null : filter.siteCode().trim().toUpperCase();
        return new MapSqlParameterSource()
                .addValue("siteCode", siteCode)
                .addValue("professionalId", filter.professionalId())
                .addValue("specialtyId", filter.specialtyId())
                .addValue("from", filter.from() == null ? null : filter.from().atStartOfDay())
                .addValue("to", filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay());
    }
}
