package com.citas.api.infrastructure.adapters.in.web.booking;

import com.citas.api.application.port.in.BookAppointmentUseCase;
import com.citas.api.application.port.in.BookAppointmentUseCase.BookingCommand;
import com.citas.api.application.port.in.ViewOwnAppointmentsUseCase;
import com.citas.api.application.port.in.CancelOwnAppointmentUseCase;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.AppointmentView;
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

    AppointmentController(BookAppointmentUseCase booking, ViewOwnAppointmentsUseCase ownAppointments,
                          CancelOwnAppointmentUseCase cancellation) {
        this.booking = booking;
        this.ownAppointments = ownAppointments;
        this.cancellation = cancellation;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AppointmentResponse book(@AuthenticationPrincipal AuthenticatedUser user,
                             @Valid @RequestBody BookingRequest request) {
        return AppointmentResponse.from(booking.book(user.userId(), request.toCommand()));
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

    record AppointmentViewResponse(Long id, String status, String professionalName, String specialtyName,
                                   String siteCode, String siteName, String date, String startTime, String endTime,
                                   int durationMinutes, String rejectionReason) {
        static AppointmentViewResponse from(AppointmentView appointment) {
            return new AppointmentViewResponse(appointment.id(), appointment.status().name(),
                    appointment.professionalName(), appointment.specialtyName(), appointment.siteCode(),
                    appointment.siteName(), appointment.startAt().toLocalDate().toString(),
                    appointment.startAt().toLocalTime().toString(), appointment.endAt().toLocalTime().toString(),
                    appointment.durationMinutes(), appointment.rejectionReason());
        }
    }
}
