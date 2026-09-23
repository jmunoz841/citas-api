---
id: HU-009
tipo: historia-de-usuario
titulo: "Activar y desactivar profesional"
estado: Borrador
epica: "[[EP-004-gestion-de-profesionales]]"
esfuerzo: "Bajo"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-008-crear-profesional]]"
relacionadas:
  - "[[HU-012-consultar-disponibilidad]]"
---

# HU-009 — Activar y desactivar profesional

## Historia de usuario

**COMO** ADMIN  
**QUIERO** activar o desactivar un profesional  
**PARA** controlar quién puede ofrecer y recibir citas sin perder su historial

> Como ADMIN, quiero activar o desactivar un profesional para controlar quién puede ofrecer y recibir citas sin perder su historial.

## Contexto y descripción

Implementa la parte de activación/desactivación de RF-07.

## Alcance

- Cambio de estado activo/inactivo del profesional.
- Efecto en disponibilidad visible.

## Fuera de alcance

- Reasignación automática de citas existentes del profesional desactivado.

## Reglas de negocio

- Solo ADMIN cambia el estado.
- Un profesional inactivo no aparece en la búsqueda de disponibilidad ni puede recibir nuevas citas.
- La desactivación no borra datos ni historial.

## Dependencias y relaciones

- Épica: [[EP-004-gestion-de-profesionales]]
- Dependencias: [[HU-008-crear-profesional]]
- Relacionadas: [[HU-012-consultar-disponibilidad]]

## Esfuerzo

**Nivel:** Bajo

**Justificación de dificultad:** cambio de estado con impacto en filtros de consulta.

## Tareas de desarrollo

- [ ] **T-01 — Caso de uso activar/desactivar**  
  Dificultad: Bajo  
  Descripción: cambio de estado autorizado para ADMIN.
- [ ] **T-02 — Acción en vista de profesionales**  
  Dificultad: Bajo  
  Descripción: control de estado en el CRUD.
- [ ] **T-03 — Pruebas**  
  Dificultad: Bajo  
  Descripción: cambio de estado, autorización y exclusión de disponibilidad.

## Criterios de aceptación

### CA-01 — Desactivar

**Dado** un profesional activo  
**Cuando** ADMIN lo desactiva  
**Entonces** su estado cambia a inactivo y sus datos se conservan

### CA-02 — Exclusión de reservas

**Dado** un profesional inactivo  
**Cuando** un USER consulta disponibilidad o intenta reservar  
**Entonces** el profesional no aparece y la reserva es rechazada

### CA-03 — Reactivar

**Dado** un profesional inactivo  
**Cuando** ADMIN lo activa  
**Entonces** vuelve a estar disponible según su agenda

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Control de estado disponible en `citas-web`.
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

- Incógnita: si un profesional inactivo puede seguir iniciando sesión para consultar su agenda histórica.
- CA-02 solo es verificable una vez exista [[HU-012-consultar-disponibilidad]].
