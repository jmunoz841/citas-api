---
id: HU-008
tipo: historia-de-usuario
titulo: "Crear profesional con especialidades y sedes"
estado: Aprobada
epica: "[[EP-004-gestion-de-profesionales]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-005-consultar-catalogos-fijos]]"
  - "[[HU-006-gestionar-especialidades]]"
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
relacionadas:
  - "[[HU-009-activar-desactivar-profesional]]"
  - "[[HU-010-gestionar-bloques-de-disponibilidad]]"
---

# HU-008 — Crear profesional con especialidades y sedes

## Historia de usuario

**COMO** ADMIN  
**QUIERO** crear profesionales con sus especialidades y sedes  
**PARA** que puedan publicar agenda y recibir citas

> Como ADMIN, quiero crear profesionales con sus especialidades y sedes para que puedan publicar agenda y recibir citas.

## Contexto y descripción

Implementa RF-07 (creación y asignaciones). El profesional es un usuario con rol `PROFESSIONAL` y datos especializados. Nombres y matrículas son sintéticos.

## Alcance

- Crear usuario `PROFESSIONAL` con credenciales iniciales.
- Registrar código profesional y matrícula ficticia.
- Asignar una o varias especialidades y marcar una primaria.
- Asignar una o ambas sedes.
- Consultar y editar profesionales y sus asignaciones.
- Vista CRUD de profesionales.

## Fuera de alcance

- Activación/desactivación ([[HU-009-activar-desactivar-profesional]]).
- Autoregistro de profesionales.

## Reglas de negocio

- Solo ADMIN crea profesionales.
- Código profesional y matrícula únicos.
- Al menos una especialidad y exactamente una primaria.
- Al menos una sede asignada.
- Especialidades N:M y sedes N:M resueltas con tablas puente (3FN).
- Email y documento únicos, igual que en [[HU-001-registro-e-inicio-de-sesion-jwt]].

## Dependencias y relaciones

- Épica: [[EP-004-gestion-de-profesionales]]
- Dependencias: [[HU-005-consultar-catalogos-fijos]], [[HU-006-gestionar-especialidades]], [[HU-001-registro-e-inicio-de-sesion-jwt]]
- Relacionadas: [[HU-009-activar-desactivar-profesional]], [[HU-010-gestionar-bloques-de-disponibilidad]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** crea usuario y perfil profesional de forma transaccional con dos relaciones N:M y regla de especialidad primaria.

## Tareas de desarrollo

- [x] **T-01 — Migración de profesionales y relaciones**  
  Dificultad: Medio  
  Descripción: perfil profesional, puentes con especialidades (con marca primaria) y sedes.
- [x] **T-02 — Caso de uso crear/editar profesional**  
  Dificultad: Alto  
  Descripción: creación transaccional de usuario + perfil + asignaciones con validaciones.
- [ ] **T-03 — Vista CRUD de profesionales**  
  Dificultad: Medio  
  Descripción: formularios de asignación de especialidades y sedes.
- [x] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: sin especialidad primaria, varias primarias, sin sede, duplicados y autorización.

## Criterios de aceptación

### CA-01 — Creación válida

**Dado** un ADMIN y datos válidos con especialidades activas, una primaria y al menos una sede  
**Cuando** crea el profesional  
**Entonces** existe un usuario `PROFESSIONAL` que puede iniciar sesión, con sus asignaciones

### CA-02 — Especialidad primaria

**Dado** un profesional sin especialidad primaria o con más de una  
**Cuando** se guarda  
**Entonces** la operación es rechazada con error de validación

### CA-03 — Sede obligatoria

**Dado** un profesional sin sedes  
**Cuando** se guarda  
**Entonces** la operación es rechazada

### CA-04 — Unicidad

**Dado** un código profesional, matrícula, email o documento existente  
**Cuando** se crea otro profesional con el mismo valor  
**Entonces** la operación es rechazada con error de conflicto

### CA-05 — Autorización

**Dado** un usuario sin rol ADMIN  
**Cuando** intenta crear o editar profesionales  
**Entonces** recibe acceso denegado

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Migración Flyway presente.
- [x] Pruebas de backend en verde.
- [ ] Vista CRUD integrada en `citas-web`.
- [x] Contrato REST documentado en `docs/contratos/administracion.md`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Creación válida | Cumple | `AdminOfferApiIntegrationTest.ca01_elProfesionalCreadoPuedeIniciarSesionYConservaSusAsignaciones` | Rol `PROFESSIONAL` en BD; inicia sesión con su contraseña temporal; la respuesta no expone contraseña ni hash |
| CA-02 Especialidad primaria | Cumple | `ca02_sinPrimariaOConVariasPrimariasSeRechaza` | 400 con `field: specialties`; índice `uk_prof_specialties_one_primary` como última defensa |
| CA-03 Sede obligatoria | Cumple | `ca03_sinSedesSeRechaza` | 400 |
| CA-04 Unicidad | Cumple | `ca04_codigoMatriculaEmailYDocumentoSonUnicos` | 409 con código distinto para cada caso |
| CA-05 Autorización | Cumple | `ca05_unProfesionalNoPuedeAdministrarProfesionales` | Un PROFESSIONAL recibe 403 |
| Especialidad inactiva | Cumple | `unaEspecialidadInactivaNoSePuedeAsignar` | 400 con `field: specialties` |
| Sede inexistente | Cumple | `unaSedeInexistenteSeRechaza` | 400 con `field: siteCodes` |
| Reasignación | Cumple | `reasignarEspecialidadesYSedesReemplazaLasAnteriores` | `PUT` reemplaza el conjunto completo; la primaria queda donde se indicó |
| DoD Migración | Cumple | `V4__oferta_administrable_hu006_hu008.sql` | `professionals`, `professional_specialties`, `professional_sites` + ADMIN inicial (D-021) |
| DoD Pruebas | Cumple | `mvnw test` 2026-09-23: 95 pruebas, 0 fallos | — |
| DoD Vista `citas-web` | **Pendiente** | — | La vista CRUD de ADMIN llega en el siguiente incremento del frontend |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

- 2026-09-23 (S3) — Backend implementado y verificado (`mvnw test` 95/95). Pendiente la vista de ADMIN en `citas-web`.

## Notas y decisiones

- Incógnita: cómo recibe el profesional su contraseña inicial (definida por ADMIN o flujo de recuperación de [[HU-002-recuperar-contrasena]]).
