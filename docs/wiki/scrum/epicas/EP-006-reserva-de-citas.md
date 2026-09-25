---
id: EP-006
tipo: epica
titulo: "Reserva de citas"
estado: Borrador
historias:
  - "[[HU-012-consultar-disponibilidad]]"
  - "[[HU-013-reservar-cita-general]]"
  - "[[HU-014-solicitar-cita-especializada]]"
  - "[[HU-015-resolver-cita-especializada]]"
  - "[[HU-022-bandeja-administrativa]]"
dependencias:
  - "[[EP-005-agenda-del-profesional]]"
---

# EP-006 — Reserva de citas

## Objetivo

Permitir al USER encontrar horarios y reservar citas generales (auto-aprobadas) o especializadas (con aprobación ADMIN) sin doble reserva (RF-10, RF-11, RF-12, RF-18).

## Valor esperado

Flujo central del producto: de la disponibilidad a una cita confirmada o solicitada.

## Actores

- USER
- ADMIN

## Alcance

- Búsqueda de disponibilidad con filtros y duración 30/60.
- Cita general `APPROVED` automática.
- Cita especializada `REQUESTED` con retención de slots.
- Aprobación/rechazo con motivo y bandeja administrativa.

## Fuera de alcance

- Pagos, facturación, datos clínicos.

## Reglas de negocio

- RN-01, RN-02, RN-03, RN-04, RN-05, RN-06, RN-08, RN-09, RN-11.

## Dependencias

- [[EP-005-agenda-del-profesional]]
- [[EP-003-catalogos]]

## Historias de usuario

- [[HU-012-consultar-disponibilidad]] — Sprint 2 — Aprobada
- [[HU-013-reservar-cita-general]] — Sprint 2 — Aprobada
- [[HU-014-solicitar-cita-especializada]] — Sprint 2 — Aprobada
- [[HU-015-resolver-cita-especializada]] — Sprint 2 — Aprobada
- [[HU-022-bandeja-administrativa]] — Sprint 3 — Borrador

## Criterio de completitud de la épica

- [ ] Todas las HU obligatorias de esta épica están `Completada`.
- [ ] No quedan dependencias bloqueantes dentro del alcance de la épica.
- [x] Existe prueba automatizada de doble reserva (`BookingApiIntegrationTest.hu013_ca03_dobleReservaConcurrenteSoloUnaCreaLaCita`, dos peticiones HTTP simultáneas → 201 y 409).

## Riesgos e incógnitas

- ~~Estrategia de concurrencia para evitar doble reserva~~ → resuelta por D-013: PK `slot_id` de `slot_reservations`.
- ~~Expiración de solicitudes `REQUESTED` no definida en el PRD~~ → D-030: sin expiración en S3.
