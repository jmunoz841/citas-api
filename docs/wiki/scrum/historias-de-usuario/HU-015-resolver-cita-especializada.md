---
id: HU-015
tipo: historia-de-usuario
titulo: "Aprobar o rechazar cita especializada"
estado: Aprobada
epica: "[[EP-006-reserva-de-citas]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-014-solicitar-cita-especializada]]"
relacionadas:
  - "[[HU-022-bandeja-administrativa]]"
  - "[[HU-021-historial-de-estados]]"
---

# HU-015 — Aprobar o rechazar cita especializada

## Historia de usuario

**COMO** ADMIN  
**QUIERO** ver las citas especializadas solicitadas y aprobarlas o rechazarlas con motivo  
**PARA** controlar la asignación de atención especializada

> Como ADMIN, quiero ver las citas especializadas solicitadas y aprobarlas o rechazarlas con motivo para controlar la asignación de atención especializada.

## Contexto y descripción

Implementa la resolución de RF-12. Incluye un listado básico de solicitudes `REQUESTED` y el dashboard ADMIN; los filtros completos quedan en [[HU-022-bandeja-administrativa]].

## Alcance

- Listado de citas `REQUESTED`.
- Aprobar → `APPROVED`.
- Rechazar con motivo → `REJECTED` y liberación de slots.
- Dashboard ADMIN y vista "aprobar/rechazar citas".

## Fuera de alcance

- Filtros avanzados y reprogramaciones ([[HU-022-bandeja-administrativa]]).

## Reglas de negocio

- Solo ADMIN resuelve solicitudes.
- Solo citas en `REQUESTED` pueden aprobarse o rechazarse (RN-11).
- El rechazo exige motivo (RN-04).
- Rechazar libera los slots (RN-09).
- Cada transición registra historial con fuente `ADMIN` y actor.

## Dependencias y relaciones

- Épica: [[EP-006-reserva-de-citas]]
- Dependencias: [[HU-014-solicitar-cita-especializada]]
- Relacionadas: [[HU-022-bandeja-administrativa]], [[HU-021-historial-de-estados]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** transiciones de estado con liberación de slots e historial, protegidas por rol.

## Tareas de desarrollo

- [x] **T-01 — Casos de uso aprobar/rechazar**  
  Dificultad: Medio  
  Descripción: validación de estado, motivo, liberación de slots e historial.
- [x] **T-02 — Listado de solicitudes**  
  Dificultad: Bajo  
  Descripción: consulta de citas `REQUESTED` para ADMIN.
- [x] **T-03 — Dashboard ADMIN y vista de aprobación**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [x] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: aprobación, rechazo sin motivo, estado inválido, liberación y autorización.

## Criterios de aceptación

### CA-01 — Aprobar

**Dado** una cita `REQUESTED`  
**Cuando** ADMIN la aprueba  
**Entonces** pasa a `APPROVED` con historial registrado

### CA-02 — Rechazar con motivo

**Dado** una cita `REQUESTED`  
**Cuando** ADMIN la rechaza con motivo  
**Entonces** pasa a `REJECTED`, guarda el motivo y sus slots vuelven a estar disponibles

### CA-03 — Rechazo sin motivo

**Dado** una cita `REQUESTED`  
**Cuando** ADMIN la rechaza sin motivo  
**Entonces** la operación es rechazada con error de validación

### CA-04 — Estado inválido

**Dado** una cita que no está en `REQUESTED`  
**Cuando** se intenta aprobar o rechazar  
**Entonces** la transición es rechazada

### CA-05 — Autorización

**Dado** un USER o PROFESSIONAL  
**Cuando** intenta resolver una solicitud  
**Entonces** recibe acceso denegado

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Pruebas de transiciones y autorización en verde.
- [x] Dashboard ADMIN y vista de aprobación integrados en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Aprobar | Cumple | `AdminAppointmentApiIntegrationTest.ca01_aprobarPasaAApprovedYRegistraHistorialDelAdmin`; `AppointmentTest.ca01_ca02_unaSolicitudSeApruebaOSeRechaza` | 200 `APPROVED`; historial `APPROVED`, `source: ADMIN`, actor = ADMIN; el slot sigue ocupado |
| CA-02 Rechazar con motivo | Cumple | `ca02_rechazarConMotivoGuardaElMotivoYLiberaLosSlots` | Cita de 60 min: 200 `REJECTED`, motivo en el historial, 0 filas en `slot_reservations` y otro USER reserva el mismo horario con 201 |
| CA-03 Rechazo sin motivo | Cumple | `ca03_rechazarSinMotivoEsUnErrorDeValidacion`; `AppointmentTest.ca03_rechazarExigeMotivo` | Motivo en blanco o ausente → 400 `VALIDATION_ERROR`, `field: reason`; la cita sigue `REQUESTED` con su slot |
| CA-04 Estado inválido | Cumple | `ca04_soloUnaCitaSolicitadaPuedeAprobarseORechazarse`; `ca04_unaCitaGeneralAprobadaNoPasaPorElAdmin`; `aprobarYRechazarALaVezSoloResuelveUnaVez` | Ya rechazada o general → 409 `INVALID_STATUS_TRANSITION`. Aprobar y rechazar a la vez → un 200 y un 409, un solo registro de decisión |
| CA-05 Autorización | Cumple | `ca05_soloElAdminPuedeVerYResolverSolicitudes` | USER y PROFESSIONAL → 403 en listado, aprobar y rechazar; sin token → 401 |
| Listado | Cumple | `elListadoMuestraSoloLasSolicitudesPendientes` | Solo `REQUESTED`, con nombres de paciente, profesional y especialidad |
| Red → Green | Cumple | Pruebas escritas antes de la implementación | Red: 9 pruebas, 7 fallos (404: los endpoints no existían); las 2 que ya pasaban cubren autorización y cita inexistente, que dependen de reglas previas. Green: 13/13 (9 integración + 4 dominio) |
| DoD Pruebas | Cumple | `mvnw clean test` 2026-09-25: 149 pruebas, 0 fallos | — |
| DoD Vista `citas-web` | Cumple | `RequestsPage.test.tsx` (10 pruebas: listado, vacío, sin conexión, aprobar, rechazar sin motivo y con motivo, contador, error del servidor, 409 al aprobar y al rechazar) | `citas-web` `576db18`; `npm test` 95/95, lint y build en verde; verificada contra la API real con capturas autenticadas frente a Stitch v4 (D-026) |
| Contrato | Cumple | `docs/contratos/citas.md` § Decisión del ADMIN | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-25 (S3) — Backend implementado con pruebas primero (Red → Green) y verificado (`mvnw clean test` 149/149). Pendiente el dashboard ADMIN.

- 2026-09-25 (S3) — Vista integrada en `citas-web` (`citas-web` `576db18`) según el diseño aprobado en Stitch v4. Estado de la HU sin cambios.

## Notas y decisiones

- La transición es un `UPDATE` condicionado a `status_code = 'REQUESTED'`: la base decide cuál de dos decisiones simultáneas se aplica, igual que la PK de `slot_reservations` en la reserva.
- Resuelto (D-030): las solicitudes `REQUESTED` no expiran en S3.
