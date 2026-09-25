---
id: HU-016
tipo: historia-de-usuario
titulo: "Consultar mis citas"
estado: Aprobada
epica: "[[EP-007-ciclo-de-vida-de-citas]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-013-reservar-cita-general]]"
  - "[[HU-015-resolver-cita-especializada]]"
relacionadas:
  - "[[HU-017-cancelar-cita]]"
  - "[[HU-018-solicitar-reprogramacion]]"
---

# HU-016 — Consultar mis citas

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** consultar mis citas y filtrarlas por estado y fecha  
**PARA** conocer el estado y los detalles de mi atención

> Como USER autenticado, quiero consultar mis citas y filtrarlas por estado y fecha para conocer el estado y los detalles de mi atención.

## Contexto y descripción

Implementa RF-13. Incluye listado y detalle.

## Alcance

- Listado de citas propias con filtros por estado y fecha.
- Detalle: sede, profesional, especialidad, fecha/hora, duración, estado y motivo de rechazo.
- Vistas "mis citas" y detalle.

## Fuera de alcance

- Acciones de cancelación/reprogramación (HU propias).

## Reglas de negocio

- Un USER solo ve sus propias citas (ownership).
- El motivo de rechazo se muestra cuando existe.

## Dependencias y relaciones

- Épica: [[EP-007-ciclo-de-vida-de-citas]]
- Dependencias: [[HU-013-reservar-cita-general]], [[HU-015-resolver-cita-especializada]]
- Relacionadas: [[HU-017-cancelar-cita]], [[HU-018-solicitar-reprogramacion]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** consulta con joins de varios catálogos, filtros y ownership.

## Tareas de desarrollo

- [ ] **T-01 — Consulta de citas del usuario**  
  Dificultad: Medio  
  Descripción: listado paginable/filtrable y detalle.
- [ ] **T-02 — Vistas de listado y detalle**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [ ] **T-03 — Pruebas**  
  Dificultad: Medio  
  Descripción: filtros, campos mínimos y acceso a citas ajenas.

## Criterios de aceptación

### CA-01 — Listado con datos mínimos

**Dado** un USER con citas  
**Cuando** consulta sus citas  
**Entonces** ve sede, profesional, especialidad, fecha/hora, duración y estado de cada una

### CA-02 — Filtros

**Dado** citas en varios estados y fechas  
**Cuando** filtra por estado y/o fecha  
**Entonces** solo ve las que cumplen los filtros

### CA-03 — Motivo de rechazo

**Dado** una cita `REJECTED`  
**Cuando** consulta su detalle  
**Entonces** ve el motivo de rechazo

### CA-04 — Ownership

**Dado** una cita de otro usuario  
**Cuando** intenta consultarla  
**Entonces** recibe acceso denegado o no encontrado

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Vistas integradas en `citas-web`.
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

- 2026-09-25 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S4 (bloque "Ciclo de la cita", D-031).

## Notas y decisiones

- Ninguna.
