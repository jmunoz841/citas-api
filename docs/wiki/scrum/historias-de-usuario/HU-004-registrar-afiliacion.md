---
id: HU-004
tipo: historia-de-usuario
titulo: "Registrar afiliación EPS"
estado: Borrador
epica: "[[EP-002-perfil-y-afiliacion]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-003-consultar-y-actualizar-perfil]]"
  - "[[HU-007-gestionar-eps-y-planes]]"
  - "[[HU-005-consultar-catalogos-fijos]]"
relacionadas: []
---

# HU-004 — Registrar afiliación EPS

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** asociar mi EPS, plan y régimen  
**PARA** que mi afiliación quede registrada en el sistema

> Como USER autenticado, quiero asociar mi EPS, plan y régimen para que mi afiliación quede registrada en el sistema.

## Contexto y descripción

Implementa la segunda parte de RF-04. La afiliación referencia catálogos (EPS, plan, régimen) en lugar de duplicar nombres (3FN).

## Alcance

- Registrar/actualizar afiliación del USER.
- Selección desde catálogos activos.

## Fuera de alcance

- Validación contra sistemas reales de EPS.

## Reglas de negocio

- La afiliación referencia EPS, plan y régimen por clave; no se duplican sus nombres en el usuario.
- El plan seleccionado debe pertenecer a la EPS seleccionada.
- No se permiten afiliaciones duplicadas (misma EPS/plan/régimen) para el mismo usuario.
- Solo se pueden seleccionar EPS/planes activos.

## Dependencias y relaciones

- Épica: [[EP-002-perfil-y-afiliacion]]
- Dependencias: [[HU-003-consultar-y-actualizar-perfil]], [[HU-007-gestionar-eps-y-planes]], [[HU-005-consultar-catalogos-fijos]]
- Relacionadas: ninguna

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** relaciona varios catálogos con reglas de pertenencia y unicidad.

## Tareas de desarrollo

- [ ] **T-01 — Migración de afiliación**  
  Dificultad: Bajo  
  Descripción: relación usuario–EPS/plan/régimen con restricción de unicidad.
- [ ] **T-02 — Caso de uso de afiliación**  
  Dificultad: Medio  
  Descripción: validación de pertenencia plan–EPS, catálogos activos y duplicados.
- [ ] **T-03 — Vista de afiliación**  
  Dificultad: Medio  
  Descripción: selección dependiente EPS → plan.
- [ ] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: afiliación válida, plan de otra EPS, duplicado y catálogo inactivo.

## Criterios de aceptación

### CA-01 — Afiliación válida

**Dado** un USER autenticado y EPS/plan/régimen activos y coherentes  
**Cuando** registra su afiliación  
**Entonces** queda asociada a su perfil

### CA-02 — Plan de otra EPS

**Dado** un plan que no pertenece a la EPS seleccionada  
**Cuando** se registra la afiliación  
**Entonces** es rechazada con error de validación

### CA-03 — Afiliación duplicada

**Dado** un USER con una afiliación existente  
**Cuando** intenta registrar la misma combinación  
**Entonces** es rechazada sin duplicar registros

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Migración Flyway de afiliación presente.
- [ ] Pruebas de backend en verde.
- [ ] Vista de afiliación integrada en `citas-web`.
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

## Notas y decisiones

- Incógnita: si un USER puede tener más de una afiliación simultánea o solo una vigente. Se resolverá en la normalización 3FN.
