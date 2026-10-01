---
id: HU-017
tipo: historia-de-usuario
titulo: "Cancelar cita"
estado: Completada
epica: "[[EP-007-ciclo-de-vida-de-citas]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-016-consultar-mis-citas]]"
relacionadas:
  - "[[HU-019-resolver-reprogramacion]]"
  - "[[HU-021-historial-de-estados]]"
---

# HU-017 — Cancelar cita

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** cancelar una cita futura que no esté finalizada  
**PARA** liberar el horario cuando ya no puedo asistir

> Como USER autenticado, quiero cancelar una cita futura que no esté finalizada para liberar el horario cuando ya no puedo asistir.

## Contexto y descripción

Implementa RF-14.

## Alcance

- Cancelación de citas propias futuras en estado no terminal.
- Liberación de slots.
- Registro de historial.
- Acción de cancelar en detalle de cita.

## Fuera de alcance

- Reactivación de citas canceladas.

## Reglas de negocio

- Solo citas futuras y no terminales (`REQUESTED`, `APPROVED`) son cancelables.
- `CANCELLED` libera los slots (RN-09).
- Una cita cancelada no se reactiva.
- Si existe una reprogramación `PENDING`, también se liberan sus slots retenidos (supuesto).
- Se registra historial con fuente `USER`.

## Dependencias y relaciones

- Épica: [[EP-007-ciclo-de-vida-de-citas]]
- Dependencias: [[HU-016-consultar-mis-citas]]
- Relacionadas: [[HU-019-resolver-reprogramacion]], [[HU-021-historial-de-estados]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** transición con liberación de recursos, ownership e interacción con reprogramaciones pendientes.

## Tareas de desarrollo

- [x] **T-01 — Caso de uso cancelar**
  Dificultad: Medio  
  Descripción: validaciones de estado/fecha/ownership, liberación e historial.
- [x] **T-02 — Acción en frontend**
  Dificultad: Bajo  
  Descripción: confirmación de cancelación.
- [x] **T-03 — Pruebas**
  Dificultad: Medio  
  Descripción: estados no cancelables, pasado, ajena, liberación.

## Criterios de aceptación

### CA-01 — Cancelación válida

**Dado** una cita propia futura en `APPROVED` o `REQUESTED`  
**Cuando** el USER la cancela  
**Entonces** queda `CANCELLED`, sus slots quedan disponibles y se registra historial

### CA-02 — Estado terminal o pasada

**Dado** una cita `CANCELLED`, `REJECTED`, `COMPLETED`, `NO_SHOW` o pasada  
**Cuando** se intenta cancelar  
**Entonces** la operación es rechazada

### CA-03 — Cita ajena

**Dado** una cita de otro usuario  
**Cuando** se intenta cancelar  
**Entonces** la operación es rechazada

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Acción de cancelación integrada en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `MyAppointmentsApiIntegrationTest#ca01_cancelarUnaCitaPropiaFuturaLaCancelaLiberaSlotsYRegistraHistorial` | Estado, liberación de slots e historial en la misma operación. |
| CA-02 | Cumple | `MyAppointmentsApiIntegrationTest#ca02_unaCitaPasadaOTerminalNoSePuedeCancelar` | Rechaza cita pasada y estados terminales. |
| CA-03 | Cumple | `MyAppointmentsApiIntegrationTest#ca03_unUserNoPuedeCancelarLaCitaDeOtroPaciente` | Aislamiento por ownership con `404`. |
| DoD | Cumple | API `clean test` 167/167; `MyAppointmentsPage`; contrato `docs/contratos/citas.md` | Acción web con confirmación; evidencia consolidada en [[evidencia-s4]]. |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-25 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S4 (bloque "Ciclo de la cita", D-031).

- 2026-09-30 (S4) — HU pasa a `En validación`: la matriz registra evidencia de los tres CA y la DoD en `Cumple`.

- 2026-09-30 (S4) — HU `Completada` con confirmación explícita del Product Owner.

## Notas y decisiones

- Resuelto (D-033): cancelar una cita con reprogramación `PENDING` cancela también la solicitud y libera su retención.
