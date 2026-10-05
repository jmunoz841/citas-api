package com.citas.api.application.port.in;

import com.citas.api.domain.model.integration.DailySummary;

import java.time.LocalDate;

/** Resumen operativo del día para WF-003 (HU-025). Sin datos personales. */
public interface DailySummaryUseCase {

    /** {@code date} nula = hoy en America/Bogota. */
    DailySummary summary(LocalDate date);
}
