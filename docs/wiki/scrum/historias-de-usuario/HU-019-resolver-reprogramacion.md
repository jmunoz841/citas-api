---
id: HU-019
tipo: historia-de-usuario
titulo: "Aprobar o rechazar reprogramación"
estado: Borrador
epica: "[[EP-007-ciclo-de-vida-de-citas]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-018-solicitar-reprogramacion]]"
relacionadas:
  - "[[HU-022-bandeja-administrativa]]"
  - "[[HU-017-cancelar-cita]]"
---

# HU-019 — Aprobar o rechazar reprogramación

## Historia de usuario

**COMO** ADMIN  
**QUIERO** aprobar o rechazar solicitudes de reprogramación  
**PARA** que el cambio de horario se aplique de forma controlada

> Como ADMIN, quiero aprobar o rechazar solicitudes de reprogramación para que el cambio de horario se aplique de forma controlada.

## Contexto y descripción

Implementa la resolución de RF-15.

## Alcance

- Aprobar: liberar slots antiguos, asignar nuevos y actualizar la cita.
- Rechazar con motivo: liberar la reserva provisional y mantener la cita original.
- Vista "aprobar/rechazar reprogramaciones".

## Fuera de alcance

- Filtros de bandeja ([[HU-022-bandeja-administrativa]]).

## Reglas de negocio

- Solo ADMIN resuelve; solo solicitudes `PENDING`.
- El rechazo requiere motivo (RN-04).
- Aprobación atómica: nunca quedan ambas franjas asignadas ni ninguna.
- Tras rechazo, el USER conserva la cita o puede cancelarla ([[HU-017-cancelar-cita]]).
- Se registra historial de la cita cuando cambia su horario.

## Dependencias y relaciones

- Épica: [[EP-007-ciclo-de-vida-de-citas]]
- Dependencias: [[HU-018-solicitar-reprogramacion]]
- Relacionadas: [[HU-022-bandeja-administrativa]], [[HU-017-cancelar-cita]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** intercambio transaccional de reservas y consistencia entre cita, solicitud e historial.

## Tareas de desarrollo

- [ ] **T-01 — Casos de uso aprobar/rechazar reprogramación**  
  Dificultad: Alto  
  Descripción: intercambio o liberación de slots en una transacción.
- [ ] **T-02 — Vista de resolución**  
  Dificultad: Medio  
  Descripción: comparación franja original vs nueva, motivo.
- [ ] **T-03 — Pruebas**  
  Dificultad: Alto  
  Descripción: aprobación, rechazo, sin motivo, estado inválido, consistencia de slots.

## Criterios de aceptación

### CA-01 — Aprobar

**Dado** una reprogramación `PENDING`  
**Cuando** ADMIN la aprueba  
**Entonces** la cita queda con la nueva franja, los slots antiguos quedan libres y la solicitud queda aprobada

### CA-02 — Rechazar

**Dado** una reprogramación `PENDING`  
**Cuando** ADMIN la rechaza con motivo  
**Entonces** la franja provisional queda libre y la cita conserva su franja original

### CA-03 — Motivo obligatorio

**Dado** una reprogramación `PENDING`  
**Cuando** se rechaza sin motivo  
**Entonces** la operación es rechazada

### CA-04 — Estado inválido y autorización

**Dado** una solicitud no `PENDING` o un usuario sin rol ADMIN  
**Cuando** se intenta resolver  
**Entonces** la operación es rechazada

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de consistencia de slots en verde.
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

- Resuelto (D-033): si llega la hora de la cita original con la reprogramación aún `PENDING`, la solicitud se cancela y libera su retención.
