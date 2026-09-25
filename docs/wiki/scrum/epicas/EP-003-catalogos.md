---
id: EP-003
tipo: epica
titulo: "Catálogos"
estado: Borrador
historias:
  - "[[HU-005-consultar-catalogos-fijos]]"
  - "[[HU-006-gestionar-especialidades]]"
  - "[[HU-007-gestionar-eps-y-planes]]"
dependencias:
  - "[[EP-001-identidad-y-acceso]]"
---

# EP-003 — Catálogos

## Objetivo

Disponer de catálogos fijos precargados y de catálogos configurables por ADMIN (RF-05, RF-06, RF-09).

## Valor esperado

Datos de referencia consistentes y normalizados para profesionales, agenda, citas y afiliaciones.

## Actores

- ADMIN
- USER y PROFESSIONAL (lectura)

## Alcance

- Seed y consulta de roles, estados de cita, estados de reprogramación, regímenes y sedes.
- CRUD de especialidades (con duración 30/60) y de EPS/planes.

## Fuera de alcance

- Edición de catálogos fijos.

## Reglas de negocio

- Catálogos fijos de solo lectura.
- Sin borrado físico de catálogos referenciados; activación/desactivación.
- Duración de especialidad: 30 o 60 minutos.

## Dependencias

- [[EP-001-identidad-y-acceso]]

## Historias de usuario

- [[HU-005-consultar-catalogos-fijos]] — Sprint 2 — Aprobada
- [[HU-006-gestionar-especialidades]] — Sprint 2 — Aprobada
- [[HU-007-gestionar-eps-y-planes]] — Sprint 3 — Borrador

## Criterio de completitud de la épica

- [ ] Todas las HU obligatorias de esta épica están `Completada`.
- [ ] No quedan dependencias bloqueantes dentro del alcance de la épica.

## Riesgos e incógnitas

- Valores exactos de regímenes por confirmar.
