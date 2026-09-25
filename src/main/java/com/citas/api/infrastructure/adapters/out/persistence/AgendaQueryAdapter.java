package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.AgendaQueryPort;
import com.citas.api.domain.model.agenda.AgendaSlot;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Lecturas de agenda con SQL explícito (HU-012). Son proyecciones de solo lectura que cruzan
 * cinco tablas; con JDBC la consulta y el mapeo de tipos quedan a la vista. Se ejecutan en la
 * misma conexión y transacción que JPA.
 */
@Component
class AgendaQueryAdapter implements AgendaQueryPort {

    private static final String OFFERS = """
            SELECT p.user_id AS professional_id,
                   CONCAT(u.first_names, ' ', u.last_names) AS professional_name,
                   s.id AS specialty_id, s.name AS specialty_name,
                   s.duration_minutes, s.is_general
            FROM professionals p
            JOIN users u ON u.id = p.user_id
            JOIN professional_specialties ps ON ps.professional_id = p.user_id
            JOIN specialties s ON s.id = ps.specialty_id
            WHERE p.is_active = TRUE AND ps.is_active = TRUE AND s.is_active = TRUE
              AND (:specialtyId IS NULL OR s.id = :specialtyId)
              AND (:general IS NULL OR s.is_general = :general)
              AND (:professionalId IS NULL OR p.user_id = :professionalId)
            """;

    private static final String SLOTS = """
            SELECT sl.id, sl.block_id, sl.professional_id, b.site_code, sl.start_at,
                   (sr.slot_id IS NOT NULL) AS taken
            FROM availability_slots sl
            JOIN availability_blocks b ON b.id = sl.block_id
            LEFT JOIN slot_reservations sr ON sr.slot_id = sl.id
            WHERE sl.professional_id IN (:professionalIds)
              AND sl.start_at >= :from AND sl.start_at < :to
              AND (:siteCode IS NULL OR b.site_code = :siteCode)
            ORDER BY sl.professional_id, sl.start_at
            """;

    private final NamedParameterJdbcTemplate jdbc;

    AgendaQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProfessionalOffer> findOffers(Long specialtyId, Boolean general, Long professionalId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("specialtyId", specialtyId)
                .addValue("general", general)
                .addValue("professionalId", professionalId);
        return jdbc.query(OFFERS, params, (rs, row) -> new ProfessionalOffer(
                rs.getLong("professional_id"), rs.getString("professional_name"),
                rs.getLong("specialty_id"), rs.getString("specialty_name"),
                rs.getInt("duration_minutes"), rs.getBoolean("is_general")));
    }

    @Override
    public List<AgendaSlot> findSlots(Collection<Long> professionalIds, LocalDateTime from, LocalDateTime to,
                                      String siteCode) {
        if (professionalIds.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("professionalIds", professionalIds)
                .addValue("from", from)
                .addValue("to", to)
                .addValue("siteCode", siteCode);
        return jdbc.query(SLOTS, params, (rs, row) -> new AgendaSlot(
                rs.getLong("id"), rs.getLong("block_id"), rs.getLong("professional_id"),
                rs.getString("site_code"), rs.getObject("start_at", LocalDateTime.class),
                rs.getBoolean("taken")));
    }
}
