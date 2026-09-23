package com.citas.api.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorios de los catálogos fijos. Solo lectura: los valores los siembra Flyway y la API
 * no los modifica (HU-005, CA-03). Se ordena por código para que la respuesta sea estable.
 */
final class CatalogJpaRepositories {

    private CatalogJpaRepositories() {
    }
}

interface SiteJpaRepository extends JpaRepository<SiteJpaEntity, String> {

    List<SiteJpaEntity> findAllByOrderByCodeAsc();
}

interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

    List<RoleJpaEntity> findAllByOrderByCodeAsc();
}

interface DocumentTypeJpaRepository extends JpaRepository<DocumentTypeJpaEntity, String> {

    List<DocumentTypeJpaEntity> findAllByOrderByCodeAsc();
}

interface RegimeJpaRepository extends JpaRepository<RegimeJpaEntity, String> {

    List<RegimeJpaEntity> findAllByOrderByCodeAsc();
}

interface AppointmentStatusJpaRepository extends JpaRepository<AppointmentStatusJpaEntity, String> {

    List<AppointmentStatusJpaEntity> findAllByOrderByCodeAsc();
}

interface RescheduleStatusJpaRepository extends JpaRepository<RescheduleStatusJpaEntity, String> {

    List<RescheduleStatusJpaEntity> findAllByOrderByCodeAsc();
}
