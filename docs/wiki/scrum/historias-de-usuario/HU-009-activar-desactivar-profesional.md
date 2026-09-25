---
id: HU-009
tipo: historia-de-usuario
titulo: "Activar y desactivar profesional"
estado: Completada
epica: "[[EP-004-gestion-de-profesionales]]"
esfuerzo: "Bajo"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-008-crear-profesional]]"
relacionadas:
  - "[[HU-012-consultar-disponibilidad]]"
---

# HU-009 — Activar y desactivar profesional

## Historia de usuario

**COMO** ADMIN  
**QUIERO** activar o desactivar un profesional  
**PARA** controlar quién puede ofrecer y recibir citas sin perder su historial

> Como ADMIN, quiero activar o desactivar un profesional para controlar quién puede ofrecer y recibir citas sin perder su historial.

## Contexto y descripción

Implementa la parte de activación/desactivación de RF-07.

## Alcance

- Cambio de estado activo/inactivo del profesional.
- Efecto en disponibilidad visible.

## Fuera de alcance

- Reasignación automática de citas existentes del profesional desactivado.

## Reglas de negocio

- Solo ADMIN cambia el estado.
- Un profesional inactivo no aparece en la búsqueda de disponibilidad ni puede recibir nuevas citas.
- La desactivación no borra datos ni historial.

## Dependencias y relaciones

- Épica: [[EP-004-gestion-de-profesionales]]
- Dependencias: [[HU-008-crear-profesional]]
- Relacionadas: [[HU-012-consultar-disponibilidad]]

## Esfuerzo

**Nivel:** Bajo

**Justificación de dificultad:** cambio de estado con impacto en filtros de consulta.

## Tareas de desarrollo

- [x] **T-01 — Caso de uso activar/desactivar**  
  Dificultad: Bajo  
  Descripción: cambio de estado autorizado para ADMIN.
- [x] **T-02 — Acción en vista de profesionales**  
  Dificultad: Bajo  
  Descripción: control de estado en el CRUD.
- [x] **T-03 — Pruebas**  
  Dificultad: Bajo  
  Descripción: cambio de estado, autorización y exclusión de disponibilidad.

## Criterios de aceptación

### CA-01 — Desactivar

**Dado** un profesional activo  
**Cuando** ADMIN lo desactiva  
**Entonces** su estado cambia a inactivo y sus datos se conservan

### CA-02 — Exclusión de reservas

**Dado** un profesional inactivo  
**Cuando** un USER consulta disponibilidad o intenta reservar  
**Entonces** el profesional no aparece y la reserva es rechazada

### CA-03 — Reactivar

**Dado** un profesional inactivo  
**Cuando** ADMIN lo activa  
**Entonces** vuelve a estar disponible según su agenda

## Definition of Done

- [x] Criterios CA-01 y CA-03 validados con evidencia.
- [x] CA-02 (exclusión de la búsqueda y de la reserva) validado con HU-012 y HU-013.
- [x] Pruebas de backend en verde.
- [x] Control de estado disponible en `citas-web`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Desactivar | Cumple | `AdminOfferApiIntegrationTest.ca01_ca03_desactivarYReactivarConservaDatosYAsignaciones` | `active: false`; especialidades y sedes intactas en BD |
| CA-02 Exclusión de reservas | Cumple | `BookingApiIntegrationTest.hu009_ca02_profesionalInactivoNoApareceYNoSePuedeReservar` | Con agenda publicada: aparece en la búsqueda; tras desactivarlo, 0 resultados y la reserva responde 400 con `field: professionalId` |
| Desactivar con agenda | Cumple | `hu009_ca02_...`; `reasignarEspecialidadesYSedesDeUnProfesionalConCitasConservaLasQueSiguen` | Corregido 2026-09-25: guardar el profesional borraba y reinsertaba sus asignaciones, y la FK RESTRICT de `availability_blocks` devolvía 500 al desactivar a un profesional con bloques. Ahora se sincroniza por diferencia |
| CA-03 Reactivar | Cumple | `ca01_ca03_desactivarYReactivarConservaDatosYAsignaciones` | Vuelve a `active: true` con sus asignaciones |
| Profesional inexistente | Cumple | `unProfesionalInexistenteDevuelve404` | 404 `NOT_FOUND` |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-23: 95 pruebas, 0 fallos | — |
| DoD Control en `citas-web` | Cumple | `ProfessionalsPage.test.tsx` (HU-009 CA-01 desactivar con confirmación, CA-03 activar) | `citas-web` `576db18`; `npm test` 95/95, lint y build en verde; verificada contra la API real con capturas autenticadas frente a Stitch v4 (D-026) |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-23 (S3) — Backend implementado y verificado (`mvnw test` 95/95). Pendiente la vista de ADMIN en `citas-web`.

- 2026-09-25 (S3) — CA-02 validado con la búsqueda y la reserva (`mvnw test` 136/136). Corregido el 500 al desactivar un profesional con agenda. Pendiente la vista de ADMIN.

- 2026-09-25 (S3) — Vista integrada en `citas-web` (`citas-web` `576db18`) según el diseño aprobado en Stitch v4. Estado de la HU sin cambios.

- 2026-09-25 (S3) — HU `Completada` con confirmación explícita del Product Owner (Juan Muñoz): todos los CA y la DoD en `Cumple` con evidencia. Resumen en [[evidencia-s3]].

## Notas y decisiones

- Incógnita: si un profesional inactivo puede seguir iniciando sesión para consultar su agenda histórica.
- Resuelto (D-029): desactivar no cancela las citas ya reservadas; cancelarlas queda para S4.
