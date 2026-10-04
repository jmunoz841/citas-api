# Contrato REST — Búsqueda, reserva, aprobación, ciclo de vida y reprogramación de citas (HU-012 a HU-022)

- **Base URL (local):** `http://localhost:8081` (`API_PORT`, D-010)
- **Versionado:** todos los endpoints bajo `/api/v1/` (D-018).
- **Autorización:** `/api/v1/availability` y `/api/v1/appointments/**` exigen rol `USER`. Sin token → `401`; con otro rol (ADMIN, PROFESSIONAL) → `403`. `/api/v1/admin/appointments/**` exige rol `ADMIN`.
- **Paciente:** sale **siempre del access token**, nunca del cuerpo.
- **Fechas y horas:** fecha `YYYY-MM-DD` y hora `HH:mm`, en hora local de `America/Bogota` (D-007).
- **Implementación:** `infrastructure/adapters/in/web/booking/` y `infrastructure/adapters/in/web/admin/` (`AdminAppointmentController`, `AdminRescheduleController`).

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/v1/availability?date=&siteCode=&type=&specialtyId=&professionalId=` | Horarios reservables de un día |
| POST | `/api/v1/appointments` | Reserva una cita (`201`). Medicina General → `APPROVED`; otra especialidad → `REQUESTED` |
| GET | `/api/v1/admin/appointments/requests?siteCode=&professionalId=&specialtyId=&from=&to=` | ADMIN: citas `REQUESTED` pendientes de decisión, filtrables (HU-015, HU-022) |
| POST | `/api/v1/admin/appointments/{id}/approve` | ADMIN: aprueba → `APPROVED` |
| POST | `/api/v1/admin/appointments/{id}/reject` | ADMIN: rechaza con motivo → `REJECTED` y libera los slots |
| GET | `/api/v1/appointments?status=&from=&to=` | USER: lista sus propias citas (HU-016) |
| GET | `/api/v1/appointments/{id}` | USER: consulta el detalle de una cita propia (HU-016) |
| POST | `/api/v1/appointments/{id}/cancel` | USER: cancela una cita propia futura (HU-017) |
| POST | `/api/v1/appointments/{id}/reschedule-requests` | USER: solicita reprogramar una cita propia `APPROVED` futura (`201`, HU-018) |
| GET | `/api/v1/admin/reschedule-requests?siteCode=&professionalId=&specialtyId=&from=&to=` | ADMIN: reprogramaciones `PENDING`, filtrables (HU-019, HU-022) |
| POST | `/api/v1/admin/reschedule-requests/{id}/approve` | ADMIN: aprueba → la cita pasa a la nueva franja (HU-019) |
| POST | `/api/v1/admin/reschedule-requests/{id}/reject` | ADMIN: rechaza con motivo → la cita conserva su franja (HU-019) |

## Mis citas (HU-016)

```http
GET /api/v1/appointments?status=APPROVED&from=2026-10-01&to=2026-10-31
GET /api/v1/appointments/31
```

Solo un `USER` autenticado puede usar estos endpoints. La identidad del paciente viene del
access token; no se acepta ni se expone un identificador de paciente. Todos los filtros son opcionales:

| Parametro | Formato | Regla |
|---|---|---|
| `status` | `REQUESTED`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED` o `NO_SHOW` | filtra por estado |
| `from` | `YYYY-MM-DD` | fecha inicial inclusiva |
| `to` | `YYYY-MM-DD` | fecha final inclusiva; no puede ser anterior a `from` |

El listado devuelve `200` y `items`, ordenados por inicio ascendente. El detalle devuelve el
mismo objeto. Ambos incluyen sede, profesional, especialidad, fecha/hora, duracion y estado:

```json
{
  "id": 31, "status": "REJECTED", "professionalName": "Laura Gomez",
  "specialtyName": "Cardiologia", "siteCode": "HIC",
  "siteName": "Hospital Internacional de Colombia", "date": "2026-10-01",
  "startTime": "09:00", "endTime": "10:00", "durationMinutes": 60,
  "rejectionReason": "El especialista no atiende esta patologia",
  "professionalId": 12, "specialtyId": 4,
  "reschedule": null
}
```

`professionalId` y `specialtyId` permiten buscar disponibilidad del mismo profesional y especialidad
para reprogramar. `reschedule` es la última solicitud de reprogramación de la cita, o `null` si nunca
se pidió (ver [Reprogramación](#reprogramación-hu-018-hu-019)).

`rejectionReason` es `null` en citas no rechazadas. Una cita inexistente o de otro USER
responde `404 NOT_FOUND`, sin revelar su existencia. Filtros invalidos responden
`400 VALIDATION_ERROR`.

## Cancelar cita (HU-017)

```http
POST /api/v1/appointments/31/cancel
```

La cancelacion solo esta disponible para el `USER` propietario de una cita futura en estado
`REQUESTED` o `APPROVED`. Devuelve `200` con la respuesta de cita y estado `CANCELLED`.
La operacion libera todas las filas de `slot_reservations` de la cita y agrega un registro de
historial con `source: USER` y el paciente como actor, en la misma transaccion.

Una cita ajena o inexistente devuelve `404 NOT_FOUND` sin revelar su existencia. Una cita pasada
o en estado distinto de `REQUESTED` y `APPROVED` devuelve `400 VALIDATION_ERROR` o
`409 INVALID_STATUS_TRANSITION`, respectivamente. Una cita cancelada no se reactiva.

## Historial de estados (HU-021)

`GET /api/v1/appointments/{id}/history` devuelve al USER dueño los cambios inmutables, ordenados
por fecha, con estado, fuente, actor, fecha/hora y motivo. ADMIN usa
`/api/v1/admin/appointments/{id}/history` y el PROFESSIONAL asignado
`/api/v1/professional/appointments/{id}/history`. Una cita ajena responde `404`; no existen
endpoints de edición ni borrado del historial.

```json
{
  "items": [
    { "status": "APPROVED", "source": "USER", "actorUserId": 24,
      "changedAt": "2026-10-01T09:00:00", "reason": null }
  ]
}
```

## Buscar disponibilidad (HU-012)

| Parámetro | Obligatorio | Valores |
|---|---|---|
| `date` | Sí | `YYYY-MM-DD` |
| `siteCode` | No | `HIC`, `ICV` |
| `type` | No | `GENERAL` (Medicina General) o `SPECIALIZED` (cualquier otra) |
| `specialtyId` | No | id de especialidad |
| `professionalId` | No | id del profesional |

Cada filtro presente restringe el resultado (CA-03).

```http
GET /api/v1/availability?date=2026-09-26&specialtyId=4&siteCode=HIC
```

Respuesta `200`, ordenada por hora de inicio:

```json
{
  "items": [
    {
      "professionalId": 12, "professionalName": "Laura Gómez",
      "specialtyId": 4, "specialtyName": "Cardiología", "type": "SPECIALIZED",
      "durationMinutes": 60, "siteCode": "HIC",
      "date": "2026-09-26", "startTime": "09:00", "endTime": "10:00"
    }
  ]
}
```

Cada ítem es un **inicio reservable**, no un slot suelto:

| Regla | Detalle |
|---|---|
| Duración | 30 min ocupan 1 slot; 60 min, **2 slots consecutivos del mismo bloque**. Dos bloques contiguos no se combinan (RN-05) |
| Ocupación | Se excluyen slots reservados (`APPROVED`) o retenidos (`REQUESTED`) (RN-01) |
| Pasado | Solo inicios posteriores al momento de la consulta (RN-06) |
| Oferta activa | Solo profesionales activos, especialidades activas y asociación profesional–especialidad activa (RN-08, HU-009 CA-02) |

Ejemplo (CA-02): bloque 08:00–10:00 con 08:30 reservado → para 60 min solo se ofrece `09:00`.

## Reservar una cita (HU-013, HU-014)

```json
POST /api/v1/appointments
{ "professionalId": 12, "specialtyId": 4, "siteCode": "HIC", "date": "2026-09-26", "startTime": "09:00" }
```

Respuesta `201`:

```json
{
  "id": 31, "status": "REQUESTED", "professionalId": 12, "specialtyId": 4, "siteCode": "HIC",
  "date": "2026-09-26", "startTime": "09:00", "endTime": "10:00", "durationMinutes": 60
}
```

Un solo endpoint para los dos flujos (D-027): **la especialidad decide el estado inicial**. El cliente envía los mismos datos que le devolvió la búsqueda.

| Especialidad | Estado inicial | Slots |
|---|---|---|
| Medicina General (`type: GENERAL`) | `APPROVED` (RN-02) | Reservados |
| Cualquier otra (`type: SPECIALIZED`) | `REQUESTED` (RN-03); espera la decisión del ADMIN (HU-015) | Retenidos |

En la misma transacción se crean la cita, la ocupación de sus slots y el primer registro del historial (`source: USER`, actor = paciente).

### Doble reserva

La aplicación **no** comprueba antes si el horario está libre. Inserta una fila por slot en `slot_reservations`, cuya clave primaria es `slot_id`: si otra cita ya ocupa el slot, MySQL rechaza la inserción (error 1062), la transacción entera se deshace y la API responde `409 SLOT_UNAVAILABLE`. Con dos reservas simultáneas sobre el mismo slot, exactamente una recibe `201` y la otra `409` (HU-013 CA-03).

## Reglas

| Regla | Respuesta si se incumple | Dónde se garantiza |
|---|---|---|
| El horario no está ocupado | `409 SLOT_UNAVAILABLE` | Base: `pk_slot_reservations` |
| El horario existe completo en un bloque | `409 SLOT_UNAVAILABLE` | Dominio (`SlotPlanner`) |
| No reservar en el pasado | `400`, `field: startTime` | Aplicación (MySQL no admite `NOW()` en un `CHECK`) |
| Especialidad existente y activa | `400`, `field: specialtyId` | Aplicación |
| Profesional existente y activo | `400`, `field: professionalId` | Aplicación |
| El profesional atiende la especialidad | `400`, `field: specialtyId` | Aplicación + FK `fk_appt_professional_specialty` |
| El profesional atiende en la sede | `400`, `field: siteCode` | Aplicación + FK `fk_appt_professional_site` |
| El slot es del mismo profesional | — | Base: FK compuesta `fk_sr_slot` |

## Errores

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Cuerpo incompleto, `date` ausente o con formato inválido, `type` desconocido, o cualquier regla `400` de la tabla anterior |
| 401 | `UNAUTHORIZED` | Sin access token válido |
| 403 | `FORBIDDEN` | Autenticado sin el rol del endpoint (`USER` para buscar y reservar, `ADMIN` para decidir) |
| 409 | `SLOT_UNAVAILABLE` | El horario ya lo ocupa otra cita o dejó de existir |
| 409 | `INVALID_STATUS_TRANSITION` | Aprobar o rechazar una cita que no está `REQUESTED` (ya resuelta, o general) |
| 409 | `APPOINTMENT_NOT_RESCHEDULABLE` | Reprogramar una cita que no está `APPROVED` (HU-018 CA-02) |
| 409 | `RESCHEDULE_ALREADY_PENDING` | La cita ya tiene una reprogramación `PENDING` (HU-018 CA-04) |
| 409 | `RESCHEDULE_NOT_PENDING` | Aprobar o rechazar una reprogramación ya resuelta (HU-019 CA-04) |
| 409 | `RESCHEDULE_EXPIRED` | Aprobar una reprogramación cuya cita original o franja pedida ya empezó (D-033) |
| 409 | `CONCURRENT_UPDATE` | Dos operaciones simultáneas se bloquearon en la base (p. ej. aprobar y cancelar la misma cita); una se deshizo. Recargar y reintentar |
| 404 | `NOT_FOUND` | Aprobar o rechazar una cita o reprogramación inexistente; reprogramar una cita ajena |

## Decisión del ADMIN (HU-015)

### Listado de solicitudes

```http
GET /api/v1/admin/appointments/requests?siteCode=HIC&professionalId=12&specialtyId=4&from=2026-10-01&to=2026-10-31
```

Todos los filtros son opcionales y combinables (HU-022); ver [Bandeja administrativa](#bandeja-administrativa-hu-022).
Respuesta `200`, ordenada por fecha de la cita:

```json
{
  "items": [
    {
      "id": 31, "status": "REQUESTED", "patientName": "Ana Pérez", "professionalName": "Laura Gómez",
      "specialtyName": "Cardiología", "siteCode": "HIC",
      "date": "2026-09-26", "startTime": "09:00", "endTime": "10:00", "durationMinutes": 60
    }
  ]
}
```

### Aprobar y rechazar

```http
POST /api/v1/admin/appointments/31/approve
POST /api/v1/admin/appointments/31/reject
{ "reason": "El especialista no atiende esta patología" }
```

Respuesta `200`: `{ "id": 31, "status": "APPROVED" }` o `{ "id": 31, "status": "REJECTED" }`.

| Regla | Respuesta si se incumple | Dónde se garantiza |
|---|---|---|
| Solo ADMIN | `403` (`401` sin token) | `SecurityConfig`: `/api/v1/admin/**` |
| Solo una cita `REQUESTED` se resuelve (RN-11) | `409 INVALID_STATUS_TRANSITION` | Dominio + `UPDATE ... WHERE status_code = 'REQUESTED'` |
| El rechazo exige motivo, máx. 500 caracteres (RN-04) | `400`, `field: reason` | DTO + dominio + `CHECK chk_ash_rejection_reason` |
| Rechazar libera los slots (RN-09) | — | `DELETE` de sus filas en `slot_reservations`, en la misma transacción |
| Cada decisión deja historial `source: ADMIN` con el ADMIN como actor | — | Aplicación + `CHECK chk_ash_actor_required` |

**Dos decisiones a la vez.** El cambio de estado es un `UPDATE` condicionado a `status_code = 'REQUESTED'`. Si dos ADMIN aprueban y rechazan la misma cita a la vez, InnoDB serializa las dos actualizaciones: la segunda ya no encuentra la cita pendiente, actualiza 0 filas y responde `409`. Solo queda un registro de decisión en el historial.

## Reprogramación (HU-018, HU-019)

Migración `V9__reprogramacion_hu018_hu019.sql`: tabla `reschedule_requests` y titular exclusivo en
`slot_reservations` (la cita **o** la solicitud `PENDING`, nunca ambas: `CHECK chk_sr_single_holder`).

### Solicitar (USER, HU-018)

```http
POST /api/v1/appointments/31/reschedule-requests
{ "siteCode": "ICV", "date": "2026-10-08", "startTime": "14:00" }
```

Profesional y especialidad son los de la cita; no se envían. El cliente obtiene las franjas libres con
`GET /api/v1/availability?date=&professionalId=&specialtyId=` usando los `professionalId` y `specialtyId`
de la cita. La sede puede cambiar (supuesto S-20 del diseño 3FN).

Respuesta `201`:

```json
{
  "id": 7, "appointmentId": 31, "status": "PENDING",
  "originalDate": "2026-10-06", "originalStartTime": "08:00", "originalSiteCode": "HIC",
  "requestedDate": "2026-10-08", "requestedStartTime": "14:00", "requestedSiteCode": "ICV"
}
```

Mientras está `PENDING`, la cita sigue `APPROVED` en su franja original y la nueva franja queda
retenida: deja de aparecer en la búsqueda y nadie más la puede reservar. El paciente ve el estado en
el campo `reschedule` de su cita:

```json
"reschedule": {
  "id": 7, "status": "PENDING", "requestedDate": "2026-10-08", "requestedStartTime": "14:00",
  "requestedSiteCode": "ICV", "decisionReason": null
}
```

| Regla | Respuesta si se incumple | Dónde se garantiza |
|---|---|---|
| Cita propia | `404 NOT_FOUND` (no se revela que existe) | Aplicación |
| Solo citas `APPROVED` (RF-15) | `409 APPOINTMENT_NOT_RESCHEDULABLE` | Dominio |
| Solo citas futuras | `400`, `field: appointmentId` | Dominio |
| Nueva franja futura y distinta de la actual | `400`, `field: startTime` | Dominio + `CHECK chk_rr_changes_time` |
| Profesional, especialidad y asociación siguen activos | `400`, `field: appointmentId` | Aplicación |
| La nueva franja existe completa en un bloque y está libre | `409 SLOT_UNAVAILABLE` | `SlotPlanner` + `pk_slot_reservations` |
| Máximo una reprogramación `PENDING` por cita (D-033) | `409 RESCHEDULE_ALREADY_PENDING` | Aplicación + índice funcional `uk_rr_one_pending_per_appointment` |
| La nueva franja no puede solaparse con la actual (p. ej. una cita de 08:00–09:00 no se mueve a las 08:30): esos slots ya son de la cita y cada slot tiene un único titular | `409 SLOT_UNAVAILABLE` | `pk_slot_reservations` + `chk_sr_single_holder`; la búsqueda ya no los ofrece |

### Bandeja y decisión (ADMIN, HU-019)

```http
GET /api/v1/admin/reschedule-requests?siteCode=ICV&from=2026-10-01
```

Respuesta `200`, ordenada por la franja solicitada:

```json
{
  "items": [
    {
      "id": 7, "appointmentId": 31, "status": "PENDING", "patientName": "Ana Pérez",
      "professionalName": "Laura Gómez", "specialtyName": "Cardiología", "durationMinutes": 60,
      "originalDate": "2026-10-06", "originalStartTime": "08:00", "originalSiteCode": "HIC",
      "requestedDate": "2026-10-08", "requestedStartTime": "14:00", "requestedSiteCode": "ICV",
      "requestedAt": "2026-10-04T16:20:11.123"
    }
  ]
}
```

```http
POST /api/v1/admin/reschedule-requests/7/approve
POST /api/v1/admin/reschedule-requests/7/reject
{ "reason": "El profesional no puede ese día" }
```

Respuesta `200`: `{ "id": 7, "appointmentId": 31, "status": "APPROVED", "decisionReason": null }` o
`{ ..., "status": "REJECTED", "decisionReason": "El profesional no puede ese día" }`.

| Regla | Respuesta si se incumple | Dónde se garantiza |
|---|---|---|
| Solo ADMIN | `403` (`401` sin token) | `SecurityConfig`: `/api/v1/admin/**` |
| Solo una reprogramación `PENDING` se resuelve | `409 RESCHEDULE_NOT_PENDING` | Dominio + `UPDATE ... WHERE status_code = 'PENDING'` |
| El rechazo exige motivo, máx. 500 caracteres (RN-04) | `400`, `field: reason` | DTO + dominio + `CHECK chk_rr_rejection_reason` |
| Aprobar es atómico: libera los slots antiguos, pasa a la cita los retenidos y mueve su horario | — | Una transacción; `CHECK chk_sr_single_holder` |
| Rechazar libera la franja retenida y la cita conserva la suya | — | `DELETE` de las filas de la solicitud |
| Aprobar deja historial `APPROVED`, `source: ADMIN`, con motivo "Reprogramada del … al …" enlazado a la solicitud | — | FK `fk_ash_reschedule_request` |
| No se aprueba si ya empezó la cita original o la franja pedida | `409 RESCHEDULE_EXPIRED` | Dominio |

### Cierre automático (D-033)

- **Cancelar la cita** (HU-017) con una reprogramación `PENDING` la deja `CANCELLED` y libera ambas franjas.
- **Cerrar la cita** como `COMPLETED`/`NO_SHOW` (HU-020) también cancela la solicitud pendiente.
- **Al llegar la hora** de la cita original (o de la franja pedida) con la solicitud `PENDING`, pasa a
  `CANCELLED` y libera su retención. Lo hace la bandeja antes de listar y un job cada 5 minutos
  (`citas.reschedule.expiration-delay`, por defecto `PT5M`).

## Bandeja administrativa (HU-022)

Las dos listas del ADMIN aceptan los mismos filtros, opcionales y combinables (todos se cumplen a la vez):

| Parámetro | Formato | En solicitudes especializadas | En reprogramaciones |
|---|---|---|---|
| `siteCode` | `HIC` o `ICV` (no distingue mayúsculas) | Sede de la cita | Sede **solicitada** |
| `professionalId` | número | Profesional de la cita | Profesional de la cita |
| `specialtyId` | número | Especialidad de la cita | Especialidad de la cita |
| `from` | `YYYY-MM-DD` | Inicio de la cita ≥ fecha | Inicio **solicitado** ≥ fecha |
| `to` | `YYYY-MM-DD`, no anterior a `from` | Inicio de la cita ≤ fecha (inclusive) | Inicio **solicitado** ≤ fecha (inclusive) |

Solo aparecen las pendientes (`REQUESTED` y `PENDING`); las resueltas no. `to` anterior a `from` →
`400`, `field: to`.
