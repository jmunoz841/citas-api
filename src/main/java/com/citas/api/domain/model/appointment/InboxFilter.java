package com.citas.api.domain.model.appointment;

import java.time.LocalDate;

/**
 * Filtros combinables de la bandeja administrativa (HU-022). Cada filtro nulo se ignora; las
 * fechas son inclusivas.
 */
public record InboxFilter(String siteCode, Long professionalId, Long specialtyId, LocalDate from, LocalDate to) {

    public static InboxFilter none() {
        return new InboxFilter(null, null, null, null, null);
    }
}
