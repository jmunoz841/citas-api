---
id: HU-021
tipo: historia-de-usuario
titulo: "Consultar historial de estados de una cita"
estado: Borrador
epica: "[[EP-007-ciclo-de-vida-de-citas]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-013-reservar-cita-general]]"
relacionadas:
  - "[[HU-015-resolver-cita-especializada]]"
  - "[[HU-017-cancelar-cita]]"
  - "[[HU-020-cerrar-atencion]]"
---

# HU-021 — Consultar historial de estados de una cita

## Historia de usuario

**COMO** ADMIN o USER dueño de la cita  
**QUIERO** consultar el historial de cambios de estado de una cita  
**PARA** tener trazabilidad verificable de cada transición

> Como ADMIN o USER dueño de la cita, quiero consultar el historial de cambios de estado de una cita para tener trazabilidad verificable de cada transición.

## Contexto y descripción

Implementa RF-19 y RN-12. El registro del historial nace en [[HU-013-reservar-cita-general]]; esta HU garantiza completitud, inmutabilidad y consulta.

## Alcance

- Verificación de que todas las transiciones existentes registran historial.
- Consulta del historial en el detalle de la cita.

## Fuera de alcance

- Auditoría de entidades distintas de citas.

## Reglas de negocio

- Cada registro guarda: cita, estado nuevo, actor (si existe), fuente `SYSTEM`/`USER`/`ADMIN`, fecha/hora y motivo opcional.
- El historial no se edita ni borra mediante la API (RN-12).
- USER solo ve historial de sus citas; ADMIN ve todos.

## Dependencias y relaciones

- Épica: [[EP-007-ciclo-de-vida-de-citas]]
- Dependencias: [[HU-013-reservar-cita-general]]
- Relacionadas: [[HU-015-resolver-cita-especializada]], [[HU-017-cancelar-cita]], [[HU-020-cerrar-atencion]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** transversal a todas las transiciones de estado; requiere revisar consistencia en varios casos de uso.

## Tareas de desarrollo

- [ ] **T-01 — Consulta de historial**  
  Dificultad: Bajo  
  Descripción: caso de uso y endpoint con ownership.
- [ ] **T-02 — Revisión transversal de transiciones**  
  Dificultad: Medio  
  Descripción: confirmar registro en creación, aprobación, rechazo, cancelación, reprogramación y cierre.
- [ ] **T-03 — Visualización en detalle de cita**  
  Dificultad: Bajo  
  Descripción: línea de tiempo de estados.
- [ ] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: completitud por transición, inmutabilidad y ownership.

## Criterios de aceptación

### CA-01 — Registro completo

**Dado** cualquier transición de estado de una cita  
**Cuando** ocurre  
**Entonces** existe un registro con estado nuevo, fuente, fecha/hora, actor cuando aplica y motivo cuando se proporcionó

### CA-02 — Inmutabilidad

**Dado** un registro de historial  
**Cuando** se intenta modificar o borrar vía API  
**Entonces** la operación no está disponible

### CA-03 — Consulta con ownership

**Dado** un USER  
**Cuando** consulta el historial de una cita ajena  
**Entonces** recibe acceso denegado o no encontrado

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Historial visible en `citas-web`.
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

## Notas y decisiones

- Incógnita: si PROFESSIONAL también debe consultar historial de sus citas.
