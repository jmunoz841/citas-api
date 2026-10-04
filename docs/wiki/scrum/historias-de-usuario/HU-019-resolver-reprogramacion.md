---
id: HU-019
tipo: historia-de-usuario
titulo: "Aprobar o rechazar reprogramación"
estado: Completada
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

- [x] **T-01 — Casos de uso aprobar/rechazar reprogramación**  
  Dificultad: Alto  
  Descripción: intercambio o liberación de slots en una transacción. `RescheduleResolutionService`, `/api/v1/admin/reschedule-requests/{id}/{approve,reject}`.
- [x] **T-02 — Vista de resolución**  
  Dificultad: Medio  
  Descripción: comparación franja original vs nueva, motivo. Pestaña "Reprogramaciones" de la bandeja, `ApproveRescheduleDialog` y `RejectRescheduleDialog`.
- [x] **T-03 — Pruebas**  
  Dificultad: Alto  
  Descripción: aprobación, rechazo, sin motivo, estado inválido, consistencia de slots. `RescheduleApiIntegrationTest.hu019_*` y `d033_*`, `RequestsPage.inbox.test.tsx`.

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
| CA-01 | Cumple | `hu019_ca01_aprobarMueveLaCitaLiberaLosSlotsAntiguosYRegistraHistorial`; `RequestsPage.inbox.test.tsx` CA-01 | Cita en la nueva sede/hora con exactamente 2 slots nuevos; la franja original se puede volver a reservar; historial `ADMIN` enlazado a la solicitud |
| CA-02 | Cumple | `hu019_ca02_rechazarLiberaLaFranjaProvisionalYConservaLaOriginal`; prueba de frontend CA-02/CA-03 | Retención liberada, cita intacta, el paciente ve el motivo y puede pedir otra franja |
| CA-03 | Cumple | `hu019_ca03_rechazarSinMotivoSeRechaza`; `RescheduleRequestTest.hu019_ca03_*` | `400`, `field: reason`; la solicitud sigue `PENDING` |
| CA-04 | Cumple | `hu019_ca04_soloUnaPendienteSeResuelveYSoloPorAdmin`; `RescheduleRequestTest.hu019_ca04_*` | USER/PROFESSIONAL → `403`; segunda decisión → `409 RESCHEDULE_NOT_PENDING`; inexistente → `404` |
| DoD | Cumple | Consistencia de slots: CA-01, CA-02 y `d033_*`; `mvnw clean test` 197/197; pestaña de reprogramaciones; prueba de humo HTTP (aprobar libera la franja original) | Trazabilidad Scrum y épica actualizadas |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-30 (S4) — HU `Aprobada` explícitamente por el Product Owner para completar reprogramación antes de S5.

- 2026-10-04 (S4) — Implementada en LOOP-02 iteración 1. D-033 aplicado: cancelar o cerrar la cita cancela la solicitud pendiente; al llegar la hora, la bandeja y un job cada 5 minutos la cierran. Integración escrita pero no ejecutada (sin Docker). Queda `Aprobada`.

- 2026-10-04 (S4) — LOOP-02: `mvnw clean test` 197/197; prueba de humo HTTP (aprobar mueve la cita y libera la franja original); Verifier PASS. Iteración 2: orden de bloqueo coherente entre aprobar y cancelar, interbloqueo → `409 CONCURRENT_UPDATE`, y aviso al paciente de una solicitud `CANCELLED`. Pendiente la confirmación del Product Owner.

- 2026-10-04 (S4) — Cierre confirmado por el Product Owner: HU `Completada`.

## Notas y decisiones

- Resuelto (D-033): si llega la hora de la cita original con la reprogramación aún `PENDING`, la solicitud se cancela y libera su retención.
- Aprobado (D-037): la solicitud también vence si llega la hora **pedida** antes de decidir (aprobarla movería la cita al pasado, RN-06). Aprobar una vencida responde `409 RESCHEDULE_EXPIRED`.
