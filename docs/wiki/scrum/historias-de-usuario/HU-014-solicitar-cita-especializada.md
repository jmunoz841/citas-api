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

- [x] **T-01 — Caso de uso solicitar cita especializada**  
  Dificultad: Medio  
  Descripción: validaciones de especialidad/profesional/sede y retención de slots.
- [x] **T-02 — Flujo especializado en la vista**  
  Dificultad: Medio  
  Descripción: selección de especialidad, sede, profesional y horario.
- [x] **T-03 — Pruebas**  
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

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Pruebas de backend en verde.
- [x] Flujo especializado integrado en `citas-web`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/` (EP-006 sincronizada el 2026-09-25).

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Solicitud creada | Cumple | `BookingApiIntegrationTest.hu014_ca01_citaEspecializadaQuedaSolicitadaYRetieneElSlot` | 201 `REQUESTED`; slot retenido en `slot_reservations`; historial `REQUESTED`; el slot desaparece de la búsqueda |
| CA-02 Retención de 60 minutos | Cumple | `hu014_ca02_sesentaMinutosRetieneDosSlotsQueDesaparecenDeLaBusqueda` | Retiene 08:00 y 08:30; la búsqueda solo ofrece desde 09:00 |
| CA-03 Especialidad no asociada o inactiva | Cumple | `hu014_ca03_especialidadNoAsociadaAlProfesionalSeRechaza`; `hu012_ca04_especialidadInactivaNoAparece` | 400 con `field: specialtyId` y ninguna cita creada; FK `fk_appt_professional_specialty` como última defensa |
| Sede no asignada | Cumple | `unaSedeDondeNoAtiendeElProfesionalSeRechaza` | 400 con `field: siteCode` |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-25: 136 pruebas, 0 fallos | — |
| DoD Flujo en `citas-web` | Cumple | `PatientHomePage.test.tsx` (especialidad enviada como solicitud, resultado "Solicitud enviada") | `citas-web` `576db18`; `npm test` 95/95, lint y build en verde; verificada contra la API real con capturas autenticadas frente a Stitch v4 (D-026) |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-25 (S3) — Backend implementado y verificado sobre el mismo `POST /api/v1/appointments` de HU-013: la especialidad decide el estado inicial (`mvnw test` 136/136). Pendiente el flujo en la vista.

- 2026-09-25 (S3) — Vista integrada en `citas-web` (`citas-web` `576db18`) según el diseño aprobado en Stitch v4. Estado de la HU sin cambios.

## Notas y decisiones

- Resuelto (D-030): las solicitudes `REQUESTED` no expiran en S3.
