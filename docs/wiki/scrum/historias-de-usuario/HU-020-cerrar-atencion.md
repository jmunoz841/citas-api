---
id: HU-020
tipo: historia-de-usuario
titulo: "Cerrar atención"
estado: Aprobada
epica: "[[EP-007-ciclo-de-vida-de-citas]]"
esfuerzo: "Bajo"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-011-consultar-agenda-del-profesional]]"
relacionadas:
  - "[[HU-021-historial-de-estados]]"
---

# HU-020 — Cerrar atención

## Historia de usuario

**COMO** PROFESSIONAL  
**QUIERO** marcar una cita pasada como atendida o como inasistencia  
**PARA** dejar registro del resultado de la atención

> Como PROFESSIONAL, quiero marcar una cita pasada como atendida o como inasistencia para dejar registro del resultado de la atención.

## Contexto y descripción

Implementa RF-17.

## Alcance

- Transición `APPROVED` → `COMPLETED` o `NO_SHOW`.
- Registro de historial.
- Acción desde la agenda del profesional.

## Fuera de alcance

- Registro clínico o diagnóstico (fuera de alcance del PRD).

## Reglas de negocio

- Solo el profesional asignado a la cita.
- Solo citas `APPROVED` cuya hora de inicio ya pasó.
- Estados terminales no cambian nuevamente.
- Se registra historial con actor.

## Dependencias y relaciones

- Épica: [[EP-007-ciclo-de-vida-de-citas]]
- Dependencias: [[HU-011-consultar-agenda-del-profesional]]
- Relacionadas: [[HU-021-historial-de-estados]]

## Esfuerzo

**Nivel:** Bajo

**Justificación de dificultad:** transición simple con validaciones de ownership y tiempo.

## Tareas de desarrollo

- [ ] **T-01 — Caso de uso cerrar atención**  
  Dificultad: Bajo  
  Descripción: validación de estado, fecha y ownership; historial.
- [ ] **T-02 — Acción en agenda**  
  Dificultad: Bajo  
  Descripción: botones de completado/inasistencia.
- [ ] **T-03 — Pruebas**  
  Dificultad: Bajo  
  Descripción: cita futura, ajena, estado inválido.

## Criterios de aceptación

### CA-01 — Marcar resultado

**Dado** una cita propia `APPROVED` ya iniciada  
**Cuando** el PROFESSIONAL la marca `COMPLETED` o `NO_SHOW`  
**Entonces** el estado cambia y se registra historial

### CA-02 — Cita futura

**Dado** una cita que aún no inicia  
**Cuando** se intenta cerrar  
**Entonces** la operación es rechazada

### CA-03 — Cita ajena o estado inválido

**Dado** una cita de otro profesional o no `APPROVED`  
**Cuando** se intenta cerrar  
**Entonces** la operación es rechazada

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Acción integrada en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-25 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S4 (bloque "Ciclo de la cita", D-031).

## Notas y decisiones

- Resuelto (D-034): la cita es cerrable desde su hora de inicio (no desde su fin).
