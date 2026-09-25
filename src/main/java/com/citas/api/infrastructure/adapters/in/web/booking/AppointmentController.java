package com.citas.api.infrastructure.adapters.in.web.booking;

import com.citas.api.application.port.in.BookAppointmentUseCase;
import com.citas.api.application.port.in.BookAppointmentUseCase.BookingCommand;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.infrastructure.adapters.out.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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

    AppointmentController(BookAppointmentUseCase booking) {
        this.booking = booking;
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
}
