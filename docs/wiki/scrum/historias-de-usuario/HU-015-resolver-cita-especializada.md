---
id: HU-015
tipo: historia-de-usuario
titulo: "Aprobar o rechazar cita especializada"
estado: Borrador
epica: "[[EP-006-reserva-de-citas]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-014-solicitar-cita-especializada]]"
relacionadas:
  - "[[HU-022-bandeja-administrativa]]"
  - "[[HU-021-historial-de-estados]]"
---

# HU-015 — Aprobar o rechazar cita especializada

## Historia de usuario

**COMO** ADMIN  
**QUIERO** ver las citas especializadas solicitadas y aprobarlas o rechazarlas con motivo  
**PARA** controlar la asignación de atención especializada

> Como ADMIN, quiero ver las citas especializadas solicitadas y aprobarlas o rechazarlas con motivo para controlar la asignación de atención especializada.

## Contexto y descripción

Implementa la resolución de RF-12. Incluye un listado básico de solicitudes `REQUESTED` y el dashboard ADMIN; los filtros completos quedan en [[HU-022-bandeja-administrativa]].

## Alcance

- Listado de citas `REQUESTED`.
- Aprobar → `APPROVED`.
- Rechazar con motivo → `REJECTED` y liberación de slots.
- Dashboard ADMIN y vista "aprobar/rechazar citas".

## Fuera de alcance

- Filtros avanzados y reprogramaciones ([[HU-022-bandeja-administrativa]]).

## Reglas de negocio

- Solo ADMIN resuelve solicitudes.
- Solo citas en `REQUESTED` pueden aprobarse o rechazarse (RN-11).
- El rechazo exige motivo (RN-04).
- Rechazar libera los slots (RN-09).
- Cada transición registra historial con fuente `ADMIN` y actor.

## Dependencias y relaciones

- Épica: [[EP-006-reserva-de-citas]]
- Dependencias: [[HU-014-solicitar-cita-especializada]]
- Relacionadas: [[HU-022-bandeja-administrativa]], [[HU-021-historial-de-estados]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** transiciones de estado con liberación de slots e historial, protegidas por rol.

## Tareas de desarrollo

- [ ] **T-01 — Casos de uso aprobar/rechazar**  
  Dificultad: Medio  
  Descripción: validación de estado, motivo, liberación de slots e historial.
- [ ] **T-02 — Listado de solicitudes**  
  Dificultad: Bajo  
  Descripción: consulta de citas `REQUESTED` para ADMIN.
- [ ] **T-03 — Dashboard ADMIN y vista de aprobación**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [ ] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: aprobación, rechazo sin motivo, estado inválido, liberación y autorización.

## Criterios de aceptación

### CA-01 — Aprobar

**Dado** una cita `REQUESTED`  
**Cuando** ADMIN la aprueba  
**Entonces** pasa a `APPROVED` con historial registrado

### CA-02 — Rechazar con motivo

**Dado** una cita `REQUESTED`  
**Cuando** ADMIN la rechaza con motivo  
**Entonces** pasa a `REJECTED`, guarda el motivo y sus slots vuelven a estar disponibles

### CA-03 — Rechazo sin motivo

**Dado** una cita `REQUESTED`  
**Cuando** ADMIN la rechaza sin motivo  
**Entonces** la operación es rechazada con error de validación

### CA-04 — Estado inválido

**Dado** una cita que no está en `REQUESTED`  
**Cuando** se intenta aprobar o rechazar  
**Entonces** la transición es rechazada

### CA-05 — Autorización

**Dado** un USER o PROFESSIONAL  
**Cuando** intenta resolver una solicitud  
**Entonces** recibe acceso denegado

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de transiciones y autorización en verde.
- [ ] Dashboard ADMIN y vista de aprobación integrados en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| CA-04 | Pendiente | — | — |
| CA-05 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

## Notas y decisiones

- Ninguna.
