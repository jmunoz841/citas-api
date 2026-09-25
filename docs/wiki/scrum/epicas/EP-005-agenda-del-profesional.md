---
id: EP-005
tipo: epica
titulo: "Agenda del profesional"
estado: Borrador
historias:
  - "[[HU-010-gestionar-bloques-de-disponibilidad]]"
  - "[[HU-011-consultar-agenda-del-profesional]]"
dependencias:
  - "[[EP-004-gestion-de-profesionales]]"
---

# EP-005 — Agenda del profesional

## Objetivo

Permitir al PROFESSIONAL publicar disponibilidad por sede y consultar su agenda de citas (RF-08, RF-16).

## Valor esperado

Oferta de horarios reservables y visibilidad de la atención programada.

## Actores

- PROFESSIONAL

## Alcance

- Bloques por día/sede discretizados en slots de 30 minutos.
- Consulta de citas `APPROVED` por día/semana y sede.

## Fuera de alcance

- Bloques recurrentes.

## Reglas de negocio

- Sin bloques en el pasado ni solapados (RN-06).
- Solo en sedes asignadas (RN-07).
- Sin edición/borrado de bloques con citas comprometidas.
- Visibilidad limitada a citas propias.

## Dependencias

- [[EP-004-gestion-de-profesionales]]

## Historias de usuario

- [[HU-010-gestionar-bloques-de-disponibilidad]] — Sprint 2 — Completada
- [[HU-011-consultar-agenda-del-profesional]] — Sprint 3 — Borrador

## Criterio de completitud de la épica

- [ ] Todas las HU obligatorias de esta épica están `Completada`.
- [ ] No quedan dependencias bloqueantes dentro del alcance de la épica.

## Riesgos e incógnitas

- ~~Representación de slots (materializados vs calculados)~~ → resuelta por D-013: slots materializados de 30 minutos.
