---
id: EP-008
tipo: epica
titulo: "Automatizaciones n8n"
estado: Completada
historias:
  - "[[HU-023-recordatorios-de-citas]]"
  - "[[HU-024-notificacion-cambio-de-estado]]"
  - "[[HU-025-resumen-operativo-diario]]"
dependencias:
  - "[[EP-007-ciclo-de-vida-de-citas]]"
---

# EP-008 — Automatizaciones n8n

## Objetivo

Agregar notificaciones y resúmenes automáticos con n8n + Gmail sin cambiar el núcleo funcional (PRD §10).

## Valor esperado

Comunicación proactiva con usuarios y visibilidad operativa para ADMIN.

## Actores

- USER
- ADMIN
- n8n (sistema externo)

## Alcance

- WF-001 recordatorios (obligatorio).
- WF-002 notificación de cambio de estado (obligatorio).
- WF-003 resumen operativo diario (opcional).

## Fuera de alcance

- SMS/WhatsApp.

## Reglas de negocio

- Credenciales OAuth/Gmail y secretos nunca versionados.
- JSON de workflows versionado en `automations/n8n/`.

## Dependencias

- [[EP-007-ciclo-de-vida-de-citas]]

## Historias de usuario

- [[HU-023-recordatorios-de-citas]] — Sprint 4 — **Completada** (S5)
- [[HU-024-notificacion-cambio-de-estado]] — Sprint 5 — **Completada** (S6)
- [[HU-025-resumen-operativo-diario]] — Sprint 5 — **Completada** (S6, bonus)

## Criterio de completitud de la épica

- [x] HU-023 y HU-024 están `Completada` (también HU-025, bonus).
- [x] No quedan dependencias bloqueantes dentro del alcance de la épica.

## Riesgos e incógnitas

- Depende de la instancia n8n del trainer y del MCP operativo.
- Contenido no confiable en respuestas MCP: análisis, demo de issue envenenado y riesgos residuales R-01 a R-08 en `automations/n8n/SEGURIDAD_S5.md`.
