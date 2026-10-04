package com.citas.api.infrastructure.adapters.in.web.booking;

import com.citas.api.application.port.in.BookAppointmentUseCase;
import com.citas.api.application.port.in.BookAppointmentUseCase.BookingCommand;
import com.citas.api.application.port.in.ViewOwnAppointmentsUseCase;
import com.citas.api.application.port.in.CancelOwnAppointmentUseCase;
import com.citas.api.application.port.in.ViewAppointmentHistoryUseCase;
import com.citas.api.application.port.in.RequestRescheduleUseCase;
import com.citas.api.application.port.in.RequestRescheduleUseCase.RescheduleCommand;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.AppointmentView;
import com.citas.api.domain.model.appointment.RescheduleInfo;
import com.citas.api.domain.model.appointment.RescheduleRequest;
import com.citas.api.infrastructure.adapters.out.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Reserva de citas del USER autenticado (HU-013, HU-014).
 *
 * <p>Un solo endpoint para ambos flujos: la especialidad elegida decide el estado inicial
 * ({@code APPROVED} para Medicina General, {@code REQUESTED} para las demás). El paciente sale
 * siempre del access token.</p>
 */
@RestController
@RequestMapping("/api/v1/appointments")
class AppointmentController {

    private final BookAppointmentUseCase booking;
    private final ViewOwnAppointmentsUseCase ownAppointments;
    private final CancelOwnAppointmentUseCase cancellation;
    private final ViewAppointmentHistoryUseCase history;
    private final RequestRescheduleUseCase reschedules;

    AppointmentController(BookAppointmentUseCase booking, ViewOwnAppointmentsUseCase ownAppointments,
                          CancelOwnAppointmentUseCase cancellation, ViewAppointmentHistoryUseCase history,
                          RequestRescheduleUseCase reschedules) {
        this.booking = booking;
        this.ownAppointments = ownAppointments;
        this.cancellation = cancellation;
        this.history = history;
        this.reschedules = reschedules;
    }

    @GetMapping
    AppointmentListResponse list(@AuthenticationPrincipal AuthenticatedUser user,
                                 @RequestParam(required = false) AppointmentStatus status,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new AppointmentListResponse(ownAppointments.list(user.userId(), status, from, to).stream()
                .map(AppointmentViewResponse::from).toList());
    }

    @GetMapping("/{appointmentId}")
    AppointmentViewResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId) {
        return AppointmentViewResponse.from(ownAppointments.get(user.userId(), appointmentId));
    }

    @PostMapping("/{appointmentId}/cancel")
    AppointmentResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId) {
        return AppointmentResponse.from(cancellation.cancel(user.userId(), appointmentId));
    }

    @GetMapping("/{appointmentId}/history")
    HistoryResponse history(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId) {
        return new HistoryResponse(history.forPatient(user.userId(), appointmentId).stream().map(HistoryItem::from).toList()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AppointmentResponse book(@AuthenticationPrincipal AuthenticatedUser user,
                             @Valid @RequestBody BookingRequest request) {
        return AppointmentResponse.from(booking.book(user.userId(), request.toCommand()));
    }

    /** HU-018: profesional y especialidad son los de la cita; el paciente elige la nueva franja. */
    @PostMapping("/{appointmentId}/reschedule-requests")
    @ResponseStatus(HttpStatus.CREATED)
    RescheduleResponse requestReschedule(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable Long appointmentId,
                                         @Valid @RequestBody RescheduleBody request) {
        return RescheduleResponse.from(reschedules.request(user.userId(), appointmentId,
                new RescheduleCommand(request.siteCode(), request.date(), request.startTime())));
    }

    record RescheduleBody(
            @NotBlank @Size(max = 10) String siteCode,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime) {
    }

    record RescheduleResponse(Long id, Long appointmentId, String status, String originalDate,
                              String originalStartTime, String originalSiteCode, String requestedDate,
                              String requestedStartTime, String requestedSiteCode) {

        static RescheduleResponse from(RescheduleRequest request) {
            return new RescheduleResponse(request.id(), request.appointmentId(), request.status().name(),
                    request.originalStartAt().toLocalDate().toString(),
                    request.originalStartAt().toLocalTime().toString(), request.originalSiteCode(),
                    request.requestedStartAt().toLocalDate().toString(),
                    request.requestedStartAt().toLocalTime().toString(), request.requestedSiteCode());
        }
    }

    record BookingRequest(
            @NotNull Long professionalId,
            @NotNull Long specialtyId,
            @NotBlank @Size(max = 10) String siteCode,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime) {

        BookingCommand toCommand() {
            return new BookingCommand(professionalId, specialtyId, siteCode, date, startTime);
        }
    }

    record AppointmentResponse(Long id, String status, Long professionalId, Long specialtyId, String siteCode,
                               String date, String startTime, String endTime, int durationMinutes) {

        static AppointmentResponse from(Appointment appointment) {
            return new AppointmentResponse(appointment.getId(), appointment.getStatus().name(),
                    appointment.getProfessionalId(), appointment.getSpecialtyId(), appointment.getSiteCode(),
                    appointment.getStartAt().toLocalDate().toString(),
                    appointment.getStartAt().toLocalTime().toString(),
                    appointment.getEndAt().toLocalTime().toString(), appointment.getDurationMinutes());
        }
    }

    record AppointmentListResponse(java.util.List<AppointmentViewResponse> items) {
    }
    record HistoryResponse(java.util.List<HistoryItem> items) { }
    record HistoryItem(String status,String source,Long actorUserId,String changedAt,String reason) { static HistoryItem from(com.citas.api.domain.model.appointment.AppointmentHistoryEntry h){return new HistoryItem(h.status().name(),h.source(),h.actorUserId(),h.changedAt().toString(),h.reason());} }

    record AppointmentViewResponse(Long id, String status, String professionalName, String specialtyName,
                                   String siteCode, String siteName, String date, String startTime, String endTime,
                                   int durationMinutes, String rejectionReason, Long professionalId,
                                   Long specialtyId, RescheduleSummaryResponse reschedule) {
        static AppointmentViewResponse from(AppointmentView appointment) {
            return new AppointmentViewResponse(appointment.id(), appointment.status().name(),
                    appointment.professionalName(), appointment.specialtyName(), appointment.siteCode(),
                    appointment.siteName(), appointment.startAt().toLocalDate().toString(),
                    appointment.startAt().toLocalTime().toString(), appointment.endAt().toLocalTime().toString(),
                    appointment.durationMinutes(), appointment.rejectionReason(), appointment.professionalId(),
                    appointment.specialtyId(), RescheduleSummaryResponse.from(appointment.reschedule()));
        }
    }

    /** Última reprogramación de la cita; {@code null} si nunca se pidió. */
    record RescheduleSummaryResponse(Long id, String status, String requestedDate, String requestedStartTime,
                                     String requestedSiteCode, String decisionReason) {
        static RescheduleSummaryResponse from(RescheduleInfo info) {
            if (info == null) {
                return null;
            }
            return new RescheduleSummaryResponse(info.id(), info.status().name(),
                    info.requestedStartAt().toLocalDate().toString(),
                    info.requestedStartAt().toLocalTime().toString(), info.requestedSiteCode(),
                    info.decisionReason());
        }
    }
}
