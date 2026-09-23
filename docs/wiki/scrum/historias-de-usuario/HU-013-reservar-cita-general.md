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

- [ ] **T-01 — Migración de citas, slots reservados e historial**  
  Dificultad: Alto  
  Descripción: modelo de cita, vínculo con slots con restricción que impida doble reserva, historial de estados.
- [ ] **T-02 — Caso de uso reservar cita general**  
  Dificultad: Alto  
  Descripción: validación, reserva transaccional y registro de historial.
- [ ] **T-03 — Vista de solicitud de cita**  
  Dificultad: Medio  
  Descripción: confirmación y manejo de "horario ya no disponible".
- [ ] **T-04 — Pruebas**  
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

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Migración Flyway presente.
- [ ] Prueba de doble reserva en verde.
- [ ] Vista de solicitud integrada en `citas-web`.
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

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

## Notas y decisiones

- Decisión pendiente (normalización): mecanismo de prevención de doble reserva (restricción única por slot activo, bloqueo pesimista u optimista).
