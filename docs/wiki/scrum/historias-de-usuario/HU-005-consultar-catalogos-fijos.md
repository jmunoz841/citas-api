---
id: HU-005
tipo: historia-de-usuario
titulo: "Consultar catálogos fijos"
estado: Completada
epica: "[[EP-003-catalogos]]"
esfuerzo: "Bajo"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
relacionadas:
  - "[[HU-008-crear-profesional]]"
  - "[[HU-010-gestionar-bloques-de-disponibilidad]]"
---

# HU-005 — Consultar catálogos fijos

## Historia de usuario

**COMO** usuario autenticado (USER, PROFESSIONAL o ADMIN)  
**QUIERO** consultar los catálogos fijos del sistema  
**PARA** seleccionar sedes, regímenes y estados válidos en los formularios

> Como usuario autenticado, quiero consultar los catálogos fijos del sistema para seleccionar sedes, regímenes y estados válidos en los formularios.

## Contexto y descripción

Implementa RF-05. Catálogos precargados por seed y de solo lectura: roles, estados de cita, estados de reprogramación, regímenes y sedes (HIC e ICV).

## Alcance

- Seed Flyway de sedes, regímenes, estados de cita y estados de reprogramación (roles ya sembrados en [[HU-001-registro-e-inicio-de-sesion-jwt]]).
- Consulta de lectura de los catálogos.

## Fuera de alcance

- Creación, edición o borrado de catálogos fijos.

## Reglas de negocio

- Los catálogos fijos son de solo lectura desde la API.
- Sedes fijas: Hospital Internacional de Colombia (HIC) e Instituto Cardiovascular (ICV) con las direcciones del PRD.
- Estados de cita modelados de forma coherente (sin textos divergentes).

## Dependencias y relaciones

- Épica: [[EP-003-catalogos]]
- Dependencias: [[HU-001-registro-e-inicio-de-sesion-jwt]]
- Relacionadas: [[HU-008-crear-profesional]], [[HU-010-gestionar-bloques-de-disponibilidad]]

## Esfuerzo

**Nivel:** Bajo

**Justificación de dificultad:** datos estáticos sembrados y endpoints de lectura.

## Tareas de desarrollo

- [x] **T-01 — Seed de catálogos fijos** (2026-09-23: `V2__catalogos_fijos_hu005.sql`)  
  Dificultad: Bajo  
  Descripción: migración Flyway con los valores del PRD.
- [x] **T-02 — Consulta de catálogos** (2026-09-23: 6 endpoints bajo `/api/v1/catalogs`)  
  Dificultad: Bajo  
  Descripción: casos de uso y endpoints de lectura.
- [x] **T-03 — Pruebas** (2026-09-23: 9 pruebas de integración)  
  Dificultad: Bajo  
  Descripción: presencia de valores sembrados e inexistencia de operaciones de escritura.

## Criterios de aceptación

### CA-01 — Sedes disponibles

**Dado** un usuario autenticado  
**Cuando** consulta las sedes  
**Entonces** obtiene exactamente HIC e ICV con sus direcciones

### CA-02 — Catálogos de estados y regímenes

**Dado** un usuario autenticado  
**Cuando** consulta regímenes, estados de cita y estados de reprogramación  
**Entonces** obtiene los valores sembrados

### CA-03 — Solo lectura

**Dado** cualquier rol  
**Cuando** intenta crear, modificar o borrar un catálogo fijo vía API  
**Entonces** la operación no está disponible

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Migración Flyway con seed presente.
- [x] Pruebas de backend en verde.
- [x] Contrato REST documentado en `docs/contratos/catalogos.md`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Sedes | Cumple | `CatalogApiIntegrationTest.ca01_lasSedesSonExactamenteHicEIcvConSuDireccion` | Exactamente HIC e ICV con las direcciones del PRD |
| CA-02 Estados y regímenes | Cumple | `ca02_regimenesDevuelveLosValoresSembrados`, `ca02_estadosDeCitaIncluyenSuMarcaDeTerminal`, `ca02_estadosDeReprogramacionDevuelveLosCuatroValores`, `ca02_rolesYTiposDeDocumentoVienenDeLaMigracionV1` | 2 regímenes, 6 estados de cita, 4 de reprogramación, 3 roles y 6 tipos de documento |
| CA-03 Solo lectura | Cumple | `ca03_unUsuarioAutenticadoTampocoPuedeEscribirEnLosCatalogos` (405), `ca03_sinSesionLaEscrituraNiSiquieraLlegaAlControlador` (401) | No existe ningún manejador de escritura |
| DoD Migración | Cumple | `V2__catalogos_fijos_hu005.sql`, aplicada por Flyway en cada ejecución de Testcontainers | 4 tablas nuevas + seeds |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-23: 64 pruebas, 0 fallos, BUILD SUCCESS | 9 nuevas en `CatalogApiIntegrationTest` |
| DoD Contrato | Cumple | `docs/contratos/catalogos.md` | Falta el consumo desde `citas-web` |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-23 (S3) — Backend implementado y verificado: migración V2, 6 endpoints de lectura y 9 pruebas de integración. Falta el consumo desde `citas-web`.

- 2026-09-25 (S3) — HU `Completada` con confirmación explícita del Product Owner (Juan Muñoz): todos los CA y la DoD en `Cumple` con evidencia. Resumen en [[evidencia-s3]].

## Notas y decisiones

- Los catálogos son **públicos**, sin autenticación: el formulario de registro necesita los tipos de documento y las sedes antes de que exista una sesión. CA-01 describe el caso de un usuario autenticado, que también puede leerlos; no se restringe el acceso anónimo a datos de referencia no sensibles.
- Resuelto: los regímenes sembrados son `CONTRIBUTIVO` y `SUBSIDIADO` (supuesto S-02 del diseño 3FN). La pregunta Q-03 sobre regímenes especiales sigue abierta y no afecta a S3.
- Los endpoints se agrupan bajo `/api/v1/catalogs` (D-018). En el mismo cambio, la autenticación pasó de `/api/auth/*` a `/api/v1/auth/*`.
