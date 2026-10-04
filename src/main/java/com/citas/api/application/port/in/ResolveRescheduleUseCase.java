package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.InboxFilter;
import com.citas.api.domain.model.appointment.RescheduleRequest;
import com.citas.api.domain.model.appointment.RescheduleSummary;

import java.util.List;

/**
 * Decisión del ADMIN sobre las reprogramaciones pendientes (HU-019) y su bandeja filtrable
 * (HU-022). El ADMIN sale del access token y queda como decisor.
 */
public interface ResolveRescheduleUseCase {

    /** Pendientes que cumplen los filtros; antes cierra las vencidas (D-033). */
    List<RescheduleSummary> listPending(InboxFilter filter);

    RescheduleRequest approve(Long adminUserId, Long requestId);

    RescheduleRequest reject(Long adminUserId, Long requestId, String reason);

    /** Cierra como {@code CANCELLED} las pendientes vencidas y libera su retención (D-033). */
    int expireOverdue();
}
