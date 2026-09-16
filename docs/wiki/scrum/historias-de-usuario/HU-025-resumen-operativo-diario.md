---
id: HU-025
tipo: historia-de-usuario
titulo: "Resumen operativo diario"
estado: Borrador
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

- [ ] **T-01 — Consulta agregada del día**  
  Dificultad: Medio  
  Descripción: conteos por sede y estado.
- [ ] **T-02 — Workflow n8n y exportación**  
  Dificultad: Medio  
  Descripción: trigger programado, formato y envío.

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

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] JSON exportado en `automations/n8n/`.
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

- HU opcional (bonus) según la guía S6.
