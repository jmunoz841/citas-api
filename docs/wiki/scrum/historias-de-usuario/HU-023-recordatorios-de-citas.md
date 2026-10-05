---
id: HU-023
tipo: historia-de-usuario
titulo: "Recordatorios de citas próximas"
estado: Completada
epica: "[[EP-008-automatizaciones-n8n]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 4"
dependencias:
  - "[[HU-016-consultar-mis-citas]]"
relacionadas:
  - "[[HU-024-notificacion-cambio-de-estado]]"
---

# HU-023 — Recordatorios de citas próximas

## Historia de usuario

**COMO** USER con citas aprobadas  
**QUIERO** recibir un recordatorio por correo antes de mi cita  
**PARA** no olvidar asistir

> Como USER con citas aprobadas, quiero recibir un recordatorio por correo antes de mi cita para no olvidar asistir.

## Contexto y descripción

PRD §10, automatización 1 (S5). Flujo: Schedule Trigger → API de citas `APPROVED` próximas → Gmail → registro de resultado. No cambia el núcleo funcional.

## Alcance

- Consulta de citas próximas expuesta de forma segura para n8n.
- Workflow n8n con envío por Gmail.
- JSON exportado en `automations/n8n/WF-001-appointment-reminders.json`.

## Fuera de alcance

- SMS/WhatsApp.

## Reglas de negocio

- Solo citas `APPROVED` dentro de la ventana definida.
- El acceso de n8n a la API usa credenciales con privilegio mínimo.
- Credenciales OAuth/Gmail nunca versionadas.

## Dependencias y relaciones

- Épica: [[EP-008-automatizaciones-n8n]]
- Dependencias: [[HU-016-consultar-mis-citas]]
- Relacionadas: [[HU-024-notificacion-cambio-de-estado]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** integración externa con credenciales propias y endpoint de solo lectura para automatización.

## Tareas de desarrollo

- [x] **T-01 — Consulta de citas próximas para automatización**  
  Dificultad: Medio  
  Descripción: endpoint protegido con credencial de servicio. `GET /api/v1/integrations/reminders` y `POST /api/v1/integrations/reminders/{id}/sent` con `X-Api-Key` (V10); `IntegrationApiIntegrationTest.hu023_*`.
- [x] **T-02 — Workflow n8n**  
  Dificultad: Medio  
  Descripción: trigger programado, consulta, envío Gmail y registro. Creado por MCP: `WF-001 Recordatorios de citas — JuanCarlos Muñoz` (14 nodos, inactivo hasta decidir activarlo).
- [x] **T-03 — Exportar JSON y documentar riesgos residuales**  
  Dificultad: Bajo  
  Descripción: versionado sin credenciales. `automations/n8n/WF-001-appointment-reminders.json` y `automations/n8n/SEGURIDAD_S5.md`.

## Criterios de aceptación

### CA-01 — Envío de recordatorio

**Dado** una cita `APPROVED` dentro de la ventana de recordatorio  
**Cuando** se ejecuta el workflow  
**Entonces** el USER recibe un correo con sede, profesional, especialidad y fecha/hora

### CA-02 — Exclusión

**Dado** citas en otros estados o fuera de la ventana  
**Cuando** se ejecuta el workflow  
**Entonces** no se envían recordatorios para ellas

### CA-03 — Versionado seguro

**Dado** el workflow exportado  
**Cuando** se revisa el JSON versionado  
**Entonces** no contiene credenciales ni secretos

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Ejecución exitosa registrada en n8n.
- [x] JSON exportado en `automations/n8n/`.
- [x] Riesgos residuales documentados.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | Ejecución manual n8n 378 (2026-10-04) | Cita sintética APPROVED <24 h: correo con sede, dirección, profesional, especialidad, fecha y hora; marcada con `alreadySent: false`. Resultado: 1 consultada, 1 enviada, 0 fallidas, 1 marcada |
| CA-02 | Cumple | Ejecuciones 378 y 379 | Citas REQUESTED, CANCELLED y APPROVED a 72 h no aparecen en la consulta; la segunda corrida devuelve `items: []` → "Sin recordatorios" (sin duplicados) |
| CA-03 | Cumple | `automations/n8n/WF-001-appointment-reminders.json` | JSON válido; sin IDs de credencial (solo nombres), URL del túnel (placeholder en `Config.apiBaseUrl`), emails, claves ni tokens |
| Fallo de API | Cumple | Ejecución 380 | Túnel cortado: 3 intentos, rama "API no disponible", ningún correo ni marcado |
| DoD | Cumple | `automations/n8n/SEGURIDAD_S5.md`, EP-008 | Riesgos residuales R-01 a R-08; demo de issue envenenado y prueba de dato envenenado contra el nodo "Preparar correo"; `mvnw clean test` 210/210 |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-10-04 (S5) — HU `Aprobada` explícitamente por el Product Owner. Decisiones D-040 a D-043 (ver `automations/n8n/PLAN_S5_S6.md`): ventana por defecto de 24 h, sin duplicados por cita y horario, acceso de n8n con `X-Api-Key` de rol `INTEGRATION`.
- 2026-10-04 (S5) — WF-001 creado por MCP y validado con ejecuciones manuales 378, 379 y 380 (envío, no duplicado, API caída). JSON exportado y revisado. Riesgos residuales y demo de contenido no confiable en `SEGURIDAD_S5.md`.
- 2026-10-04 (S5) — HU `Completada`: cierre confirmado explícitamente por el Product Owner, con la evidencia de cada CA y del DoD (commits `ecdc491` y `44d05d5`).

## Notas y decisiones

- Ventana de recordatorio: 24 h (D-040 a D-043), configurable en el nodo `Config` (1–72 h). Resuelve la incógnita abierta en S2.
