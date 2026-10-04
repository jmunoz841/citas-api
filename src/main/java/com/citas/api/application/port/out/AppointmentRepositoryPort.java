package com.citas.api.application.port.out;

import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.AppointmentSummary;
import com.citas.api.domain.model.appointment.AppointmentView;
import com.citas.api.domain.model.appointment.InboxFilter;
import com.citas.api.domain.model.appointment.ProfessionalAppointmentView;
import com.citas.api.domain.model.appointment.AppointmentHistoryEntry;
import com.citas.api.domain.model.appointment.StatusChange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Citas, ocupación de slots e historial (V6).
 */
public interface AppointmentRepositoryPort {

    Appointment save(Appointment appointment);

    /**
     * Ocupa los slots a nombre de la cita. Si alguno ya está ocupado, la clave primaria de
     * {@code slot_reservations} rechaza la inserción y se lanza
     * {@link com.citas.api.domain.exception.BusinessConflictException#slotUnavailable()}.
     */
    void occupySlots(Appointment appointment, List<AgendaSlot> slots);

    void recordStatus(StatusChange change);

    Optional<Appointment> findById(Long appointmentId);

    /**
     * Cambia el estado solo si la cita sigue en {@code expected}. Si otra transacción la resolvió
     * antes, no cambia nada y lanza
     * {@link com.citas.api.domain.exception.BusinessConflictException#invalidStatusTransition()}.
     */
    void changeStatus(Appointment updated, AppointmentStatus expected);

    /** Libera los slots de la cita: vuelven a estar disponibles (RN-09). */
    void releaseSlots(Long appointmentId);

    /**
     * Mueve la cita a su nueva sede y horario solo si sigue {@code APPROVED}; si no, lanza
     * {@link com.citas.api.domain.exception.BusinessConflictException#appointmentNotReschedulable()}
     * (HU-019 CA-01).
     */
    void updateSchedule(Appointment moved);

    /** Citas {@code REQUESTED} que cumplen los filtros, las más próximas primero (HU-015, HU-022). */
    List<AppointmentSummary> findRequested(InboxFilter filter);

    /**
     * Citas del paciente con inicio en [{@code from}, {@code to}), más próxima primero. Cada
     * filtro nulo se ignora.
     */
    List<AppointmentView> findViewsByPatient(Long patientUserId, AppointmentStatus status, LocalDateTime from,
                                             LocalDateTime to);

    Optional<AppointmentView> findViewByIdAndPatient(Long appointmentId, Long patientUserId);

    List<ProfessionalAppointmentView> findApprovedViewsByProfessional(Long professionalId, LocalDateTime from,
                                                                       LocalDateTime to, String siteCode);
    List<AppointmentHistoryEntry> findHistory(Long appointmentId);
}
