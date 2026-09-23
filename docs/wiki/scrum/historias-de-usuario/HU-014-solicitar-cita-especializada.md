---
id: HU-014
tipo: historia-de-usuario
titulo: "Solicitar cita especializada"
estado: Aprobada
epica: "[[EP-006-reserva-de-citas]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-013-reservar-cita-general]]"
relacionadas:
  - "[[HU-015-resolver-cita-especializada]]"
---

# HU-014 — Solicitar cita especializada

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** solicitar una cita especializada eligiendo especialidad, sede, profesional y horario  
**PARA** asegurar el horario mientras la administración la aprueba

> Como USER autenticado, quiero solicitar una cita especializada eligiendo especialidad, sede, profesional y horario para asegurar el horario mientras la administración la aprueba.

## Contexto y descripción

Implementa la parte de solicitud de RF-12. Reutiliza el mecanismo de reserva de [[HU-013-reservar-cita-general]].

## Alcance

- Solicitud con especialidad distinta de Medicina General.
- Estado inicial `REQUESTED`.
- Retención de slots (30 o 60 min).
- Flujo especializado en la vista "solicitar cita".

## Fuera de alcance

- Aprobación/rechazo ([[HU-015-resolver-cita-especializada]]).

## Reglas de negocio

- La solicitud nace en `REQUESTED` (RN-03).
- Los slots quedan retenidos y no pueden reservarse por otros (RN-01).
- Para 60 min se retienen 2 slots consecutivos (RN-05).
- La especialidad debe estar activa y asociada al profesional (RN-08).
- Se registra historial del estado inicial.

## Dependencias y relaciones

- Épica: [[EP-006-reserva-de-citas]]
- Dependencias: [[HU-013-reservar-cita-general]]
- Relacionadas: [[HU-015-resolver-cita-especializada]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** reutiliza la reserva existente, añadiendo estado inicial distinto, retención de 2 slots y validaciones de especialidad.

## Tareas de desarrollo

- [ ] **T-01 — Caso de uso solicitar cita especializada**  
  Dificultad: Medio  
  Descripción: validaciones de especialidad/profesional/sede y retención de slots.
- [ ] **T-02 — Flujo especializado en la vista**  
  Dificultad: Medio  
  Descripción: selección de especialidad, sede, profesional y horario.
- [ ] **T-03 — Pruebas**  
  Dificultad: Medio  
  Descripción: 30/60 min, slots retenidos, especialidad no asociada.

## Criterios de aceptación

### CA-01 — Solicitud creada

**Dado** un horario libre de una especialidad asociada al profesional  
**Cuando** el USER confirma  
**Entonces** la cita queda `REQUESTED` y los slots quedan retenidos

### CA-02 — Retención de 60 minutos

**Dado** una especialidad de 60 min  
**Cuando** se solicita  
**Entonces** se retienen 2 slots consecutivos y no aparecen en disponibilidad

### CA-03 — Especialidad no asociada

**Dado** un profesional sin la especialidad seleccionada o especialidad inactiva  
**Cuando** se solicita la cita  
**Entonces** la operación es rechazada

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Flujo especializado integrado en `citas-web`.
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

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

## Notas y decisiones

- Incógnita: el PRD no define expiración automática de solicitudes `REQUESTED` no resueltas.
