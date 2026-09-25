---
id: HU-012
tipo: historia-de-usuario
titulo: "Consultar disponibilidad"
estado: Aprobada
epica: "[[EP-006-reserva-de-citas]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-010-gestionar-bloques-de-disponibilidad]]"
  - "[[HU-006-gestionar-especialidades]]"
relacionadas:
  - "[[HU-013-reservar-cita-general]]"
  - "[[HU-014-solicitar-cita-especializada]]"
  - "[[HU-009-activar-desactivar-profesional]]"
---

# HU-012 — Consultar disponibilidad

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** buscar horarios disponibles filtrando por sede, tipo de cita, especialidad, profesional y fecha  
**PARA** elegir un horario que realmente pueda reservar

> Como USER autenticado, quiero buscar horarios disponibles filtrando por sede, tipo de cita, especialidad, profesional y fecha para elegir un horario que realmente pueda reservar.

## Contexto y descripción

Implementa RF-10 y aplica RF-09. Solo se muestran horarios que puedan completar la duración de la especialidad (1 slot para 30 min, 2 consecutivos para 60 min).

## Alcance

- Filtros: sede, tipo general/especializada, especialidad, profesional, fecha.
- Cálculo de horarios reservables según duración.
- Vista "buscar disponibilidad".

## Fuera de alcance

- Creación de la cita ([[HU-013-reservar-cita-general]], [[HU-014-solicitar-cita-especializada]]).

## Reglas de negocio

- Se excluyen slots reservados o retenidos (RN-01).
- Para 60 min solo se muestran inicios con 2 slots consecutivos libres en el mismo bloque/sede (RN-05).
- No se muestran horarios pasados (RN-06).
- Solo profesionales activos con la especialidad activa asociada (RN-08).

## Dependencias y relaciones

- Épica: [[EP-006-reserva-de-citas]]
- Dependencias: [[HU-010-gestionar-bloques-de-disponibilidad]], [[HU-006-gestionar-especialidades]]
- Relacionadas: [[HU-013-reservar-cita-general]], [[HU-014-solicitar-cita-especializada]], [[HU-009-activar-desactivar-profesional]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** consulta combinada con múltiples filtros y cálculo de slots consecutivos libres.

## Tareas de desarrollo

- [x] **T-01 — Servicio de dominio de disponibilidad**  
  Dificultad: Alto  
  Descripción: cálculo de inicios reservables según duración y estado de slots.
- [x] **T-02 — Consulta con filtros**  
  Dificultad: Medio  
  Descripción: caso de uso y endpoint con filtros combinables e índices adecuados.
- [ ] **T-03 — Vista de búsqueda**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [x] **T-04 — Pruebas de slots 30/60**  
  Dificultad: Medio  
  Descripción: huecos de un slot, slots consecutivos, pasado, profesional/especialidad inactivos.

## Criterios de aceptación

### CA-01 — Especialidad de 30 minutos

**Dado** un bloque con slots libres y reservados  
**Cuando** se busca disponibilidad para una especialidad de 30 min  
**Entonces** se muestran solo los slots libres

### CA-02 — Especialidad de 60 minutos

**Dado** un bloque 08:00–10:00 con el slot 08:30 reservado  
**Cuando** se busca disponibilidad para una especialidad de 60 min  
**Entonces** solo se ofrece 09:00

### CA-03 — Filtros

**Dado** disponibilidad en varias sedes, especialidades y profesionales  
**Cuando** se aplican filtros  
**Entonces** los resultados cumplen todos los filtros aplicados

### CA-04 — Exclusiones

**Dado** horarios pasados, profesionales inactivos o especialidades inactivas  
**Cuando** se busca disponibilidad  
**Entonces** no aparecen en los resultados

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Pruebas de reglas de slots 30/60 en verde.
- [ ] Vista de búsqueda integrada en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 30 minutos | Cumple | `SlotPlannerTest.ca01_treintaMinutosOfreceSoloLosSlotsLibres`; `BookingApiIntegrationTest.hu012_ca01_treintaMinutosMuestraSoloSlotsLibres` | Bloque 08:00–10:00 con 08:30 reservado → 08:00, 09:00, 09:30 |
| CA-02 60 minutos | Cumple | `SlotPlannerTest.ca02_sesentaMinutosConEl0830OcupadoSoloOfrece0900`; `hu012_ca02_sesentaMinutosConEl0830ReservadoSoloOfrece0900` | Solo 09:00–10:00. Además: el último slot de un bloque no inicia una cita de 60 y dos bloques contiguos no se combinan |
| CA-03 Filtros | Cumple | `hu012_ca03_losResultadosCumplenTodosLosFiltros` | `specialtyId`, `siteCode`, `professionalId` y `type` combinables |
| CA-04 Exclusiones | Cumple | `hu012_ca04_especialidadInactivaNoAparece`; `hu012_ca04_horariosPasadosNoAparecen`; `hu009_ca02_profesionalInactivoNoApareceYNoSePuedeReservar`; `SlotPlannerTest.ca04_losHorariosPasadosNoSeOfrecen` | Pasado, profesional inactivo y especialidad inactiva excluidos |
| Parámetros | Cumple | `laFechaEsObligatoriaYElTipoDebeSerValido` | Sin `date` o con `type` desconocido → 400 con el campo |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-25: 136 pruebas, 0 fallos | 7 de dominio + integración contra MySQL 8.4 |
| DoD Vista `citas-web` | **Pendiente** | — | Llega con el modal de reserva de la pasada de frontend |
| Contrato | Cumple | `docs/contratos/citas.md` | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-25 (S3) — Backend implementado y verificado: `GET /api/v1/availability`, `SlotPlanner` en el dominio, `mvnw test` 136/136. Pendiente la vista de búsqueda.

## Notas y decisiones

- Supuesto: dos slots de bloques contiguos distintos no se combinan para una cita de 60 min.
