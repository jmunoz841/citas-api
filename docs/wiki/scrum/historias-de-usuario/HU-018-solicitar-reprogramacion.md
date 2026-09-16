---
id: HU-018
tipo: historia-de-usuario
titulo: "Solicitar reprogramación"
estado: Borrador
epica: "[[EP-007-ciclo-de-vida-de-citas]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-016-consultar-mis-citas]]"
  - "[[HU-012-consultar-disponibilidad]]"
relacionadas:
  - "[[HU-019-resolver-reprogramacion]]"
---

# HU-018 — Solicitar reprogramación

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** solicitar una nueva fecha/hora para una cita aprobada futura  
**PARA** cambiar el horario sin perder mi cita actual mientras se decide

> Como USER autenticado, quiero solicitar una nueva fecha/hora para una cita aprobada futura para cambiar el horario sin perder mi cita actual mientras se decide.

## Contexto y descripción

Implementa la solicitud de RF-15. La reprogramación conserva profesional y especialidad.

## Alcance

- Solicitud sobre cita `APPROVED` futura propia.
- Selección de nueva franja disponible del mismo profesional y especialidad.
- Solicitud en `PENDING` con retención de la nueva franja.
- Vista "solicitar reprogramación".

## Fuera de alcance

- Resolución por ADMIN ([[HU-019-resolver-reprogramacion]]).
- Cambio de profesional (se trata como nueva cita).

## Reglas de negocio

- Solo citas `APPROVED` y futuras (RF-15).
- Se conservan profesional y especialidad.
- La nueva franja se retiene mientras la solicitud está `PENDING`.
- La cita original conserva su franja hasta la decisión (RN-10).
- No puede existir más de una reprogramación `PENDING` por cita (supuesto).
- La nueva franja cumple duración y slots consecutivos (RN-05).

## Dependencias y relaciones

- Épica: [[EP-007-ciclo-de-vida-de-citas]]
- Dependencias: [[HU-016-consultar-mis-citas]], [[HU-012-consultar-disponibilidad]]
- Relacionadas: [[HU-019-resolver-reprogramacion]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** coexistencia de dos reservas para una misma cita y reglas de retención/concurrencia.

## Tareas de desarrollo

- [ ] **T-01 — Migración de reprogramaciones**  
  Dificultad: Medio  
  Descripción: solicitud vinculada a la cita, estado y franja retenida.
- [ ] **T-02 — Caso de uso solicitar reprogramación**  
  Dificultad: Alto  
  Descripción: validaciones y retención transaccional de nueva franja.
- [ ] **T-03 — Vista de reprogramación**  
  Dificultad: Medio  
  Descripción: disponibilidad restringida al mismo profesional/especialidad.
- [ ] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: estado inválido, franja ocupada, conservación de la franja original.

## Criterios de aceptación

### CA-01 — Solicitud válida

**Dado** una cita propia `APPROVED` futura  
**Cuando** el USER elige una franja libre del mismo profesional y especialidad  
**Entonces** se crea una reprogramación `PENDING`, la nueva franja queda retenida y la original se conserva

### CA-02 — Cita no elegible

**Dado** una cita no `APPROVED` o pasada  
**Cuando** se solicita reprogramación  
**Entonces** la operación es rechazada

### CA-03 — Franja no disponible

**Dado** una franja ocupada o retenida  
**Cuando** se solicita como nueva franja  
**Entonces** la operación es rechazada

### CA-04 — Solicitud pendiente existente

**Dado** una cita con reprogramación `PENDING`  
**Cuando** se solicita otra  
**Entonces** la operación es rechazada

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Migración Flyway presente.
- [ ] Pruebas de backend en verde.
- [ ] Vista integrada en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| CA-04 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

## Notas y decisiones

- Supuesto a confirmar: máximo una reprogramación `PENDING` por cita.
