package com.citas.api.infrastructure.adapters.out.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

/**
 * Entidades de los catálogos fijos (V1 y V2). Son tablas de referencia de dos o tres columnas
 * con clave primaria natural {@code code}, así que se agrupan aquí en vez de dispersarlas en
 * seis archivos de doce líneas. Ninguna sale del paquete de persistencia.
 */
final class CatalogJpaEntities {

    private CatalogJpaEntities() {
    }
}

/** Base de los catálogos de código y nombre. */
@MappedSuperclass
abstract class CodeNameJpaEntity {

    @Id
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    protected CodeNameJpaEntity() {
    }

    String getCode() {
        return code;
    }

    String getName() {
        return name;
    }
}

/** Base de los catálogos de estados, que además marcan si el estado es terminal. */
@MappedSuperclass
abstract class StatusJpaEntity extends CodeNameJpaEntity {

    @Column(name = "is_terminal", nullable = false)
    private boolean terminal;

    protected StatusJpaEntity() {
    }

    boolean isTerminal() {
        return terminal;
    }
}

@Entity
@Table(name = "roles")
class RoleJpaEntity extends CodeNameJpaEntity {
}

/** El código de tipo de documento es más corto que el del resto de catálogos (V1: VARCHAR(10)). */
@Entity
@Table(name = "document_types")
@AttributeOverride(name = "code", column = @Column(name = "code", nullable = false, length = 10))
class DocumentTypeJpaEntity extends CodeNameJpaEntity {
}

@Entity
@Table(name = "regimes")
class RegimeJpaEntity extends CodeNameJpaEntity {
}

@Entity
@Table(name = "appointment_statuses")
class AppointmentStatusJpaEntity extends StatusJpaEntity {
}

@Entity
@Table(name = "reschedule_statuses")
class RescheduleStatusJpaEntity extends StatusJpaEntity {
}

/** Sedes: añaden dirección y usan códigos y nombres más largos que el resto. */
@Entity
@Table(name = "sites")
class SiteJpaEntity {

    @Id
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    protected SiteJpaEntity() {
    }

    String getCode() {
        return code;
    }

    String getName() {
        return name;
    }

    String getAddress() {
        return address;
    }
}
