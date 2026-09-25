---
id: HU-013
tipo: historia-de-usuario
titulo: "Reservar cita general"
estado: Aprobada
epica: "[[EP-006-reserva-de-citas]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-012-consultar-disponibilidad]]"
relacionadas:
  - "[[HU-014-solicitar-cita-especializada]]"
  - "[[HU-021-historial-de-estados]]"
---

# HU-013 — Reservar cita general

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** reservar una cita de Medicina General con un profesional disponible  
**PARA** obtener atención sin esperar aprobación administrativa

> Como USER autenticado, quiero reservar una cita de Medicina General con un profesional disponible para obtener atención sin esperar aprobación administrativa.

## Contexto y descripción

Implementa RF-11 y RN-02. Primera HU que crea citas; establece el mecanismo de reserva de slots y el registro de historial de estados (RF-19) que reutilizan las siguientes HU.

## Alcance

- Selección de profesional general y horario disponible.
- Creación de la cita en `APPROVED` si el horario sigue libre al confirmar.
- Reserva atómica de los slots.
- Registro del estado inicial en el historial.
- Vista "solicitar cita" para el flujo general.

## Fuera de alcance

- Citas especializadas ([[HU-014-solicitar-cita-especializada]]).

## Reglas de negocio

- La especialidad es `Medicina General`.
- Ninguna cita ocupa slots reservados/retenidos (RN-01); la verificación ocurre al confirmar.
- Dos reservas simultáneas del mismo slot: solo una tiene éxito.
- No se reservan horarios pasados (RN-06).
- Estado inicial `APPROVED` con historial fuente `SYSTEM` o `USER`.

## Dependencias y relaciones

- Épica: [[EP-006-reserva-de-citas]]
- Dependencias: [[HU-012-consultar-disponibilidad]]
- Relacionadas: [[HU-014-solicitar-cita-especializada]], [[HU-021-historial-de-estados]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** requiere control de concurrencia contra doble reserva y transacción que une cita, slots e historial.

## Tareas de desarrollo

- [x] **T-01 — Migración de citas, slots reservados e historial**  
  Dificultad: Alto  
  Descripción: modelo de cita, vínculo con slots con restricción que impida doble reserva, historial de estados.
- [x] **T-02 — Caso de uso reservar cita general**  
  Dificultad: Alto  
  Descripción: validación, reserva transaccional y registro de historial.
- [x] **T-03 — Vista de solicitud de cita**  
  Dificultad: Medio  
  Descripción: confirmación y manejo de "horario ya no disponible".
- [x] **T-04 — Pruebas**  
  Dificultad: Alto  
  Descripción: reserva exitosa, slot ocupado, doble reserva concurrente, horario pasado.

## Criterios de aceptación

### CA-01 — Reserva exitosa

**Dado** un horario libre de Medicina General  
**Cuando** el USER confirma la reserva  
**Entonces** la cita queda `APPROVED`, los slots quedan ocupados y existe un registro de historial

### CA-02 — Horario ocupado al confirmar

**Dado** un horario reservado por otro usuario después de la búsqueda  
**Cuando** el USER confirma  
**Entonces** la reserva es rechazada con un error identificable

### CA-03 — Doble reserva concurrente

**Dado** dos solicitudes simultáneas para el mismo slot  
**Cuando** se procesan  
**Entonces** exactamente una crea la cita

### CA-04 — Horario pasado

**Dado** un horario en el pasado  
**Cuando** se intenta reservar  
**Entonces** la operación es rechazada

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Migración Flyway presente.
- [x] Prueba de doble reserva en verde.
- [x] Vista de solicitud integrada en `citas-web`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/` (EP-006 sincronizada el 2026-09-25).

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Reserva exitosa | Cumple | `BookingApiIntegrationTest.hu013_ca01_citaGeneralQuedaAprobadaOcupaElSlotYDejaHistorial` | 201 `APPROVED`; 1 fila en `slot_reservations`; historial `APPROVED`, `source: USER`, actor = paciente |
| CA-02 Horario ocupado al confirmar | Cumple | `hu013_ca02_horarioOcupadoAlConfirmarSeRechazaConCodigoIdentificable` | 409 `SLOT_UNAVAILABLE`; la transacción fallida no deja una cita huérfana |
| CA-03 Doble reserva concurrente (LOOP) | Cumple | `hu013_ca03_dobleReservaConcurrenteSoloUnaCreaLaCita`; `hu013_ca03_laClavePrimariaDeLaBaseImpideOcuparDosVecesElMismoSlot` | Dos hilos por HTTP sobre el mismo slot → exactamente un 201 y un 409; 1 cita y 1 ocupación en BD. La aplicación no comprueba antes: decide `pk_slot_reservations` (un `INSERT` directo duplicado lanza `DuplicateKeyException`) |
| CA-04 Horario pasado | Cumple | `hu013_ca04_horarioPasadoSeRechaza` | 400 con `field: startTime` |
| Horario inexistente | Cumple | `unHorarioQueNoExisteSeRechaza` | 409 `SLOT_UNAVAILABLE` |
| Autorización | Cumple | `soloUnUserPuedeBuscarYReservar` | Sin token 401; PROFESSIONAL y ADMIN 403 |
| DoD Migración | Cumple | `V6__citas_hu012_hu014.sql` | `appointments`, `slot_reservations`, `appointment_status_history`. Aplicada en `jmunoz-citas-mysql` el 2026-09-25 (Flyway v6) |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-25: 136 pruebas, 0 fallos | — |
| DoD Vista `citas-web` | Cumple | `PatientHomePage.test.tsx` (Medicina General confirmada, 409 `SLOT_UNAVAILABLE` vuelve al paso 3, horario pasado) | `citas-web` `576db18`; `npm test` 95/95, lint y build en verde; verificada contra la API real con capturas autenticadas frente a Stitch v4 (D-026) |
| Contrato | Cumple | `docs/contratos/citas.md` | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-25 (S3) — Backend implementado y verificado: migración V6, `POST /api/v1/appointments`, LOOP de doble reserva en verde (`mvnw test` 136/136). Pendiente la vista de solicitud.

- 2026-09-25 (S3) — Vista integrada en `citas-web` (`citas-web` `576db18`) según el diseño aprobado en Stitch v4. Estado de la HU sin cambios.

## Notas y decisiones

- Resuelto (D-013): la doble reserva la impide la clave primaria `slot_id` de `slot_reservations`, no un bloqueo de la aplicación. Los slots de una cita de 60 minutos se insertan ordenados por id para que dos reservas en competencia no se interbloqueen.
- Resuelto (D-025): sin triggers de inmutabilidad; el historial solo se inserta desde la aplicación.
