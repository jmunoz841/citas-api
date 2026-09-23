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

- [ ] **T-01 — Migración de profesionales y relaciones**  
  Dificultad: Medio  
  Descripción: perfil profesional, puentes con especialidades (con marca primaria) y sedes.
- [ ] **T-02 — Caso de uso crear/editar profesional**  
  Dificultad: Alto  
  Descripción: creación transaccional de usuario + perfil + asignaciones con validaciones.
- [ ] **T-03 — Vista CRUD de profesionales**  
  Dificultad: Medio  
  Descripción: formularios de asignación de especialidades y sedes.
- [ ] **T-04 — Pruebas**  
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

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Migración Flyway presente.
- [ ] Pruebas de backend en verde.
- [ ] Vista CRUD integrada en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| CA-04 | Pendiente | — | — |
| CA-05 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

## Notas y decisiones

- Incógnita: cómo recibe el profesional su contraseña inicial (definida por ADMIN o flujo de recuperación de [[HU-002-recuperar-contrasena]]).
