---
id: HU-006
tipo: historia-de-usuario
titulo: "Gestionar especialidades"
estado: Aprobada
epica: "[[EP-003-catalogos]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
relacionadas:
  - "[[HU-008-crear-profesional]]"
  - "[[HU-012-consultar-disponibilidad]]"
---

# HU-006 — Gestionar especialidades

## Historia de usuario

**COMO** ADMIN  
**QUIERO** crear, consultar, editar y activar/desactivar especialidades con su duración  
**PARA** definir qué tipos de cita ofrece el sistema y cuántos slots consumen

> Como ADMIN, quiero crear, consultar, editar y activar/desactivar especialidades con su duración para definir qué tipos de cita ofrece el sistema y cuántos slots consumen.

## Contexto y descripción

Implementa RF-06 (especialidades) y RF-09. Cada especialidad define una duración de 30 o 60 minutos. `Medicina General` debe existir para [[HU-013-reservar-cita-general]].

## Alcance

- CRUD de especialidades para ADMIN.
- Duración 30 o 60 minutos por especialidad.
- Activación/desactivación.
- Vista CRUD de especialidades.

## Fuera de alcance

- Duraciones distintas de 30/60 minutos.
- Sobrescritura de duración por profesional.

## Reglas de negocio

- Solo ADMIN gestiona especialidades.
- Duración permitida: 30 o 60 minutos.
- Nombre de especialidad único.
- No se borra físicamente una especialidad referenciada; se desactiva.
- Una especialidad inactiva no puede reservarse (RN-08).

## Dependencias y relaciones

- Épica: [[EP-003-catalogos]]
- Dependencias: [[HU-001-registro-e-inicio-de-sesion-jwt]]
- Relacionadas: [[HU-008-crear-profesional]], [[HU-012-consultar-disponibilidad]], [[HU-013-reservar-cita-general]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** CRUD con autorización por rol, regla de duración y borrado lógico condicionado a referencias.

## Tareas de desarrollo

- [x] **T-01 — Migración de especialidades**  
  Dificultad: Bajo  
  Descripción: tabla con nombre único, duración restringida, estado activo y seed de `Medicina General`.
- [x] **T-02 — Casos de uso CRUD**  
  Dificultad: Medio  
  Descripción: validaciones de duración y unicidad; desactivación en lugar de borrado cuando hay referencias.
- [x] **T-03 — Autorización ADMIN**  
  Dificultad: Bajo  
  Descripción: restringir escritura a ADMIN.
- [x] **T-04 — Vista CRUD de especialidades**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [x] **T-05 — Pruebas**  
  Dificultad: Medio  
  Descripción: duración inválida, nombre duplicado, rol no autorizado y borrado de especialidad referenciada.

## Criterios de aceptación

### CA-01 — Crear especialidad

**Dado** un ADMIN autenticado  
**Cuando** crea una especialidad con nombre único y duración 30 o 60  
**Entonces** queda registrada y activa

### CA-02 — Duración inválida

**Dado** un ADMIN  
**Cuando** registra una duración distinta de 30 o 60 minutos  
**Entonces** la operación es rechazada con error de validación

### CA-03 — Autorización

**Dado** un USER o PROFESSIONAL  
**Cuando** intenta crear, editar o desactivar una especialidad  
**Entonces** recibe error de acceso denegado

### CA-04 — Especialidad referenciada

**Dado** una especialidad asociada a profesionales o citas  
**Cuando** ADMIN intenta eliminarla  
**Entonces** no se borra físicamente y puede desactivarse

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Migración Flyway presente.
- [x] Pruebas de backend y de autorización en verde.
- [x] Vista CRUD integrada en `citas-web`.
- [x] Contrato REST documentado en `docs/contratos/administracion.md`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Crear especialidad | Cumple | `AdminOfferApiIntegrationTest.ca01_adminCreaUnaEspecialidadActivaConDuracionValida` | Nace activa y no general |
| CA-02 Duración inválida | Cumple | `ca02_unaDuracionDistintaDe30O60SeRechaza` (20, 45, 90 y 0) | 400 con `field: durationMinutes`; `CHECK chk_specialties_duration` como última defensa |
| CA-03 Autorización | Cumple | `ca03_unUsuarioSinRolAdminNoPuedeGestionarEspecialidades` (403), `ca03_sinTokenLaAdministracionDevuelve401` | `/api/v1/admin/**` exige `hasRole('ADMIN')` |
| CA-04 Especialidad referenciada | Cumple | `ca04_unaEspecialidadReferenciadaNoSeBorraPeroSePuedeDesactivar` | No hay DELETE (405); tras desactivar, la fila y la asignación del profesional siguen en BD |
| Unicidad de nombre | Cumple | `elNombreDeEspecialidadEsUnico` | 409 `SPECIALTY_NAME_ALREADY_REGISTERED` |
| Una sola general | Cumple | `medicinaGeneralEstaSembradaComoLaUnicaEspecialidadGeneral` | Índice funcional `uk_specialties_single_general` |
| DoD Migración | Cumple | `V4__oferta_administrable_hu006_hu008.sql` | Semilla `Medicina General` (30 min, general) |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-23: 95 pruebas, 0 fallos | 17 nuevas en `AdminOfferApiIntegrationTest` |
| DoD Vista `citas-web` | Cumple | `SpecialtiesPage.test.tsx` (7 pruebas: listado, crear, nombre repetido, duración, editar, desactivar con confirmación, activar) | `citas-web` `576db18`; `npm test` 95/95, lint y build en verde; verificada contra la API real con capturas autenticadas frente a Stitch v4 (D-026) |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-23 (S3) — Backend implementado y verificado (`mvnw test` 95/95). Pendiente la vista de ADMIN en `citas-web`.

- 2026-09-25 (S3) — Vista integrada en `citas-web` (`citas-web` `576db18`) según el diseño aprobado en Stitch v4. Estado de la HU sin cambios.

## Notas y decisiones

- Supuesto: `Medicina General` se siembra con duración de 30 minutos (a confirmar).
- Supuesto: el tipo de cita general/especializada se deriva de si la especialidad es `Medicina General`.
