package com.citas.api.application.service;

import com.citas.api.application.port.in.RequestRescheduleUseCase;
import com.citas.api.application.port.out.AgendaQueryPort;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.application.port.out.RescheduleRepositoryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.agenda.SlotPlanner;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.RescheduleRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Solicitud de reprogramación del USER (HU-018).
 *
 * <p>La cita conserva su franja; la solicitud retiene la nueva en la misma transacción. Como en
 * la reserva, la aplicación <b>no</b> comprueba si los slots están libres antes de retenerlos: lo
 * decide la clave primaria de {@code slot_reservations} al insertar (HU-018 CA-03). Una segunda
 * solicitud pendiente la impide el índice {@code uk_rr_one_pending_per_appointment} (CA-04).</p>
 */
public class RescheduleRequestService implements RequestRescheduleUseCase {

    private final AppointmentRepositoryPort appointments;
    private final RescheduleRepositoryPort reschedules;
    private final AgendaQueryPort agenda;
    private final Clock clock;

    public RescheduleRequestService(AppointmentRepositoryPort appointments, RescheduleRepositoryPort reschedules,
                                    AgendaQueryPort agenda, Clock clock) {
        this.appointments = appointments;
        this.reschedules = reschedules;
        this.agenda = agenda;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RescheduleRequest request(Long patientUserId, Long appointmentId, RescheduleCommand command) {
        if (command.date() == null || command.startTime() == null) {
            throw new InvalidFieldException("startTime", "Fecha y hora de inicio son obligatorias");
        }
        if (command.siteCode() == null || command.siteCode().isBlank()) {
            throw new InvalidFieldException("siteCode", "La sede es obligatoria");
        }
        // Una cita ajena se trata como inexistente: no se revela que existe.
        Appointment appointment = appointments.findById(appointmentId)
                .filter(current -> current.getPatientUserId().equals(patientUserId))
                .orElseThrow(ResourceNotFoundException::appointment);

        LocalDateTime requestedStartAt = LocalDateTime.of(command.date(), command.startTime());
        String siteCode = command.siteCode().trim().toUpperCase();
        RescheduleRequest draft = RescheduleRequest.open(appointment, requestedStartAt, siteCode,
                LocalDateTime.now(clock));
        if (reschedules.findPendingByAppointment(appointmentId).isPresent()) {
            throw BusinessConflictException.reschedulePending();
        }
        requireStillOffered(appointment);

        List<AgendaSlot> slots = agenda.findSlots(List.of(appointment.getProfessionalId()), requestedStartAt,
                requestedStartAt.plusMinutes(appointment.getDurationMinutes()), siteCode);
        List<AgendaSlot> run = SlotPlanner.allocate(slots, requestedStartAt, appointment.getDurationMinutes())
                .orElseThrow(BusinessConflictException::slotUnavailable);

        RescheduleRequest saved = reschedules.save(draft);
        reschedules.holdSlots(saved, appointment.getProfessionalId(), run);
        return saved;
    }

    /** Mismo profesional y especialidad, ambos todavía activos y asociados (RF-15, RN-08). */
    private void requireStillOffered(Appointment appointment) {
        if (agenda.findOffers(appointment.getSpecialtyId(), null, appointment.getProfessionalId()).isEmpty()) {
            throw new InvalidFieldException("appointmentId",
                    "El profesional ya no atiende esa especialidad; reserva una cita nueva");
        }
    }
}
