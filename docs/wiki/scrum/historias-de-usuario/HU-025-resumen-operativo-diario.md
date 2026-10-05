---
id: HU-025
tipo: historia-de-usuario
titulo: "Resumen operativo diario"
estado: Aprobada
epica: "[[EP-008-automatizaciones-n8n]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 5"
dependencias:
  - "[[HU-022-bandeja-administrativa]]"
relacionadas:
  - "[[HU-023-recordatorios-de-citas]]"
---

# HU-025 — Resumen operativo diario

## Historia de usuario

**COMO** ADMIN  
**QUIERO** recibir un resumen diario de citas agrupado por sede y estado  
**PARA** conocer la operación del día sin consultar la aplicación

> Como ADMIN, quiero recibir un resumen diario de citas agrupado por sede y estado para conocer la operación del día sin consultar la aplicación.

## Contexto y descripción

PRD §10, automatización 3. Opcional/bonus en S6. Flujo: Schedule → API resumen del día → agrupación → Gmail.

## Alcance

- Consulta agregada del día por sede y estado.
- Workflow n8n con correo al ADMIN.
- JSON exportado en `automations/n8n/WF-003-daily-operational-summary.json`.

## Fuera de alcance

- Dashboards analíticos históricos.

## Reglas de negocio

- El resumen contiene conteos agregados, sin datos personales de usuarios.
- Acceso de n8n con privilegio mínimo.

## Dependencias y relaciones

- Épica: [[EP-008-automatizaciones-n8n]]
- Dependencias: [[HU-022-bandeja-administrativa]]
- Relacionadas: [[HU-023-recordatorios-de-citas]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** consulta agregada nueva e integración programada.

## Tareas de desarrollo

- [x] **T-01 — Consulta agregada del día**  
  Dificultad: Medio  
  Descripción: conteos por sede y estado. `GET /api/v1/integrations/daily-summary`; `IntegrationApiIntegrationTest.hu025_*`.
- [x] **T-02 — Workflow n8n y exportación**  
  Dificultad: Medio  
  Descripción: trigger programado, formato y envío. Creado por MCP: `WF-003 Resumen operativo diario — JuanCarlos Muñoz` (9 nodos, diario 06:30 America/Bogota); `automations/n8n/WF-003-daily-operational-summary.json`.

## Criterios de aceptación

### CA-01 — Resumen correcto

**Dado** citas del día en varias sedes y estados  
**Cuando** se ejecuta el workflow  
**Entonces** el ADMIN recibe conteos por sede y estado coincidentes con la base de datos

### CA-02 — Sin datos personales

**Dado** el correo de resumen  
**Cuando** se revisa su contenido  
**Entonces** no incluye datos personales de usuarios

## Definition of Done

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] JSON exportado en `automations/n8n/`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | Ejecución manual n8n 386 (fecha fijada 2026-10-05); `hu025_ca01_ca02_elResumenCuentaPorSedeEstadoYEspecialidadSinDatosPersonales` | Correo enviado al ADMIN con total 5; HIC: REQUESTED 1, APPROVED 2, CANCELLED 1, REJECTED 1; ICV 0; Smoke Cardio 3, Medicina General 2; pendientes 1/0. Coincide con las citas sintéticas del día (ids 2, 3, 4, 6, 7) |
| CA-02 | Cumple | Ejecución 386; contrato `integraciones.md` § Resumen diario | El correo solo lleva conteos y nombres de sede/especialidad; el endpoint no expone datos de pacientes y el nodo `Construir resumen` fuerza los conteos a número y escapa los textos |
| Fallo de API | Cumple | Ejecución 387 | Túnel cortado (530): rama "Avisar no disponible", correo "Resumen no disponible — citas" enviado y registrado; no se envía resumen |
| DoD | Cumple | `WF-003-daily-operational-summary.json` | JSON sin IDs de credencial, `webhookId`, URL del túnel ni correo del ADMIN (marcadores en `Config`); `date` vacío para que la ejecución programada resuma el día actual |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-10-04 (S5) — HU `Aprobada` explícitamente por el Product Owner como bonus de S6.
- 2026-10-04 (S6) — WF-003 creado por MCP y validado con las ejecuciones manuales 386 (resumen) y 387 (API caída). JSON exportado y revisado. El ADMIN de prueba recibe el correo en el buzón del estudiante.

## Notas y decisiones

- HU opcional (bonus) según la guía S6.
