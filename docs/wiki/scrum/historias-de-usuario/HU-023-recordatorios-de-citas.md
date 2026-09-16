---
id: HU-023
tipo: historia-de-usuario
titulo: "Recordatorios de citas próximas"
estado: Borrador
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

- [ ] **T-01 — Consulta de citas próximas para automatización**  
  Dificultad: Medio  
  Descripción: endpoint protegido con credencial de servicio.
- [ ] **T-02 — Workflow n8n**  
  Dificultad: Medio  
  Descripción: trigger programado, consulta, envío Gmail y registro.
- [ ] **T-03 — Exportar JSON y documentar riesgos residuales**  
  Dificultad: Bajo  
  Descripción: versionado sin credenciales.

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

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Ejecución exitosa registrada en n8n.
- [ ] JSON exportado en `automations/n8n/`.
- [ ] Riesgos residuales documentados.
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

- Incógnita: ventana de recordatorio (p. ej. 24 h antes) a definir en S5.
