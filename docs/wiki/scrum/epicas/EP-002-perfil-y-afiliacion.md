---
id: EP-002
tipo: epica
titulo: "Perfil y afiliación"
estado: Borrador
historias:
  - "[[HU-003-consultar-y-actualizar-perfil]]"
  - "[[HU-004-registrar-afiliacion]]"
dependencias:
  - "[[EP-001-identidad-y-acceso]]"
  - "[[EP-003-catalogos]]"
---

# EP-002 — Perfil y afiliación

## Objetivo

Permitir al USER mantener su perfil y registrar su afiliación EPS/plan/régimen (RF-04).

## Valor esperado

Datos de contacto y afiliación actualizados y normalizados.

## Actores

- USER

## Alcance

- Consulta/actualización de datos permitidos del perfil.
- Afiliación referenciando catálogos sin duplicar nombres.
- Home/dashboard USER.

## Fuera de alcance

- Validación con sistemas reales de EPS.

## Reglas de negocio

- Ownership: cada USER gestiona solo su perfil.
- Sin duplicar EPS, régimen y plan dentro del usuario (3FN).

## Dependencias

- [[EP-001-identidad-y-acceso]]
- [[EP-003-catalogos]]

## Historias de usuario

- [[HU-003-consultar-y-actualizar-perfil]] — Sprint 3 — Borrador
- [[HU-004-registrar-afiliacion]] — Sprint 3 — Completada

## Criterio de completitud de la épica

- [ ] Todas las HU obligatorias de esta épica están `Completada`.
- [ ] No quedan dependencias bloqueantes dentro del alcance de la épica.

## Riesgos e incógnitas

- Cardinalidad de afiliaciones por usuario (una vigente o varias).
