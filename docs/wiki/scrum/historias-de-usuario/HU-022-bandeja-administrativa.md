---
id: HU-022
tipo: historia-de-usuario
titulo: "Bandeja administrativa con filtros"
estado: Borrador
epica: "[[EP-006-reserva-de-citas]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-015-resolver-cita-especializada]]"
  - "[[HU-019-resolver-reprogramacion]]"
relacionadas: []
---

# HU-022 — Bandeja administrativa con filtros

## Historia de usuario

**COMO** ADMIN  
**QUIERO** una bandeja con citas especializadas solicitadas y reprogramaciones pendientes, filtrable por sede, profesional, especialidad y fecha  
**PARA** priorizar y resolver solicitudes de forma eficiente

> Como ADMIN, quiero una bandeja con citas especializadas solicitadas y reprogramaciones pendientes, filtrable por sede, profesional, especialidad y fecha para priorizar y resolver solicitudes de forma eficiente.

## Contexto y descripción

Implementa RF-18 de forma completa sobre las capacidades de resolución ya existentes.

## Alcance

- Listado unificado o por pestañas de `REQUESTED` y `PENDING`.
- Filtros por sede, profesional, especialidad y fecha.
- Acceso directo a las acciones de resolución.

## Fuera de alcance

- Resolución en lote.

## Reglas de negocio

- Solo ADMIN accede.
- Los filtros son combinables.

## Dependencias y relaciones

- Épica: [[EP-006-reserva-de-citas]]
- Dependencias: [[HU-015-resolver-cita-especializada]], [[HU-019-resolver-reprogramacion]]
- Relacionadas: ninguna

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** consultas filtradas sobre dos tipos de solicitud con catálogos relacionados.

## Tareas de desarrollo

- [ ] **T-01 — Consultas filtradas**  
  Dificultad: Medio  
  Descripción: citas `REQUESTED` y reprogramaciones `PENDING` con filtros e índices.
- [ ] **T-02 — Vista de bandeja**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [ ] **T-03 — Pruebas**  
  Dificultad: Bajo  
  Descripción: combinaciones de filtros y autorización.

## Criterios de aceptación

### CA-01 — Contenido de la bandeja

**Dado** citas `REQUESTED` y reprogramaciones `PENDING`  
**Cuando** ADMIN abre la bandeja  
**Entonces** ve ambas y no ve solicitudes ya resueltas

### CA-02 — Filtros

**Dado** solicitudes en varias sedes, profesionales, especialidades y fechas  
**Cuando** ADMIN aplica filtros  
**Entonces** los resultados cumplen todos los filtros

### CA-03 — Autorización

**Dado** un usuario sin rol ADMIN  
**Cuando** accede a la bandeja  
**Entonces** recibe acceso denegado

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Bandeja integrada en `citas-web`.
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

- Ninguna.
