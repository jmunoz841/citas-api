# Scrum — Sistema de Agendamiento de Citas

Índice del mapa Spec-Driven Development generado con `scrum-spec-orchestrator` a partir de `PRD.md`, `RESTRICCIONES_TECNICAS.md` y `database/REQUISITOS_NORMALIZACION_3FN.md`.

## Objetivo del proyecto

Aplicación web de agendamiento de citas (USER, PROFESSIONAL, ADMIN) en dos sedes fijas (HIC e ICV), con citas generales auto-aprobadas, citas especializadas con aprobación administrativa, reprogramación, auditoría de estados y automatizaciones n8n. Datos 100 % sintéticos.

## Stack

| Capa | Tecnología | Origen |
|---|---|---|
| Backend | Java 21, Spring Boot 3.5.x, Maven, Spring Data JPA, Spring Security + JWT, Flyway | Restricciones técnicas |
| Base de datos | MySQL 8.4 | Restricciones técnicas |
| Arquitectura | Hexagonal (`domain`, `application`, `infrastructure/adapters`) | Restricciones técnicas |
| Frontend | React 19 + Vite 8 + TypeScript + Tailwind CSS 4 | Importado de AI Studio (D-015) |
| Integración | REST JSON directo, sin BFF | Restricciones técnicas |

## Épicas

| Épica | HU | Estado |
|---|---|---|
| [[EP-001-identidad-y-acceso]] | HU-001, HU-002 | En curso (HU-001 Completada) |
| [[EP-002-perfil-y-afiliacion]] | HU-003, HU-004 | Borrador |
| [[EP-003-catalogos]] | HU-005, HU-006, HU-007 | Borrador |
| [[EP-004-gestion-de-profesionales]] | HU-008, HU-009 | Borrador |
| [[EP-005-agenda-del-profesional]] | HU-010, HU-011 | Borrador |
| [[EP-006-reserva-de-citas]] | HU-012, HU-013, HU-014, HU-015, HU-022 | Borrador |
| [[EP-007-ciclo-de-vida-de-citas]] | HU-016 a HU-021 | Borrador |
| [[EP-008-automatizaciones-n8n]] | HU-023, HU-024, HU-025 | Borrador |

## Sprints sugeridos (incrementos funcionales, sin duración)

### Sprint 1 — Identidad base (S2)

- [[HU-001-registro-e-inicio-de-sesion-jwt]] — Alto — **Completada**

Incremento: backend inicializado con registro + login JWT funcional y verificado.

### Sprint 2 — Flujo de reserva (S3)

- [[HU-005-consultar-catalogos-fijos]] — Bajo
- [[HU-006-gestionar-especialidades]] — Medio
- [[HU-008-crear-profesional]] — Alto
- [[HU-009-activar-desactivar-profesional]] — Bajo
- [[HU-010-gestionar-bloques-de-disponibilidad]] — Alto
- [[HU-012-consultar-disponibilidad]] — Alto
- [[HU-013-reservar-cita-general]] — Alto
- [[HU-014-solicitar-cita-especializada]] — Medio
- [[HU-015-resolver-cita-especializada]] — Medio

Incremento: un ADMIN configura profesionales, el profesional publica agenda y el USER reserva citas generales y especializadas.

### Sprint 3 — MVP completo (S4)

- [[HU-002-recuperar-contrasena]] — Medio
- [[HU-003-consultar-y-actualizar-perfil]] — Bajo
- [[HU-007-gestionar-eps-y-planes]] — Medio
- [[HU-004-registrar-afiliacion]] — Medio
- [[HU-011-consultar-agenda-del-profesional]] — Medio
- [[HU-016-consultar-mis-citas]] — Medio
- [[HU-017-cancelar-cita]] — Medio
- [[HU-018-solicitar-reprogramacion]] — Alto
- [[HU-019-resolver-reprogramacion]] — Alto
- [[HU-020-cerrar-atencion]] — Bajo
- [[HU-021-historial-de-estados]] — Medio
- [[HU-022-bandeja-administrativa]] — Medio

Incremento: ciclo de vida completo de la cita y todas las pantallas obligatorias del PRD.

### Sprint 4 — Recordatorios n8n (S5)

- [[HU-023-recordatorios-de-citas]] — Medio

### Sprint 5 — Notificaciones y cierre (S6)

- [[HU-024-notificacion-cambio-de-estado]] — Medio
- [[HU-025-resumen-operativo-diario]] — Medio (opcional)

## Decisiones pendientes

1. ~~Aprobación de HU-001~~ — Aprobada (2026-09-16).
2. ~~Refresh token~~ — Rotación en cada refresh y almacenamiento como hash (aprobado).
3. ~~Política de contraseña~~ — Mínimo 8 caracteres, al menos una letra y un número (aprobado).
4. ~~Slots y doble reserva~~ — Resuelto en el diseño 3FN (D-013).
5. ~~Framework frontend~~ — React + Vite + TypeScript (D-015).

## Trazabilidad PRD → HU

| RF | HU |
|---|---|
| RF-01, RF-02 | HU-001 |
| RF-03 | HU-002 |
| RF-04 | HU-003, HU-004 |
| RF-05 | HU-005 |
| RF-06 | HU-006, HU-007 |
| RF-07 | HU-008, HU-009 |
| RF-08 | HU-010 |
| RF-09 | HU-006, HU-012 |
| RF-10 | HU-012 |
| RF-11 | HU-013 |
| RF-12 | HU-014, HU-015 |
| RF-13 | HU-016 |
| RF-14 | HU-017 |
| RF-15 | HU-018, HU-019 |
| RF-16 | HU-011 |
| RF-17 | HU-020 |
| RF-18 | HU-015, HU-022 |
| RF-19 | HU-021 |
| RF-20 | Transversal (contrato REST en cada HU) |
| §10 n8n | HU-023, HU-024, HU-025 |
