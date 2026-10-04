package com.citas.api.infrastructure.adapters.in.web.admin;

import com.citas.api.application.port.in.ResolveRescheduleUseCase;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.InboxFilter;
import com.citas.api.domain.model.appointment.RescheduleRequest;
import com.citas.api.domain.model.appointment.RescheduleSummary;
import com.citas.api.infrastructure.adapters.out.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reprogramaciones pendientes y su decisión (HU-019, HU-022). Solo ADMIN, por la regla de
 * {@code /api/v1/admin/**} en {@code SecurityConfig}; el ADMIN que decide sale del token.
 */
@RestController
@RequestMapping("/api/v1/admin/reschedule-requests")
class AdminRescheduleController {

    private final ResolveRescheduleUseCase reschedules;

    AdminRescheduleController(ResolveRescheduleUseCase reschedules) {
        this.reschedules = reschedules;
    }

    /** Filtros opcionales y combinables sobre la franja solicitada; las fechas son inclusivas. */
    @GetMapping
    ItemsResponse<PendingResponse> listPending(
            @RequestParam(required = false) String siteCode,
            @RequestParam(required = false) Long professionalId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        InboxFilter filter = new InboxFilter(siteCode, professionalId, specialtyId, from, to);
        return new ItemsResponse<>(reschedules.listPending(filter).stream().map(PendingResponse::from).toList());
    }

    @PostMapping("/{id}/approve")
    DecisionResponse approve(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return DecisionResponse.from(reschedules.approve(admin.userId(), id));
    }

    @PostMapping("/{id}/reject")
    DecisionResponse reject(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id,
                            @Valid @RequestBody RejectRequest request) {
        return DecisionResponse.from(reschedules.reject(admin.userId(), id, request.reason()));
    }

    record RejectRequest(@NotBlank @Size(max = Appointment.MAX_REASON_LENGTH) String reason) {
    }

    record PendingResponse(Long id, Long appointmentId, String status, String patientName,
                           String professionalName, String specialtyName, int durationMinutes,
                           String originalDate, String originalStartTime, String originalSiteCode,
                           String requestedDate, String requestedStartTime, String requestedSiteCode,
                           String requestedAt) {

        static PendingResponse from(RescheduleSummary summary) {
            return new PendingResponse(summary.id(), summary.appointmentId(), summary.status().name(),
                    summary.patientName(), summary.professionalName(), summary.specialtyName(),
                    summary.durationMinutes(), summary.originalStartAt().toLocalDate().toString(),
                    summary.originalStartAt().toLocalTime().toString(), summary.originalSiteCode(),
                    summary.requestedStartAt().toLocalDate().toString(),
                    summary.requestedStartAt().toLocalTime().toString(), summary.requestedSiteCode(),
                    summary.requestedAt().toString());
        }
    }

    record DecisionResponse(Long id, Long appointmentId, String status, String decisionReason) {

        static DecisionResponse from(RescheduleRequest request) {
            return new DecisionResponse(request.id(), request.appointmentId(), request.status().name(),
                    request.decisionReason());
        }
    }

    record ItemsResponse<T>(List<T> items) {
    }
}
