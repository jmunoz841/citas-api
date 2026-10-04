package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.RescheduleRepositoryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.appointment.InboxFilter;
import com.citas.api.domain.model.appointment.RescheduleRequest;
import com.citas.api.domain.model.appointment.RescheduleStatus;
import com.citas.api.domain.model.appointment.RescheduleSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reprogramaciones y su franja retenida (V9).
 *
 * <p>Como en la reserva, la retención se inserta con SQL y la decide la clave primaria de
 * {@code slot_reservations}; las transiciones son UPDATE condicionados a {@code PENDING}, así que
 * dos decisiones simultáneas no pueden aplicarse ambas.</p>
 */
@Component
class ReschedulePersistenceAdapter implements RescheduleRepositoryPort {

    private final RescheduleRequestJpaRepository requests;
    private final NamedParameterJdbcTemplate jdbc;

    ReschedulePersistenceAdapter(RescheduleRequestJpaRepository requests, NamedParameterJdbcTemplate jdbc) {
        this.requests = requests;
        this.jdbc = jdbc;
    }

    @Override
    public RescheduleRequest save(RescheduleRequest request) {
        try {
            RescheduleRequestJpaEntity saved = requests.saveAndFlush(new RescheduleRequestJpaEntity(
                    request.appointmentId(), request.originalStartAt(), request.originalSiteCode(),
                    request.requestedStartAt(), request.requestedSiteCode(), request.status().name()));
            return request.withId(saved.getId());
        } catch (DataIntegrityViolationException e) {
            // Dos solicitudes simultáneas sobre la misma cita: el índice funcional deja pasar una.
            if (causeMentions(e, "uk_rr_one_pending_per_appointment")) {
                throw BusinessConflictException.reschedulePending();
            }
            throw e;
        }
    }

    @Override
    public void holdSlots(RescheduleRequest request, Long professionalId, List<AgendaSlot> slots) {
        // Orden fijo por id, como en la reserva: evita interbloqueos entre retenciones que compiten.
        List<AgendaSlot> ordered = slots.stream().sorted(Comparator.comparing(AgendaSlot::slotId)).toList();
        try {
            for (AgendaSlot slot : ordered) {
                jdbc.update("""
                        INSERT INTO slot_reservations (slot_id, professional_id, reschedule_request_id)
                        VALUES (:slotId, :professionalId, :requestId)
                        """, new MapSqlParameterSource()
                        .addValue("slotId", slot.slotId())
                        .addValue("professionalId", professionalId)
                        .addValue("requestId", request.id()));
            }
        } catch (DataIntegrityViolationException e) {
            // 1062 sobre la PK: el slot ya lo ocupa una cita o lo retiene otra solicitud (HU-018 CA-03).
            if (causeMentions(e, "slot_reservations.primary")) {
                throw BusinessConflictException.slotUnavailable();
            }
            throw e;
        }
    }

    @Override
    public Optional<RescheduleRequest> findById(Long requestId) {
        return jdbc.query(SELECT_REQUEST + " WHERE id = :id", new MapSqlParameterSource("id", requestId),
                REQUEST_MAPPER).stream().findFirst();
    }

    @Override
    public Optional<RescheduleRequest> findPendingByAppointment(Long appointmentId) {
        return jdbc.query(SELECT_REQUEST + " WHERE appointment_id = :appointmentId AND status_code = 'PENDING'",
                new MapSqlParameterSource("appointmentId", appointmentId), REQUEST_MAPPER).stream().findFirst();
    }

    @Override
    public void decide(RescheduleRequest decided) {
        int rows = jdbc.update("""
                UPDATE reschedule_requests
                SET status_code = :status, decided_by_user_id = :decidedBy, decided_at = :decidedAt,
                    decision_reason = :reason, version = version + 1
                WHERE id = :id AND status_code = 'PENDING'
                """, new MapSqlParameterSource()
                .addValue("status", decided.status().name())
                .addValue("decidedBy", decided.decidedByUserId())
                .addValue("decidedAt", decided.decidedAt())
                .addValue("reason", decided.decisionReason())
                .addValue("id", decided.id()));
        if (rows == 0) {
            throw BusinessConflictException.rescheduleNotPending();
        }
    }

    @Override
    public void releaseHeldSlots(Long requestId) {
        jdbc.update("DELETE FROM slot_reservations WHERE reschedule_request_id = :requestId",
                new MapSqlParameterSource("requestId", requestId));
    }

    @Override
    public void transferHeldSlots(Long requestId, Long appointmentId) {
        jdbc.update("""
                UPDATE slot_reservations SET appointment_id = :appointmentId, reschedule_request_id = NULL
                WHERE reschedule_request_id = :requestId
                """, new MapSqlParameterSource()
                .addValue("appointmentId", appointmentId)
                .addValue("requestId", requestId));
    }

    @Override
    public List<RescheduleSummary> findPending(InboxFilter filter) {
        return jdbc.query(PENDING_SUMMARIES, InboxParameters.of(filter), (rs, row) -> new RescheduleSummary(
                rs.getLong("id"), rs.getLong("appointment_id"), RescheduleStatus.valueOf(rs.getString("status_code")),
                rs.getString("patient_name"), rs.getString("professional_name"), rs.getString("specialty_name"),
                rs.getInt("duration_minutes"), rs.getObject("original_start_at", LocalDateTime.class),
                rs.getString("original_site_code"), rs.getObject("requested_start_at", LocalDateTime.class),
                rs.getString("requested_site_code"), rs.getObject("created_at", LocalDateTime.class)));
    }

    @Override
    public List<RescheduleRequest> findPendingExpiredAt(LocalDateTime now) {
        return jdbc.query(SELECT_REQUEST + """
                 WHERE status_code = 'PENDING' AND (original_start_at <= :now OR requested_start_at <= :now)
                 ORDER BY id
                """, new MapSqlParameterSource("now", now), REQUEST_MAPPER);
    }

    private static boolean causeMentions(DataIntegrityViolationException e, String constraint) {
        return String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT).contains(constraint);
    }

    private static final String SELECT_REQUEST = """
            SELECT id, appointment_id, original_start_at, original_site_code, requested_start_at,
                   requested_site_code, status_code, decided_by_user_id, decided_at, decision_reason
            FROM reschedule_requests
            """;

    private static final RowMapper<RescheduleRequest> REQUEST_MAPPER = (rs, row) -> {
        Number decidedBy = (Number) rs.getObject("decided_by_user_id");
        return new RescheduleRequest(rs.getLong("id"), rs.getLong("appointment_id"),
                rs.getObject("original_start_at", LocalDateTime.class), rs.getString("original_site_code"),
                rs.getObject("requested_start_at", LocalDateTime.class), rs.getString("requested_site_code"),
                RescheduleStatus.valueOf(rs.getString("status_code")),
                decidedBy == null ? null : decidedBy.longValue(), rs.getObject("decided_at", LocalDateTime.class),
                rs.getString("decision_reason"));
    };

    /** Los filtros de sede y fecha se aplican a la franja solicitada, que es la que se decide (HU-022). */
    private static final String PENDING_SUMMARIES = """
            SELECT rr.id, rr.appointment_id, rr.status_code, rr.original_start_at, rr.original_site_code,
                   rr.requested_start_at, rr.requested_site_code, rr.created_at, a.duration_minutes,
                   CONCAT(pu.first_names, ' ', pu.last_names) AS patient_name,
                   CONCAT(du.first_names, ' ', du.last_names) AS professional_name,
                   s.name AS specialty_name
            FROM reschedule_requests rr
            JOIN appointments a ON a.id = rr.appointment_id
            JOIN users pu ON pu.id = a.patient_user_id
            JOIN users du ON du.id = a.professional_id
            JOIN specialties s ON s.id = a.specialty_id
            WHERE rr.status_code = 'PENDING'
              AND (:siteCode IS NULL OR rr.requested_site_code = :siteCode)
              AND (:professionalId IS NULL OR a.professional_id = :professionalId)
              AND (:specialtyId IS NULL OR a.specialty_id = :specialtyId)
              AND (:from IS NULL OR rr.requested_start_at >= :from)
              AND (:to IS NULL OR rr.requested_start_at < :to)
            ORDER BY rr.requested_start_at, rr.id
            """;
}

@Entity
@Table(name = "reschedule_requests")
class RescheduleRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private Long appointmentId;

    @Column(name = "original_start_at", nullable = false, updatable = false)
    private LocalDateTime originalStartAt;

    @Column(name = "original_site_code", nullable = false, length = 10, updatable = false)
    private String originalSiteCode;

    @Column(name = "requested_start_at", nullable = false, updatable = false)
    private LocalDateTime requestedStartAt;

    @Column(name = "requested_site_code", nullable = false, length = 10, updatable = false)
    private String requestedSiteCode;

    /** Las transiciones se escriben con UPDATE condicionado en el adaptador, no con la entidad. */
    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    protected RescheduleRequestJpaEntity() {
    }

    RescheduleRequestJpaEntity(Long appointmentId, LocalDateTime originalStartAt, String originalSiteCode,
                               LocalDateTime requestedStartAt, String requestedSiteCode, String statusCode) {
        this.appointmentId = appointmentId;
        this.originalStartAt = originalStartAt;
        this.originalSiteCode = originalSiteCode;
        this.requestedStartAt = requestedStartAt;
        this.requestedSiteCode = requestedSiteCode;
        this.statusCode = statusCode;
    }

    Long getId() { return id; }
}

interface RescheduleRequestJpaRepository extends JpaRepository<RescheduleRequestJpaEntity, Long> {
}
