---
id: HU-018
tipo: historia-de-usuario
titulo: "Solicitar reprogramación"
estado: Completada
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

- [x] **T-01 — Migración de reprogramaciones**  
  Dificultad: Medio  
  Descripción: solicitud vinculada a la cita, estado y franja retenida. `V9__reprogramacion_hu018_hu019.sql` (del diseño 3FN propio).
- [x] **T-02 — Caso de uso solicitar reprogramación**  
  Dificultad: Alto  
  Descripción: validaciones y retención transaccional de nueva franja. `RescheduleRequest.open`, `RescheduleRequestService`, `POST /api/v1/appointments/{id}/reschedule-requests`.
- [x] **T-03 — Vista de reprogramación**  
  Dificultad: Medio  
  Descripción: disponibilidad restringida al mismo profesional/especialidad. `RescheduleForm` dentro del detalle de "Mis citas".
- [x] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: estado inválido, franja ocupada, conservación de la franja original. `RescheduleApiIntegrationTest.hu018_*`, `RescheduleRequestTest`, `MyAppointmentsPage.reschedule.test.tsx`.

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
| CA-01 | Cumple | `hu018_ca01_laSolicitudQuedaPendienteRetieneLaNuevaFranjaYConservaLaOriginal`; `RescheduleRequestTest.hu018_ca01_*`; `MyAppointmentsPage.reschedule.test.tsx` CA-01 | `PENDING`, 2 slots retenidos por la solicitud, 2 conservados por la cita, la franja retenida sale de la búsqueda |
| CA-02 | Cumple | `hu018_ca02_unaCitaNoAprobadaPasadaOAjenaNoSeReprograma`; `RescheduleRequestTest.hu018_ca02_*` | `REQUESTED` → `409 APPOINTMENT_NOT_RESCHEDULABLE`; pasada → `400`; ajena → `404` |
| CA-03 | Cumple | `hu018_ca03_unaFranjaOcupadaORetenidaNoSePuedeSolicitar`; prueba de frontend CA-03 | Ocupada por cita, retenida por otra solicitud o inexistente → `409 SLOT_UNAVAILABLE` |
| CA-04 | Cumple | `hu018_ca04_noSePermiteUnaSegundaSolicitudPendienteParaLaMismaCita`; prueba de frontend CA-04 | `409 RESCHEDULE_ALREADY_PENDING`; también lo impide el índice `uk_rr_one_pending_per_appointment` |
| DoD | Cumple | V9 aplicada en Testcontainers y en la base local; `mvnw clean test` 197/197; vista `RescheduleForm`; contrato `citas.md` § Reprogramación; prueba de humo HTTP contra la API real | Trazabilidad Scrum y épica actualizadas |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-30 (S4) — HU `Aprobada` explícitamente por el Product Owner para completar reprogramación antes de S5.

- 2026-10-04 (S4) — Implementada en LOOP-02 iteración 1 (`docs/loops/LOOP-02-reprogramacion/`). Backend compila; pruebas de dominio y ArchUnit en verde; integración escrita pero no ejecutada (equipo sin Docker). Frontend en verde. Queda `Aprobada`.

- 2026-10-04 (S4) — LOOP-02: `mvnw clean test` 197/197; prueba de humo HTTP contra la API real; Verifier aislado PASS. Iteración 2 corrigió sus hallazgos menores. Matriz CA/DoD en `Cumple`. Pendiente la confirmación del Product Owner para pasar a `Completada`.

- 2026-10-04 (S4) — Cierre confirmado por el Product Owner: HU `Completada`.

## Notas y decisiones

- Resuelto (D-033): máximo una reprogramación `PENDING` por cita.
- Supuesto S-20 del diseño 3FN aplicado: la reprogramación puede cambiar de sede; conserva profesional, especialidad y duración.
- Aprobado (D-036): se exige que profesional, especialidad y asociación sigan activos al solicitar (`400`, `field: appointmentId`), igual que al reservar.
