---
id: HU-011
tipo: historia-de-usuario
titulo: "Consultar agenda del profesional"
estado: Aprobada
epica: "[[EP-005-agenda-del-profesional]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-010-gestionar-bloques-de-disponibilidad]]"
  - "[[HU-013-reservar-cita-general]]"
relacionadas:
  - "[[HU-020-cerrar-atencion]]"
---

# HU-011 — Consultar agenda del profesional

## Historia de usuario

**COMO** PROFESSIONAL  
**QUIERO** consultar mis citas aprobadas por día o semana y sede  
**PARA** organizar mi atención

> Como PROFESSIONAL, quiero consultar mis citas aprobadas por día o semana y sede para organizar mi atención.

## Contexto y descripción

Implementa RF-16. Incluye el dashboard PROFESSIONAL y la vista de agenda.

## Alcance

- Consulta de citas `APPROVED` propias por día/semana.
- Filtro por sede.
- Dashboard PROFESSIONAL y vista de agenda.

## Fuera de alcance

- Gestión de bloques ([[HU-010-gestionar-bloques-de-disponibilidad]]).
- Cierre de atención ([[HU-020-cerrar-atencion]]).

## Reglas de negocio

- Solo se muestran citas del propio profesional.
- El profesional no ve datos de usuarios fuera de sus propias citas.
- Solo citas en estado `APPROVED`.

## Dependencias y relaciones

- Épica: [[EP-005-agenda-del-profesional]]
- Dependencias: [[HU-010-gestionar-bloques-de-disponibilidad]], [[HU-013-reservar-cita-general]]
- Relacionadas: [[HU-020-cerrar-atencion]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** consulta por rangos de fecha con filtros y control estricto de visibilidad de datos.

## Tareas de desarrollo

- [ ] **T-01 — Consulta de agenda**  
  Dificultad: Medio  
  Descripción: caso de uso por rango de fechas y sede, filtrado por profesional autenticado.
- [ ] **T-02 — Dashboard y vista de agenda**  
  Dificultad: Medio  
  Descripción: vista diaria/semanal según diseño aprobado.
- [ ] **T-03 — Pruebas**  
  Dificultad: Medio  
  Descripción: filtros, estados excluidos y aislamiento entre profesionales.

## Criterios de aceptación

### CA-01 — Agenda por día/semana

**Dado** un PROFESSIONAL con citas aprobadas  
**Cuando** consulta un día o una semana  
**Entonces** ve sus citas `APPROVED` de ese rango con usuario, especialidad, sede y hora

### CA-02 — Filtro por sede

**Dado** citas en HIC e ICV  
**Cuando** filtra por una sede  
**Entonces** solo ve las citas de esa sede

### CA-03 — Aislamiento

**Dado** citas de otros profesionales  
**Cuando** un PROFESSIONAL consulta su agenda  
**Entonces** no ve citas ni datos de usuarios ajenos

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Dashboard y agenda integrados en `citas-web`.
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

- Ninguna.
