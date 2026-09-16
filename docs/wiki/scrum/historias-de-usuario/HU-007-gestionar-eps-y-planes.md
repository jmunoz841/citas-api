---
id: HU-007
tipo: historia-de-usuario
titulo: "Gestionar EPS y planes"
estado: Borrador
epica: "[[EP-003-catalogos]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
relacionadas:
  - "[[HU-004-registrar-afiliacion]]"
---

# HU-007 — Gestionar EPS y planes

## Historia de usuario

**COMO** ADMIN  
**QUIERO** crear, consultar, editar y activar/desactivar EPS y sus planes  
**PARA** que los usuarios puedan registrar afiliaciones válidas

> Como ADMIN, quiero crear, consultar, editar y activar/desactivar EPS y sus planes para que los usuarios puedan registrar afiliaciones válidas.

## Contexto y descripción

Implementa RF-06 para EPS y planes de EPS. Los datos son sintéticos.

## Alcance

- CRUD de EPS.
- CRUD de planes asociados a una EPS.
- Activación/desactivación.
- Vista CRUD EPS/planes.

## Fuera de alcance

- Integración con registros oficiales de EPS.

## Reglas de negocio

- Solo ADMIN gestiona EPS y planes.
- Un plan pertenece a exactamente una EPS.
- Nombre de EPS único; nombre de plan único dentro de su EPS.
- No se borra físicamente un catálogo referenciado; se desactiva.

## Dependencias y relaciones

- Épica: [[EP-003-catalogos]]
- Dependencias: [[HU-001-registro-e-inicio-de-sesion-jwt]]
- Relacionadas: [[HU-004-registrar-afiliacion]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** dos catálogos relacionados 1:N con unicidad compuesta y borrado lógico.

## Tareas de desarrollo

- [ ] **T-01 — Migración EPS y planes**  
  Dificultad: Bajo  
  Descripción: tablas relacionadas con restricciones únicas y estado activo.
- [ ] **T-02 — Casos de uso CRUD**  
  Dificultad: Medio  
  Descripción: validaciones, unicidad y desactivación.
- [ ] **T-03 — Vista CRUD EPS/planes**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [ ] **T-04 — Pruebas**  
  Dificultad: Medio  
  Descripción: duplicados, autorización y catálogo referenciado.

## Criterios de aceptación

### CA-01 — Crear EPS y plan

**Dado** un ADMIN autenticado  
**Cuando** crea una EPS y un plan asociado  
**Entonces** ambos quedan registrados y activos

### CA-02 — Duplicados

**Dado** una EPS existente o un plan existente en la misma EPS  
**Cuando** se intenta crear otro con el mismo nombre  
**Entonces** la operación es rechazada con error de conflicto

### CA-03 — Autorización

**Dado** un usuario sin rol ADMIN  
**Cuando** intenta modificar EPS o planes  
**Entonces** recibe error de acceso denegado

### CA-04 — Catálogo referenciado

**Dado** una EPS o plan usado en afiliaciones  
**Cuando** ADMIN intenta eliminarlo  
**Entonces** no se borra físicamente y puede desactivarse

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
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

## Notas y decisiones

- Ninguna.
