---
id: HU-024
tipo: historia-de-usuario
titulo: "Notificación de cambio de estado"
estado: Borrador
epica: "[[EP-008-automatizaciones-n8n]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 5"
dependencias:
  - "[[HU-015-resolver-cita-especializada]]"
  - "[[HU-019-resolver-reprogramacion]]"
  - "[[HU-017-cancelar-cita]]"
relacionadas:
  - "[[HU-023-recordatorios-de-citas]]"
---

# HU-024 — Notificación de cambio de estado

## Historia de usuario

**COMO** USER  
**QUIERO** recibir un correo cuando mi cita o reprogramación cambie de estado  
**PARA** enterarme de decisiones sin revisar la aplicación constantemente

> Como USER, quiero recibir un correo cuando mi cita o reprogramación cambie de estado para enterarme de decisiones sin revisar la aplicación constantemente.

## Contexto y descripción

PRD §10, automatización 2 (S6). Flujo: webhook desde Spring → n8n → Gmail → registro.

## Alcance

- Emisión de evento por webhook en: aprobación/rechazo de cita especializada, aprobación/rechazo de reprogramación y cancelación.
- Workflow n8n con envío Gmail.
- JSON exportado en `automations/n8n/WF-002-status-notifications.json`.

## Fuera de alcance

- Garantía de entrega exactamente una vez.

## Reglas de negocio

- La falla del webhook no revierte la transición de estado en la API.
- El payload no incluye datos sensibles innecesarios ni tokens.
- URL y secreto del webhook por variables de entorno.

## Dependencias y relaciones

- Épica: [[EP-008-automatizaciones-n8n]]
- Dependencias: [[HU-015-resolver-cita-especializada]], [[HU-019-resolver-reprogramacion]], [[HU-017-cancelar-cita]]
- Relacionadas: [[HU-023-recordatorios-de-citas]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** integración saliente desacoplada del flujo transaccional con manejo de errores.

## Tareas de desarrollo

- [ ] **T-01 — Puerto y adaptador de notificación**  
  Dificultad: Medio  
  Descripción: publicación de eventos tras confirmación de la transacción.
- [ ] **T-02 — Workflow n8n**  
  Dificultad: Medio  
  Descripción: webhook, plantilla de correo y registro.
- [ ] **T-03 — Exportar JSON y pruebas**  
  Dificultad: Bajo  
  Descripción: prueba de que la falla del webhook no afecta la transición.

## Criterios de aceptación

### CA-01 — Notificación enviada

**Dado** un evento soportado de cambio de estado  
**Cuando** ocurre  
**Entonces** el USER recibe un correo con el nuevo estado y motivo cuando aplique

### CA-02 — Resiliencia

**Dado** n8n no disponible  
**Cuando** ocurre la transición  
**Entonces** la transición se completa y el error queda registrado sin datos sensibles

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] JSON exportado en `automations/n8n/`.
- [ ] Pruebas de backend en verde.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

## Notas y decisiones

- Ninguna.
