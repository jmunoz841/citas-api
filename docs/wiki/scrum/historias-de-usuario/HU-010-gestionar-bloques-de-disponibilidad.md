---
id: HU-010
tipo: historia-de-usuario
titulo: "Gestionar bloques de disponibilidad"
estado: Aprobada
epica: "[[EP-005-agenda-del-profesional]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-008-crear-profesional]]"
relacionadas:
  - "[[HU-012-consultar-disponibilidad]]"
  - "[[HU-011-consultar-agenda-del-profesional]]"
---

# HU-010 — Gestionar bloques de disponibilidad

## Historia de usuario

**COMO** PROFESSIONAL  
**QUIERO** crear, editar, eliminar y consultar mis bloques de disponibilidad por sede  
**PARA** publicar los horarios en los que puedo atender citas

> Como PROFESSIONAL, quiero crear, editar, eliminar y consultar mis bloques de disponibilidad por sede para publicar los horarios en los que puedo atender citas.

## Contexto y descripción

Implementa RF-08. Un día puede tener varios bloques (p. ej. 08:00–12:00 HIC y 14:00–17:00 HIC). Cada bloque se discretiza en slots de 30 minutos.

## Alcance

- Crear múltiples bloques por día con sede.
- Discretización en slots de 30 minutos.
- Editar/eliminar bloques futuros sin citas comprometidas.
- Consultar calendario propio de bloques.
- Vista de gestión de bloques/calendario.

## Fuera de alcance

- Bloques recurrentes/plantillas semanales.
- Consulta de citas en la agenda ([[HU-011-consultar-agenda-del-profesional]]).

## Reglas de negocio

- No se crean bloques en el pasado (RN-06).
- No se solapan bloques del mismo profesional, incluso en sedes distintas.
- El profesional debe estar asignado a la sede del bloque (RN-07).
- Los límites del bloque deben alinearse a slots de 30 minutos.
- No se edita ni elimina un bloque con slots reservados/retenidos.
- Un profesional solo gestiona sus propios bloques.

## Dependencias y relaciones

- Épica: [[EP-005-agenda-del-profesional]]
- Dependencias: [[HU-008-crear-profesional]]
- Relacionadas: [[HU-012-consultar-disponibilidad]], [[HU-011-consultar-agenda-del-profesional]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** reglas temporales (pasado, solapamiento, alineación), generación de slots y restricciones de edición según reservas.

## Tareas de desarrollo

- [x] **T-01 — Migración de bloques y slots**  
  Dificultad: Medio  
  Descripción: modelo de bloque por profesional/sede/fecha y representación de slots con índices para agenda.
- [x] **T-02 — Reglas de dominio de bloques**  
  Dificultad: Alto  
  Descripción: pasado, solapamiento, sede habilitada, alineación a 30 minutos.
- [x] **T-03 — Casos de uso CRUD de bloques**  
  Dificultad: Medio  
  Descripción: creación con slots, edición/eliminación condicionada.
- [ ] **T-04 — Vista de calendario de bloques**  
  Dificultad: Alto  
  Descripción: según diseño aprobado.
- [x] **T-05 — Pruebas**  
  Dificultad: Medio  
  Descripción: cada regla de negocio y ownership.

## Criterios de aceptación

### CA-01 — Varios bloques en un día

**Dado** un PROFESSIONAL asignado a HIC  
**Cuando** crea 08:00–12:00 y 14:00–17:00 en HIC para una fecha futura  
**Entonces** ambos bloques quedan registrados con 8 y 6 slots de 30 minutos respectivamente

### CA-02 — Bloque en el pasado

**Dado** un PROFESSIONAL  
**Cuando** crea un bloque con inicio en el pasado  
**Entonces** la operación es rechazada

### CA-03 — Solapamiento

**Dado** un bloque existente 08:00–12:00  
**Cuando** el mismo profesional crea 11:00–13:00 el mismo día en cualquier sede  
**Entonces** la operación es rechazada

### CA-04 — Sede no asignada

**Dado** un profesional no asignado a ICV  
**Cuando** crea un bloque en ICV  
**Entonces** la operación es rechazada

### CA-05 — Bloque con citas

**Dado** un bloque con al menos un slot reservado o retenido  
**Cuando** el profesional intenta editarlo o eliminarlo  
**Entonces** la operación es rechazada

### CA-06 — Ownership

**Dado** un PROFESSIONAL  
**Cuando** intenta modificar bloques de otro profesional  
**Entonces** recibe acceso denegado

## Definition of Done

- [x] Criterios CA-01, CA-02, CA-03, CA-04 y CA-06 validados con evidencia.
- [x] CA-05 (bloque con slots comprometidos) validado con `slot_reservations` de HU-013.
- [x] Migración Flyway presente con índices para consultas de agenda.
- [x] Pruebas de reglas de bloques en verde.
- [ ] Vista de bloques/calendario integrada en `citas-web`.
- [x] Contrato REST documentado en `docs/contratos/disponibilidad.md`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Varios bloques en un día | Cumple | `AvailabilityApiIntegrationTest.ca01_dosBloquesEnUnDiaGeneranSusSlotsDe30Minutos` | 08:00–12:00 → 8 slots y 14:00–17:00 → 6; 14 filas en `availability_slots`, todas alineadas a :00 o :30 |
| CA-02 Bloque en el pasado | Cumple | `ca02_unBloqueEnElPasadoSeRechaza` | 400 con `field: startTime`; la regla vive en la aplicación porque MySQL no admite `NOW()` en un CHECK |
| CA-03 Solapamiento | Cumple | `ca03_unBloqueQueSeCruzaConOtroSeRechazaAunEnOtraSede`; `bloquesContiguosNoSeConsideranSolapados` | Rechaza 11:00–13:00 sobre 08:00–12:00, también en otra sede; 12:00–14:00 sí se acepta |
| CA-04 Sede no asignada | Cumple | `ca04_unaSedeNoAsignadaSeRechaza` | 400 con `field: siteCode`; FK compuesta `fk_blocks_professional_site` como última defensa |
| CA-05 Bloque con citas | Cumple | `BookingApiIntegrationTest.hu010_ca05_unBloqueConCitasNoSeEditaNiSeElimina`; `hu010_ca05_laFkDeLaBaseImpideBorrarLosSlotsDeUnBloqueConCitas` | `PATCH` y `DELETE` → 409 `BLOCK_HAS_APPOINTMENTS` y los 4 slots siguen en BD. Guardián en `AvailabilityService.requireNoCommittedSlots`; si una reserva se cuela después, la FK `fk_sr_slot` RESTRICT impide el borrado directo en la base |
| CA-06 Ownership | Cumple | `ca06_nadiePuedeTocarLosBloquesDeOtroProfesional`; `unUsuarioSinRolProfesionalNoAccedeALaAgenda` | Bloque ajeno → 404 (no se revela que existe) y la fila sigue intacta; sin rol → 403; sin token → 401 |
| Alineación y rango | Cumple | `lasHorasDebenCaerEnPuntoOYMedia`, `elFinDebeSerPosteriorAlInicio` | 08:15 y 12:45 rechazados |
| Edición y borrado | Cumple | `editarUnBloqueRegeneraSusSlots`, `eliminarUnBloqueSeLlevaSusSlots` | Editar 08:00–12:00 → 09:00–10:30 deja 3 slots; borrar arrastra los slots por `ON DELETE CASCADE` |
| Calendario propio | Cumple | `elCalendarioPropioSeFiltraPorDiaYSede` | Filtros `date` y `siteCode` |
| Profesional inactivo | Cumple | `unProfesionalInactivoNoPuedePublicarAgenda` | 400 con `field: professional` |
| DoD Migración | Cumple | `V5__disponibilidad_hu010.sql` | Índices `idx_blocks_professional_site` e `idx_blocks_site_start` |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-23: 108 pruebas, 0 fallos | 13 nuevas |
| DoD Vista `citas-web` | **Pendiente** | — | Llega con la pasada de frontend |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-23 (S3) — Backend implementado y verificado: migración V5, 4 endpoints y 13 pruebas de integración (`mvnw test` 108/108). Pendientes CA-05 (necesita HU-013) y la vista de calendario.

- 2026-09-25 (S3) — CA-05 validado con las reservas de HU-013 (`mvnw test` 136/136). Pendiente la vista de calendario.

## Notas y decisiones

- Resuelto (D-013): los slots se materializan como filas al crear el bloque. Es lo que permite que la ocupación sea una fila con clave primaria por slot y que la doble reserva la impida la base.
