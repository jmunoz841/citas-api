package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.SpecialtyRepositoryPort;
import com.citas.api.domain.model.professional.Specialty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Especialidades (V4). El nombre se compara sin distinguir mayúsculas porque la columna usa
 * collation `utf8mb4_0900_ai_ci`.
 */
@Component
class SpecialtyPersistenceAdapter implements SpecialtyRepositoryPort {

    private final SpecialtyJpaRepository repository;

    SpecialtyPersistenceAdapter(SpecialtyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Specialty save(Specialty specialty) {
        SpecialtyJpaEntity entity = new SpecialtyJpaEntity(specialty.getId(), specialty.getName(),
                (short) specialty.getDurationMinutes(), specialty.isGeneral(), specialty.isActive());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Specialty> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id).map(SpecialtyPersistenceAdapter::toDomain);
    }

    @Override
    public List<Specialty> findAll(boolean onlyActive) {
        List<SpecialtyJpaEntity> rows = onlyActive
                ? repository.findByActiveTrueOrderByNameAsc()
                : repository.findAllByOrderByNameAsc();
        return rows.stream().map(SpecialtyPersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByNameIgnoringId(String name, Long ignoredId) {
        return repository.existsByName(name, ignoredId);
    }

    private static Specialty toDomain(SpecialtyJpaEntity entity) {
        return Specialty.restore(entity.getId(), entity.getName(), entity.getDurationMinutes().intValue(),
                entity.isGeneral(), entity.isActive());
    }
}

@Entity
@Table(name = "specialties")
class SpecialtyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    @Column(name = "is_general", nullable = false)
    private boolean general;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected SpecialtyJpaEntity() {
    }

    SpecialtyJpaEntity(Long id, String name, Short durationMinutes, boolean general, boolean active) {
        this.id = id;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.general = general;
        this.active = active;
    }

    Long getId() { return id; }
    String getName() { return name; }
    Short getDurationMinutes() { return durationMinutes; }
    boolean isGeneral() { return general; }
    boolean isActive() { return active; }
}

interface SpecialtyJpaRepository extends JpaRepository<SpecialtyJpaEntity, Long> {

    List<SpecialtyJpaEntity> findAllByOrderByNameAsc();

    List<SpecialtyJpaEntity> findByActiveTrueOrderByNameAsc();

    @Query("""
            SELECT COUNT(s) > 0 FROM SpecialtyJpaEntity s
            WHERE s.name = :name AND (:ignoredId IS NULL OR s.id <> :ignoredId)
            """)
    boolean existsByName(String name, Long ignoredId);
}
