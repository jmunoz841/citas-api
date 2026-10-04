package com.citas.api.infrastructure.adapters.in.web.admin;

import com.citas.api.application.port.in.ResolveAppointmentRequestUseCase;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentSummary;
import com.citas.api.domain.model.appointment.InboxFilter;
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
 * Solicitudes de cita especializada pendientes de decisión (HU-015). Solo ADMIN, por la regla
 * de {@code /api/v1/admin/**} en {@code SecurityConfig}; el ADMIN que decide sale del token.
 */
@RestController
@RequestMapping("/api/v1/admin/appointments")
class AdminAppointmentController {

    private final ResolveAppointmentRequestUseCase requests;

    AdminAppointmentController(ResolveAppointmentRequestUseCase requests) {
        this.requests = requests;
    }

    /** HU-022: filtros opcionales y combinables; las fechas son inclusivas. */
    @GetMapping("/requests")
    ItemsResponse<RequestResponse> listRequested(
            @RequestParam(required = false) String siteCode,
            @RequestParam(required = false) Long professionalId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        InboxFilter filter = new InboxFilter(siteCode, professionalId, specialtyId, from, to);
        return new ItemsResponse<>(requests.listRequested(filter).stream().map(RequestResponse::from).toList());
    }

    @PostMapping("/{id}/approve")
    DecisionResponse approve(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return DecisionResponse.from(requests.approve(admin.userId(), id));
    }

    @PostMapping("/{id}/reject")
    DecisionResponse reject(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id,
                            @Valid @RequestBody RejectRequest request) {
        return DecisionResponse.from(requests.reject(admin.userId(), id, request.reason()));
    }

    record RejectRequest(@NotBlank @Size(max = Appointment.MAX_REASON_LENGTH) String reason) {
    }

    record RequestResponse(Long id, String status, String patientName, String professionalName,
                           String specialtyName, String siteCode, String date, String startTime, String endTime,
                           int durationMinutes) {

        static RequestResponse from(AppointmentSummary summary) {
            return new RequestResponse(summary.id(), summary.status().name(), summary.patientName(),
                    summary.professionalName(), summary.specialtyName(), summary.siteCode(),
                    summary.startAt().toLocalDate().toString(), summary.startAt().toLocalTime().toString(),
                    summary.endAt().toLocalTime().toString(), summary.durationMinutes());
        }
    }

    record DecisionResponse(Long id, String status) {

        static DecisionResponse from(Appointment appointment) {
            return new DecisionResponse(appointment.getId(), appointment.getStatus().name());
        }
    }

    record ItemsResponse<T>(List<T> items) {
    }
}
