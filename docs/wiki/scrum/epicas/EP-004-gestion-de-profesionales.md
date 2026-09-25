---
id: EP-004
tipo: epica
titulo: "Gestión de profesionales"
estado: Borrador
historias:
  - "[[HU-008-crear-profesional]]"
  - "[[HU-009-activar-desactivar-profesional]]"
dependencias:
  - "[[EP-003-catalogos]]"
---

# EP-004 — Gestión de profesionales

## Objetivo

Permitir a ADMIN crear y administrar profesionales con sus especialidades y sedes (RF-07).

## Valor esperado

Profesionales habilitados para publicar agenda y recibir citas.

## Actores

- ADMIN
- PROFESSIONAL (resultado)

## Alcance

- Creación de usuario PROFESSIONAL con código y matrícula ficticia.
- Asignación N:M de especialidades (una primaria) y sedes.
- Activación/desactivación.

## Fuera de alcance

- Autoregistro de profesionales.

## Reglas de negocio

- Datos sintéticos.
- Al menos una especialidad (una primaria) y una sede.

## Dependencias

- [[EP-003-catalogos]]
- [[EP-001-identidad-y-acceso]]

## Historias de usuario

- [[HU-008-crear-profesional]] — Sprint 2 — Aprobada
- [[HU-009-activar-desactivar-profesional]] — Sprint 2 — Aprobada

## Criterio de completitud de la épica

- [ ] Todas las HU obligatorias de esta épica están `Completada`.
- [ ] No quedan dependencias bloqueantes dentro del alcance de la épica.

## Riesgos e incógnitas

- Entrega de contraseña inicial al profesional.
