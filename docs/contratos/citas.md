# Contrato REST — Búsqueda, reserva y aprobación de citas (HU-012 a HU-015)

- **Base URL (local):** `http://localhost:8081` (`API_PORT`, D-010)
- **Versionado:** todos los endpoints bajo `/api/v1/` (D-018).
- **Autorización:** `/api/v1/availability` y `/api/v1/appointments/**` exigen rol `USER`. Sin token → `401`; con otro rol (ADMIN, PROFESSIONAL) → `403`. `/api/v1/admin/appointments/**` exige rol `ADMIN`.
- **Paciente:** sale **siempre del access token**, nunca del cuerpo.
- **Fechas y horas:** fecha `YYYY-MM-DD` y hora `HH:mm`, en hora local de `America/Bogota` (D-007).
- **Implementación:** `infrastructure/adapters/in/web/booking/` y `infrastructure/adapters/in/web/admin/AdminAppointmentController.java`.

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/v1/availability?date=&siteCode=&type=&specialtyId=&professionalId=` | Horarios reservables de un día |
| POST | `/api/v1/appointments` | Reserva una cita (`201`). Medicina General → `APPROVED`; otra especialidad → `REQUESTED` |
| GET | `/api/v1/admin/appointments/requests` | ADMIN: citas `REQUESTED` pendientes de decisión |
| POST | `/api/v1/admin/appointments/{id}/approve` | ADMIN: aprueba → `APPROVED` |
| POST | `/api/v1/admin/appointments/{id}/reject` | ADMIN: rechaza con motivo → `REJECTED` y libera los slots |

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

Un solo endpoint para los dos flujos: **la especialidad decide el estado inicial**. El cliente envía los mismos datos que le devolvió la búsqueda.

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
| 404 | `NOT_FOUND` | Aprobar o rechazar una cita inexistente |

## Decisión del ADMIN (HU-015)

### Listado de solicitudes

```http
GET /api/v1/admin/appointments/requests
```

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
