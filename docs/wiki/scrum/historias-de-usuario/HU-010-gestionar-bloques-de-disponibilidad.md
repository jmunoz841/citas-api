---
id: HU-010
tipo: historia-de-usuario
titulo: "Gestionar bloques de disponibilidad"
estado: Borrador
epica: "[[EP-005-agenda-del-profesional]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-008-crear-profesional]]"
relacionadas:
  - "[[HU-012-consultar-disponibilidad]]"
  - "[[HU-011-consultar-agenda-del-profesional]]"
---

# HU-010 — Gestionar bloques de disponibilidad

## Historia de usuario

**COMO** PROFESSIONAL  
**QUIERO** crear, editar, eliminar y consultar mis bloques de disponibilidad por sede  
**PARA** publicar los horarios en los que puedo atender citas

> Como PROFESSIONAL, quiero crear, editar, eliminar y consultar mis bloques de disponibilidad por sede para publicar los horarios en los que puedo atender citas.

## Contexto y descripción

Implementa RF-08. Un día puede tener varios bloques (p. ej. 08:00–12:00 HIC y 14:00–17:00 HIC). Cada bloque se discretiza en slots de 30 minutos.

## Alcance

- Crear múltiples bloques por día con sede.
- Discretización en slots de 30 minutos.
- Editar/eliminar bloques futuros sin citas comprometidas.
- Consultar calendario propio de bloques.
- Vista de gestión de bloques/calendario.

## Fuera de alcance

- Bloques recurrentes/plantillas semanales.
- Consulta de citas en la agenda ([[HU-011-consultar-agenda-del-profesional]]).

## Reglas de negocio

- No se crean bloques en el pasado (RN-06).
- No se solapan bloques del mismo profesional, incluso en sedes distintas.
- El profesional debe estar asignado a la sede del bloque (RN-07).
- Los límites del bloque deben alinearse a slots de 30 minutos.
- No se edita ni elimina un bloque con slots reservados/retenidos.
- Un profesional solo gestiona sus propios bloques.

## Dependencias y relaciones

- Épica: [[EP-005-agenda-del-profesional]]
- Dependencias: [[HU-008-crear-profesional]]
- Relacionadas: [[HU-012-consultar-disponibilidad]], [[HU-011-consultar-agenda-del-profesional]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** reglas temporales (pasado, solapamiento, alineación), generación de slots y restricciones de edición según reservas.

## Tareas de desarrollo

- [ ] **T-01 — Migración de bloques y slots**  
  Dificultad: Medio  
  Descripción: modelo de bloque por profesional/sede/fecha y representación de slots con índices para agenda.
- [ ] **T-02 — Reglas de dominio de bloques**  
  Dificultad: Alto  
  Descripción: pasado, solapamiento, sede habilitada, alineación a 30 minutos.
- [ ] **T-03 — Casos de uso CRUD de bloques**  
  Dificultad: Medio  
  Descripción: creación con slots, edición/eliminación condicionada.
- [ ] **T-04 — Vista de calendario de bloques**  
  Dificultad: Alto  
  Descripción: según diseño aprobado.
- [ ] **T-05 — Pruebas**  
  Dificultad: Medio  
  Descripción: cada regla de negocio y ownership.

## Criterios de aceptación

### CA-01 — Varios bloques en un día

**Dado** un PROFESSIONAL asignado a HIC  
**Cuando** crea 08:00–12:00 y 14:00–17:00 en HIC para una fecha futura  
**Entonces** ambos bloques quedan registrados con 8 y 6 slots de 30 minutos respectivamente

### CA-02 — Bloque en el pasado

**Dado** un PROFESSIONAL  
**Cuando** crea un bloque con inicio en el pasado  
**Entonces** la operación es rechazada

### CA-03 — Solapamiento

**Dado** un bloque existente 08:00–12:00  
**Cuando** el mismo profesional crea 11:00–13:00 el mismo día en cualquier sede  
**Entonces** la operación es rechazada

### CA-04 — Sede no asignada

**Dado** un profesional no asignado a ICV  
**Cuando** crea un bloque en ICV  
**Entonces** la operación es rechazada

### CA-05 — Bloque con citas

**Dado** un bloque con al menos un slot reservado o retenido  
**Cuando** el profesional intenta editarlo o eliminarlo  
**Entonces** la operación es rechazada

### CA-06 — Ownership

**Dado** un PROFESSIONAL  
**Cuando** intenta modificar bloques de otro profesional  
**Entonces** recibe acceso denegado

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Migración Flyway presente con índices para consultas de agenda.
- [ ] Pruebas de reglas de bloques en verde.
- [ ] Vista de bloques/calendario integrada en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| CA-04 | Pendiente | — | — |
| CA-05 | Pendiente | — | — |
| CA-06 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

## Notas y decisiones

- Decisión pendiente (normalización): materializar slots como filas o calcularlos desde el bloque.
- CA-05 solo es verificable una vez existan reservas ([[HU-013-reservar-cita-general]]).
