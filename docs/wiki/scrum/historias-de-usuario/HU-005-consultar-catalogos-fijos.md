---
id: HU-005
tipo: historia-de-usuario
titulo: "Consultar catálogos fijos"
estado: Aprobada
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

- [ ] **T-01 — Seed de catálogos fijos**  
  Dificultad: Bajo  
  Descripción: migración Flyway con los valores del PRD.
- [ ] **T-02 — Consulta de catálogos**  
  Dificultad: Bajo  
  Descripción: casos de uso y endpoints de lectura.
- [ ] **T-03 — Pruebas**  
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

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Migración Flyway con seed presente.
- [ ] Pruebas de backend en verde.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

## Notas y decisiones

- Incógnita: valores exactos de regímenes (p. ej. contributivo/subsidiado) a confirmar en la normalización.
