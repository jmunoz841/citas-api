---
id: HU-024
tipo: historia-de-usuario
titulo: "Notificación de cambio de estado"
estado: Aprobada
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

- [x] **T-01 — Puerto y adaptador de notificación**  
  Dificultad: Medio  
  Descripción: publicación de eventos tras confirmación de la transacción. `StatusNotificationPort` + `N8nWebhookNotificationAdapter` (firma HMAC); `IntegrationApiIntegrationTest.hu024_*`, `N8nWebhookNotificationAdapterTest`.
- [x] **T-02 — Workflow n8n**  
  Dificultad: Medio  
  Descripción: webhook, plantilla de correo y registro. Creado por MCP: `WF-002 Notificación de cambio de estado — JuanCarlos Muñoz` (12 nodos; Webhook con Header Auth `X-Citas-Token`, validación → 400, Switch por `eventType`, plantilla con escape HTML, Gmail con 3 intentos, registro sin datos personales, respuesta 200/502).
- [x] **T-03 — Exportar JSON y pruebas**  
  Dificultad: Bajo  
  Descripción: prueba de que la falla del webhook no afecta la transición. `automations/n8n/WF-002-status-notifications.json`; `hu024_ca02_siN8nFallaLaTransicionSeCompletaIgual` y demostración en vivo con el webhook de producción inactivo.

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

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] JSON exportado en `automations/n8n/`.
- [x] Pruebas de backend en verde.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | Ejecución n8n 385; `hu024_ca01_aprobarYRechazarUnaCitaEspecializadaEnviaEventosFirmados`, `hu024_ca01_reprogramacionYCancelacionTambienSeNotifican`; prueba offline de las 5 plantillas | En vivo, extremo a extremo: evento real `SPECIALIZED_APPROVED` (cita 6) desde la API → webhook con `X-Citas-Token` → Gmail enviado → `Responder 200`. Los otros 4 eventos: emisión firmada cubierta por las pruebas de backend y plantillas verificadas ejecutando el código exportado (rechazo con motivo, reprogramación con franja anterior/pedida, cancelación). El Product Owner dio por confirmados los 4 envíos restantes sin repetirlos en vivo |
| CA-02 | Cumple | `hu024_ca02_siN8nFallaLaTransicionSeCompletaIgual`, `N8nWebhookNotificationAdapterTest.ca02_n8nInalcanzableNoLanzaNingunaExcepcion`; demostración en vivo | Con el webhook de producción inactivo, rechazar la cita 7 la deja `REJECTED`; la API registra solo `El webhook de n8n respondió 404 al evento <uuid> (SPECIALIZED_REJECTED)`, sin datos personales |
| DoD | Cumple | `WF-002-status-notifications.json`; `mvnw clean test` 210/210; EP-008 | JSON sin IDs de credencial, `webhookId`, URLs de instancia ni emails. Payload incompleto → rama 400 verificada offline (falta `eventId`, `appointment.id`, `patient.email`). Pendiente operativo: activar el workflow en n8n (la activación por MCP la bloquea el control de permisos del agente) |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-10-04 (S5) — HU `Aprobada` explícitamente por el Product Owner. Webhook firmado con HMAC tras confirmar la transacción (D-042).

- 2026-10-04 (S6) — WF-002 creado por MCP; ejecución controlada 385 con la URL de test; CA-02 demostrado en vivo; JSON exportado. `N8N_WEBHOOK_URL` apunta a la URL de producción del webhook.

## Notas y decisiones

- La firma `X-Citas-Signature` no se verifica dentro de n8n (exigiría el secreto en el workflow); la autenticación es la cabecera `X-Citas-Token` validada por la credencial Header Auth. Riesgo residual R-04 en `automations/n8n/SEGURIDAD_S5.md`.
