---
id: EP-007
tipo: epica
titulo: "Ciclo de vida de citas"
estado: Borrador
historias:
  - "[[HU-016-consultar-mis-citas]]"
  - "[[HU-017-cancelar-cita]]"
  - "[[HU-018-solicitar-reprogramacion]]"
  - "[[HU-019-resolver-reprogramacion]]"
  - "[[HU-020-cerrar-atencion]]"
  - "[[HU-021-historial-de-estados]]"
dependencias:
  - "[[EP-006-reserva-de-citas]]"
---

# EP-007 — Ciclo de vida de citas

## Objetivo

Gestionar la vida de una cita tras su creación: consulta, cancelación, reprogramación, cierre de atención y auditoría de estados (RF-13, RF-14, RF-15, RF-17, RF-19).

## Valor esperado

MVP completo con transiciones explícitas, verificables y auditadas.

## Actores

- USER
- PROFESSIONAL
- ADMIN

## Alcance

- Mis citas con filtros y detalle.
- Cancelación con liberación de slots.
- Reprogramación con retención provisional y resolución ADMIN.
- Cierre `COMPLETED`/`NO_SHOW`.
- Historial de estados inmutable.

## Fuera de alcance

- Reactivación de citas canceladas.
- Cambio de profesional en reprogramación.

## Reglas de negocio

- RN-09, RN-10, RN-11, RN-12.

## Dependencias

- [[EP-006-reserva-de-citas]]

## Historias de usuario

- [[HU-016-consultar-mis-citas]] — Sprint 3 — Aprobada
- [[HU-017-cancelar-cita]] — Sprint 3 — Aprobada
- [[HU-018-solicitar-reprogramacion]] — Sprint 3 — Borrador
- [[HU-019-resolver-reprogramacion]] — Sprint 3 — Borrador
- [[HU-020-cerrar-atencion]] — Sprint 3 — Aprobada
- [[HU-021-historial-de-estados]] — Sprint 3 — Aprobada

## Criterio de completitud de la épica

- [ ] Todas las HU obligatorias de esta épica están `Completada`.
- [ ] No quedan dependencias bloqueantes dentro del alcance de la épica.

## Riesgos e incógnitas

- Comportamiento de reprogramaciones `PENDING` al cancelar o al llegar la fecha original.
