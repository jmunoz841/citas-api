package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.IntegrationQueryPort;
import com.citas.api.domain.model.integration.DailySummary;
import com.citas.api.domain.model.integration.DailySummary.SiteCount;
import com.citas.api.domain.model.integration.DailySummary.SpecialtyCount;
import com.citas.api.domain.model.integration.NotificationContext;
import com.citas.api.domain.model.integration.ReminderCandidate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lecturas para n8n con SQL explícito (HU-023, HU-024, HU-025). Proyecciones de solo lectura más el
 * registro idempotente de recordatorios (V10).
 */
@Component
class IntegrationQueryAdapter implements IntegrationQueryPort {

    private static final List<String> STATUSES =
            List.of("REQUESTED", "APPROVED", "REJECTED", "CANCELLED", "COMPLETED", "NO_SHOW");

    private final NamedParameterJdbcTemplate jdbc;

    IntegrationQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ReminderCandidate> findReminderCandidates(LocalDateTime from, LocalDateTime to) {
        return jdbc.query(REMINDERS, new MapSqlParameterSource().addValue("from", from).addValue("to", to),
                (rs, row) -> new ReminderCandidate(rs.getLong("id"), rs.getString("patient_first_names"),
                        rs.getString("patient_email"), rs.getString("professional_name"),
                        rs.getString("specialty_name"), rs.getString("site_code"), rs.getString("site_name"),
                        rs.getString("site_address"), rs.getObject("start_at", LocalDateTime.class),
                        rs.getObject("end_at", LocalDateTime.class)));
    }

    /** {@code INSERT IGNORE}: un registro repetido no falla y devuelve 0 filas (idempotente). */
    @Override
    public boolean recordReminderSent(Long appointmentId, LocalDateTime startAt, LocalDateTime sentAt) {
        return jdbc.update("""
                INSERT IGNORE INTO appointment_reminders (appointment_id, start_at, sent_at)
                VALUES (:appointmentId, :startAt, :sentAt)
                """, new MapSqlParameterSource()
                .addValue("appointmentId", appointmentId)
                .addValue("startAt", startAt)
                .addValue("sentAt", sentAt)) == 1;
    }

    @Override
    public DailySummary dailySummary(LocalDate date, LocalDateTime now) {
        MapSqlParameterSource day = new MapSqlParameterSource()
                .addValue("from", date.atStartOfDay())
                .addValue("to", date.plusDays(1).atStartOfDay());

        // Todas las sedes aparecen, aunque no tengan citas ese día.
        Map<String, String> siteNames = new LinkedHashMap<>();
        jdbc.query("SELECT code, name FROM sites ORDER BY code", rs -> {
            siteNames.put(rs.getString("code"), rs.getString("name"));
        });
        Map<String, Map<String, Long>> bySite = new LinkedHashMap<>();
        siteNames.keySet().forEach(code -> bySite.put(code, emptyStatusCounts()));
        Map<String, Long> byStatus = emptyStatusCounts();
        jdbc.query("""
                SELECT site_code, status_code, COUNT(*) AS total FROM appointments
                WHERE start_at >= :from AND start_at < :to
                GROUP BY site_code, status_code
                """, day, rs -> {
            long total = rs.getLong("total");
            String status = rs.getString("status_code");
            bySite.computeIfAbsent(rs.getString("site_code"), code -> emptyStatusCounts()).merge(status, total, Long::sum);
            byStatus.merge(status, total, Long::sum);
        });

        List<SiteCount> sites = new ArrayList<>();
        bySite.forEach((code, counts) -> sites.add(new SiteCount(code, siteNames.getOrDefault(code, code),
                counts.values().stream().mapToLong(Long::longValue).sum(), counts)));
        List<SpecialtyCount> specialties = jdbc.query("""
                SELECT s.name, COUNT(*) AS total FROM appointments a JOIN specialties s ON s.id = a.specialty_id
                WHERE a.start_at >= :from AND a.start_at < :to
                GROUP BY s.name ORDER BY total DESC, s.name
                """, day, (rs, row) -> new SpecialtyCount(rs.getString("name"), rs.getLong("total")));

        Long pendingRequests = jdbc.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE status_code = 'REQUESTED' AND start_at > :now",
                new MapSqlParameterSource("now", now), Long.class);
        Long pendingReschedules = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reschedule_requests WHERE status_code = 'PENDING'",
                new MapSqlParameterSource(), Long.class);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new DailySummary(date, total, byStatus, sites, specialties,
                pendingRequests == null ? 0 : pendingRequests, pendingReschedules == null ? 0 : pendingReschedules);
    }

    @Override
    public Optional<NotificationContext> findNotificationContext(Long appointmentId) {
        return jdbc.query(NOTIFICATION_CONTEXT, new MapSqlParameterSource("id", appointmentId),
                (rs, row) -> new NotificationContext(rs.getLong("id"), rs.getString("status_code"),
                        rs.getString("patient_first_names"), rs.getString("patient_email"),
                        rs.getString("specialty_name"), rs.getString("professional_name"),
                        rs.getString("site_code"), rs.getString("site_name"),
                        rs.getObject("start_at", LocalDateTime.class), rs.getObject("end_at", LocalDateTime.class)))
                .stream().findFirst();
    }

    private static Map<String, Long> emptyStatusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        STATUSES.forEach(status -> counts.put(status, 0L));
        return counts;
    }

    private static final String APPOINTMENT_COLUMNS = """
            SELECT a.id, a.status_code, a.site_code, a.start_at, a.end_at,
                   pu.first_names AS patient_first_names, pu.email AS patient_email,
                   CONCAT(du.first_names, ' ', du.last_names) AS professional_name,
                   s.name AS specialty_name, site.name AS site_name, site.address AS site_address
            FROM appointments a
            JOIN users pu ON pu.id = a.patient_user_id
            JOIN users du ON du.id = a.professional_id
            JOIN specialties s ON s.id = a.specialty_id
            JOIN sites site ON site.code = a.site_code
            """;

    private static final String REMINDERS = APPOINTMENT_COLUMNS + """
            WHERE a.status_code = 'APPROVED' AND a.start_at > :from AND a.start_at <= :to
              AND NOT EXISTS (SELECT 1 FROM appointment_reminders r
                              WHERE r.appointment_id = a.id AND r.start_at = a.start_at)
            ORDER BY a.start_at, a.id
            """;

    private static final String NOTIFICATION_CONTEXT = APPOINTMENT_COLUMNS + " WHERE a.id = :id";
}
