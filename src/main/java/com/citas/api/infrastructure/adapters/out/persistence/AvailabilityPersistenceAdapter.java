package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.AvailabilityRepositoryPort;
import com.citas.api.domain.model.agenda.AvailabilityBlock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Bloques y slots (V5). Guardar un bloque **regenera** sus slots: se borran los actuales y se
 * insertan los del nuevo horario, siempre dentro de la transacción del caso de uso.
 */
@Component
class AvailabilityPersistenceAdapter implements AvailabilityRepositoryPort {

    private final AvailabilityBlockJpaRepository blocks;
    private final AvailabilitySlotJpaRepository slots;

    AvailabilityPersistenceAdapter(AvailabilityBlockJpaRepository blocks, AvailabilitySlotJpaRepository slots) {
        this.blocks = blocks;
        this.slots = slots;
    }

    @Override
    public AvailabilityBlock save(AvailabilityBlock block) {
        AvailabilityBlockJpaEntity entity = blocks.save(new AvailabilityBlockJpaEntity(block.getId(),
                block.getProfessionalId(), block.getSiteCode(), block.getStartAt(), block.getEndAt()));
        AvailabilityBlock saved = block.withId(entity.getId());

        if (block.getId() != null) {
            slots.deleteByBlockId(block.getId());
            slots.flush();
        }
        for (LocalDateTime start : saved.slotStarts()) {
            slots.save(new AvailabilitySlotJpaEntity(saved.getId(), saved.getProfessionalId(), start));
        }
        return saved;
    }

    @Override
    public Optional<AvailabilityBlock> findById(Long blockId) {
        return blockId == null ? Optional.empty()
                : blocks.findById(blockId).map(AvailabilityPersistenceAdapter::toDomain);
    }

    @Override
    public List<AvailabilityBlock> findByProfessional(Long professionalId, LocalDate date, String siteCode) {
        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime to = date == null ? null : date.plusDays(1).atStartOfDay();
        return blocks.search(professionalId, from, to, siteCode).stream()
                .map(AvailabilityPersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean overlaps(Long professionalId, AvailabilityBlock candidate, Long excludedBlockId) {
        return blocks.overlaps(professionalId, candidate.getStartAt(), candidate.getEndAt(), excludedBlockId);
    }

    @Override
    public void deleteById(Long blockId) {
        // Los slots caen por ON DELETE CASCADE de la FK compuesta.
        blocks.deleteById(blockId);
    }

    @Override
    public int countSlots(Long blockId) {
        return slots.countByBlockId(blockId);
    }

    private static AvailabilityBlock toDomain(AvailabilityBlockJpaEntity entity) {
        return AvailabilityBlock.restore(entity.getId(), entity.getProfessionalId(), entity.getSiteCode(),
                entity.getStartAt(), entity.getEndAt());
    }
}

@Entity
@Table(name = "availability_blocks")
class AvailabilityBlockJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "site_code", nullable = false, length = 10)
    private String siteCode;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    protected AvailabilityBlockJpaEntity() {
    }

    AvailabilityBlockJpaEntity(Long id, Long professionalId, String siteCode, LocalDateTime startAt,
                               LocalDateTime endAt) {
        this.id = id;
        this.professionalId = professionalId;
        this.siteCode = siteCode;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    Long getId() { return id; }
    Long getProfessionalId() { return professionalId; }
    String getSiteCode() { return siteCode; }
    LocalDateTime getStartAt() { return startAt; }
    LocalDateTime getEndAt() { return endAt; }
}

@Entity
@Table(name = "availability_slots")
class AvailabilitySlotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_id", nullable = false)
    private Long blockId;

    /** Redundancia controlada: sostiene las restricciones compuestas del diseño 3FN. */
    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    protected AvailabilitySlotJpaEntity() {
    }

    AvailabilitySlotJpaEntity(Long blockId, Long professionalId, LocalDateTime startAt) {
        this.blockId = blockId;
        this.professionalId = professionalId;
        this.startAt = startAt;
    }

    Long getId() { return id; }
    Long getBlockId() { return blockId; }
    LocalDateTime getStartAt() { return startAt; }
}

interface AvailabilityBlockJpaRepository extends JpaRepository<AvailabilityBlockJpaEntity, Long> {

    @Query("""
            SELECT b FROM AvailabilityBlockJpaEntity b
            WHERE b.professionalId = :professionalId
              AND (:from IS NULL OR (b.startAt >= :from AND b.startAt < :to))
              AND (:siteCode IS NULL OR b.siteCode = :siteCode)
            ORDER BY b.startAt
            """)
    List<AvailabilityBlockJpaEntity> search(Long professionalId, LocalDateTime from, LocalDateTime to,
                                            String siteCode);

    @Query("""
            SELECT COUNT(b) > 0 FROM AvailabilityBlockJpaEntity b
            WHERE b.professionalId = :professionalId
              AND b.startAt < :endAt AND :startAt < b.endAt
              AND (:excludedBlockId IS NULL OR b.id <> :excludedBlockId)
            """)
    boolean overlaps(Long professionalId, LocalDateTime startAt, LocalDateTime endAt, Long excludedBlockId);
}

interface AvailabilitySlotJpaRepository extends JpaRepository<AvailabilitySlotJpaEntity, Long> {

    void deleteByBlockId(Long blockId);

    int countByBlockId(Long blockId);
}
