package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.StatusChange;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Citas, ocupación de slots e historial (V6).
 *
 * <p>La ocupación se inserta con SQL nativo y no con {@code save()}: {@code slot_reservations}
 * tiene clave natural ({@code slot_id}) y {@code save()} haría un {@code merge} que lee antes de
 * escribir, y podría convertir en UPDATE lo que debe fallar como INSERT duplicado.</p>
 */
@Component
class AppointmentPersistenceAdapter implements AppointmentRepositoryPort {

    private final AppointmentJpaRepository appointments;
    private final AppointmentStatusHistoryJpaRepository history;

    AppointmentPersistenceAdapter(AppointmentJpaRepository appointments,
                                  AppointmentStatusHistoryJpaRepository history) {
        this.appointments = appointments;
        this.history = history;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentJpaEntity entity = appointments.save(new AppointmentJpaEntity(appointment.getId(),
                appointment.getPatientUserId(), appointment.getProfessionalId(), appointment.getSpecialtyId(),
                appointment.getSiteCode(), appointment.getStartAt(), (short) appointment.getDurationMinutes(),
                appointment.getStatus().name()));
        return appointment.withId(entity.getId());
    }

    @Override
    public void occupySlots(Appointment appointment, List<AgendaSlot> slots) {
        // Orden fijo por id: dos reservas que compiten por slots comunes bloquean las filas en el
        // mismo orden y no pueden caer en un interbloqueo.
        List<AgendaSlot> ordered = slots.stream().sorted(Comparator.comparing(AgendaSlot::slotId)).toList();
        try {
            for (AgendaSlot slot : ordered) {
                appointments.insertSlotReservation(slot.slotId(), appointment.getProfessionalId(),
                        appointment.getId());
            }
        } catch (DataIntegrityViolationException e) {
            // 1062 sobre la PK: otra cita ya ocupa el slot (HU-013 CA-02, CA-03).
            String detail = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (detail.contains("slot_reservations.primary")) {
                throw BusinessConflictException.slotUnavailable();
            }
            throw e;
        }
    }

    @Override
    public void recordStatus(StatusChange change) {
        history.save(new AppointmentStatusHistoryJpaEntity(change.appointmentId(), change.status().name(),
                change.source().name(), change.actorUserId(), change.reason()));
    }
}

@Entity
@Table(name = "appointments")
class AppointmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_user_id", nullable = false)
    private Long patientUserId;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "specialty_id", nullable = false)
    private Long specialtyId;

    @Column(name = "site_code", nullable = false, length = 10)
    private String siteCode;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /** SNAPSHOT de la duración de la especialidad; {@code end_at} lo genera MySQL. */
    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    /** Bloqueo optimista para las transiciones de estado (HU-015). */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    protected AppointmentJpaEntity() {
    }

    AppointmentJpaEntity(Long id, Long patientUserId, Long professionalId, Long specialtyId, String siteCode,
                         LocalDateTime startAt, Short durationMinutes, String statusCode) {
        this.id = id;
        this.patientUserId = patientUserId;
        this.professionalId = professionalId;
        this.specialtyId = specialtyId;
        this.siteCode = siteCode;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.statusCode = statusCode;
    }

    Long getId() { return id; }
    Long getPatientUserId() { return patientUserId; }
    Long getProfessionalId() { return professionalId; }
    Long getSpecialtyId() { return specialtyId; }
    String getSiteCode() { return siteCode; }
    LocalDateTime getStartAt() { return startAt; }
    Short getDurationMinutes() { return durationMinutes; }
    String getStatusCode() { return statusCode; }
}

@Entity
@Table(name = "appointment_status_history")
class AppointmentStatusHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private Long appointmentId;

    @Column(name = "status_code", nullable = false, length = 20, updatable = false)
    private String statusCode;

    @Column(name = "source", nullable = false, length = 10, updatable = false)
    private String source;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    protected AppointmentStatusHistoryJpaEntity() {
    }

    AppointmentStatusHistoryJpaEntity(Long appointmentId, String statusCode, String source, Long actorUserId,
                                      String reason) {
        this.appointmentId = appointmentId;
        this.statusCode = statusCode;
        this.source = source;
        this.actorUserId = actorUserId;
        this.reason = reason;
    }

    Long getId() { return id; }
}

interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO slot_reservations (slot_id, professional_id, appointment_id)
            VALUES (:slotId, :professionalId, :appointmentId)
            """, nativeQuery = true)
    void insertSlotReservation(Long slotId, Long professionalId, Long appointmentId);
}

interface AppointmentStatusHistoryJpaRepository extends JpaRepository<AppointmentStatusHistoryJpaEntity, Long> {
}
