package com.citas.api.application.service;

import com.citas.api.application.port.in.BookAppointmentUseCase;
import com.citas.api.application.port.out.AgendaQueryPort;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.application.port.out.ProfessionalRepositoryPort;
import com.citas.api.application.port.out.SpecialtyRepositoryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.agenda.SlotPlanner;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.StatusChange;
import com.citas.api.domain.model.professional.Professional;
import com.citas.api.domain.model.professional.ProfessionalAssignments.SpecialtyAssignment;
import com.citas.api.domain.model.professional.Specialty;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reserva de citas general y especializada (HU-013, HU-014).
 *
 * <p>Cita, ocupación de slots e historial se escriben en una sola transacción. La aplicación
 * <b>no</b> comprueba si los slots están libres antes de ocupar: la clave primaria de
 * {@code slot_reservations} lo decide al insertar, y es la única comprobación segura cuando dos
 * reservas llegan a la vez (HU-013 CA-03). Si falla, la transacción entera se deshace.</p>
 */
public class AppointmentBookingService implements BookAppointmentUseCase {

    private final SpecialtyRepositoryPort specialties;
    private final ProfessionalRepositoryPort professionals;
    private final AgendaQueryPort agenda;
    private final AppointmentRepositoryPort appointments;
    private final Clock clock;

    public AppointmentBookingService(SpecialtyRepositoryPort specialties, ProfessionalRepositoryPort professionals,
                                     AgendaQueryPort agenda, AppointmentRepositoryPort appointments, Clock clock) {
        this.specialties = specialties;
        this.professionals = professionals;
        this.agenda = agenda;
        this.appointments = appointments;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Appointment book(Long patientUserId, BookingCommand command) {
        if (command.date() == null || command.startTime() == null) {
            throw new InvalidFieldException("startTime", "Fecha y hora de inicio son obligatorias");
        }
        Specialty specialty = requireActiveSpecialty(command.specialtyId());
        Professional professional = requireProfessionalFor(command.professionalId(), specialty);
        String siteCode = requireAssignedSite(professional, command.siteCode());

        LocalDateTime startAt = LocalDateTime.of(command.date(), command.startTime());
        if (!startAt.isAfter(LocalDateTime.now(clock))) {
            throw new InvalidFieldException("startTime", "No se puede reservar un horario pasado");
        }

        List<AgendaSlot> slots = agenda.findSlots(List.of(professional.getUserId()), startAt,
                startAt.plusMinutes(specialty.getDurationMinutes()), siteCode);
        List<AgendaSlot> run = SlotPlanner.allocate(slots, startAt, specialty.getDurationMinutes())
                .orElseThrow(BusinessConflictException::slotUnavailable);

        Appointment appointment = appointments.save(
                Appointment.book(patientUserId, professional.getUserId(), specialty, siteCode, startAt));
        appointments.occupySlots(appointment, run);
        appointments.recordStatus(StatusChange.initial(appointment));
        return appointment;
    }

    /** Especialidad inexistente o inactiva: no se puede reservar (HU-014 CA-03). */
    private Specialty requireActiveSpecialty(Long specialtyId) {
        return specialties.findById(specialtyId)
                .filter(Specialty::isActive)
                .orElseThrow(() -> new InvalidFieldException("specialtyId",
                        "La especialidad no existe o no está activa"));
    }

    /** Profesional activo que atiende la especialidad (RN-08, HU-009 CA-02, HU-014 CA-03). */
    private Professional requireProfessionalFor(Long professionalId, Specialty specialty) {
        Professional professional = professionals.findByUserId(professionalId)
                .filter(Professional::isActive)
                .orElseThrow(() -> new InvalidFieldException("professionalId",
                        "El profesional no existe o no está activo"));
        boolean attends = professional.getAssignments().specialties().stream()
                .map(SpecialtyAssignment::specialtyId)
                .anyMatch(specialty.getId()::equals);
        if (!attends) {
            throw new InvalidFieldException("specialtyId", "El profesional no atiende esa especialidad");
        }
        return professional;
    }

    private static String requireAssignedSite(Professional professional, String siteCode) {
        String normalized = siteCode == null ? "" : siteCode.trim().toUpperCase();
        if (!professional.getAssignments().siteCodes().contains(normalized)) {
            throw new InvalidFieldException("siteCode", "El profesional no atiende en esa sede");
        }
        return normalized;
    }
}
