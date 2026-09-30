package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.AppointmentSummary;
import com.citas.api.domain.model.appointment.AppointmentView;
import com.citas.api.domain.model.appointment.ProfessionalAppointmentView;
import com.citas.api.domain.model.appointment.AppointmentHistoryEntry;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    private final NamedParameterJdbcTemplate jdbc;

    AppointmentPersistenceAdapter(AppointmentJpaRepository appointments,
                                  AppointmentStatusHistoryJpaRepository history, NamedParameterJdbcTemplate jdbc) {
        this.appointments = appointments;
        this.history = history;
        this.jdbc = jdbc;
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

    @Override
    public Optional<Appointment> findById(Long appointmentId) {
        return appointments.findById(appointmentId).map(entity -> Appointment.restore(entity.getId(),
                entity.getPatientUserId(), entity.getProfessionalId(), entity.getSpecialtyId(), entity.getSiteCode(),
                entity.getStartAt(), entity.getDurationMinutes(), AppointmentStatus.valueOf(entity.getStatusCode())));
    }

    /**
     * UPDATE condicionado al estado esperado. Es una lectura actual de InnoDB: si otra
     * transacción resolvió la cita y confirmó, la condición ya no se cumple y se actualizan 0
     * filas (HU-015, dos decisiones simultáneas).
     */
    @Override
    public void changeStatus(Appointment updated, AppointmentStatus expected) {
        int rows = appointments.updateStatusIf(updated.getId(), updated.getStatus().name(), expected.name());
        if (rows == 0) {
            throw BusinessConflictException.invalidStatusTransition();
        }
    }

    @Override
    public void releaseSlots(Long appointmentId) {
        appointments.deleteSlotReservations(appointmentId);
    }

    @Override
    public List<AppointmentSummary> findByStatus(AppointmentStatus status) {
        return jdbc.query(SUMMARIES, new MapSqlParameterSource("status", status.name()), (rs, row) -> {
            LocalDateTime startAt = rs.getObject("start_at", LocalDateTime.class);
            return new AppointmentSummary(rs.getLong("id"), AppointmentStatus.valueOf(rs.getString("status_code")),
                    rs.getString("patient_name"), rs.getString("professional_name"),
                    rs.getString("specialty_name"), rs.getString("site_code"), startAt,
                    rs.getObject("end_at", LocalDateTime.class), rs.getInt("duration_minutes"));
        });
    }

    @Override
    public List<AppointmentView> findViewsByPatient(Long patientUserId, AppointmentStatus status,
                                                     LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("patientUserId", patientUserId)
                .addValue("status", status == null ? null : status.name())
                .addValue("from", from)
                .addValue("to", to);
        return jdbc.query(PATIENT_VIEWS, parameters, (rs, row) -> toView(rs));
    }

    @Override
    public Optional<AppointmentView> findViewByIdAndPatient(Long appointmentId, Long patientUserId) {
        return jdbc.query(PATIENT_VIEW_BY_ID, new MapSqlParameterSource()
                        .addValue("appointmentId", appointmentId)
                        .addValue("patientUserId", patientUserId),
                (rs, row) -> toView(rs)).stream().findFirst();
    }

    @Override
    public List<ProfessionalAppointmentView> findApprovedViewsByProfessional(Long professionalId, LocalDateTime from,
                                                                              LocalDateTime to, String siteCode) {
        return jdbc.query(PROFESSIONAL_VIEWS, new MapSqlParameterSource().addValue("professionalId", professionalId)
                .addValue("from", from).addValue("to", to).addValue("siteCode", siteCode), (rs, row) ->
                new ProfessionalAppointmentView(rs.getLong("id"), rs.getString("patient_name"),
                        rs.getString("specialty_name"), rs.getString("site_code"),
                        rs.getObject("start_at", LocalDateTime.class), rs.getObject("end_at", LocalDateTime.class),
                        rs.getInt("duration_minutes")));
    }
    @Override public List<AppointmentHistoryEntry> findHistory(Long appointmentId) {
        return jdbc.query("SELECT status_code, source, actor_user_id, changed_at, reason FROM appointment_status_history WHERE appointment_id = :id ORDER BY changed_at, id", new MapSqlParameterSource("id", appointmentId), (rs,row) -> {
            Number actor = (Number) rs.getObject("actor_user_id");
            return new AppointmentHistoryEntry(AppointmentStatus.valueOf(rs.getString("status_code")), rs.getString("source"),
                    actor == null ? null : actor.longValue(), rs.getObject("changed_at", LocalDateTime.class), rs.getString("reason"));
        }); }

    private static AppointmentView toView(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AppointmentView(rs.getLong("id"), AppointmentStatus.valueOf(rs.getString("status_code")),
                rs.getString("professional_name"), rs.getString("specialty_name"), rs.getString("site_code"),
                rs.getString("site_name"), rs.getObject("start_at", LocalDateTime.class),
                rs.getObject("end_at", LocalDateTime.class), rs.getInt("duration_minutes"),
                rs.getString("rejection_reason"));
    }

    private static final String SUMMARIES = """
            SELECT a.id, a.status_code, a.site_code, a.start_at, a.end_at, a.duration_minutes,
                   CONCAT(pu.first_names, ' ', pu.last_names) AS patient_name,
                   CONCAT(du.first_names, ' ', du.last_names) AS professional_name,
                   s.name AS specialty_name
            FROM appointments a
            JOIN users pu ON pu.id = a.patient_user_id
            JOIN users du ON du.id = a.professional_id
            JOIN specialties s ON s.id = a.specialty_id
            WHERE a.status_code = :status
            ORDER BY a.start_at, a.id
            """;

    private static final String PATIENT_VIEW_COLUMNS = """
            SELECT a.id, a.status_code, a.site_code, site.name AS site_name, a.start_at, a.end_at,
                   a.duration_minutes, CONCAT(du.first_names, ' ', du.last_names) AS professional_name,
                   s.name AS specialty_name,
                   CASE WHEN a.status_code = 'REJECTED' THEN (
                       SELECT h.reason FROM appointment_status_history h
                       WHERE h.appointment_id = a.id AND h.status_code = 'REJECTED'
                       ORDER BY h.id DESC LIMIT 1
                   ) END AS rejection_reason
            FROM appointments a
            JOIN users du ON du.id = a.professional_id
            JOIN specialties s ON s.id = a.specialty_id
            JOIN sites site ON site.code = a.site_code
            """;

    private static final String PATIENT_VIEWS = PATIENT_VIEW_COLUMNS + """
            WHERE a.patient_user_id = :patientUserId
              AND (:status IS NULL OR a.status_code = :status)
              AND (:from IS NULL OR a.start_at >= :from)
              AND (:to IS NULL OR a.start_at < :to)
            ORDER BY a.start_at, a.id
            """;

    private static final String PATIENT_VIEW_BY_ID = PATIENT_VIEW_COLUMNS + """
            WHERE a.id = :appointmentId AND a.patient_user_id = :patientUserId
            """;

    private static final String PROFESSIONAL_VIEWS = """
            SELECT a.id, a.site_code, a.start_at, a.end_at, a.duration_minutes,
                   CONCAT(pu.first_names, ' ', pu.last_names) AS patient_name, s.name AS specialty_name
            FROM appointments a JOIN users pu ON pu.id = a.patient_user_id JOIN specialties s ON s.id = a.specialty_id
            WHERE a.professional_id = :professionalId AND a.status_code = 'APPROVED'
              AND a.start_at >= :from AND a.start_at < :to AND (:siteCode IS NULL OR a.site_code = :siteCode)
            ORDER BY a.start_at, a.id
            """;
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

    @Column(name = "source", nullable = false, length = 20, updatable = false)
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

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE appointments SET status_code = :status, version = version + 1
            WHERE id = :id AND status_code = :expected
            """, nativeQuery = true)
    int updateStatusIf(Long id, String status, String expected);

    @Modifying
    @Query(value = "DELETE FROM slot_reservations WHERE appointment_id = :appointmentId", nativeQuery = true)
    void deleteSlotReservations(Long appointmentId);
}

interface AppointmentStatusHistoryJpaRepository extends JpaRepository<AppointmentStatusHistoryJpaEntity, Long> {
}
