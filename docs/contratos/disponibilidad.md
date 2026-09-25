# Contrato REST — Disponibilidad del profesional (HU-010)

- **Base URL (local):** `http://localhost:8081` (`API_PORT`, D-010)
- **Versionado:** todos los endpoints bajo `/api/v1/` (D-018).
- **Autorización:** `/api/v1/professional/**` exige rol `PROFESSIONAL`. Sin token → `401`; con otro rol → `403`.
- **Pertenencia:** el profesional sale **siempre del access token**, nunca del cuerpo ni de la ruta. Un bloque ajeno responde `404`, no `403`: no se revela que existe (CA-06).
- **Fechas y horas:** fecha `YYYY-MM-DD` y hora `HH:mm`, en hora local de `America/Bogota` (D-007).
- **Implementación:** `infrastructure/adapters/in/web/professional/AvailabilityController.java`.

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/v1/professional/availability-blocks?date=&siteCode=` | Calendario propio, filtrable por día y sede |
| POST | `/api/v1/professional/availability-blocks` | Publica un bloque (`201`) |
| PATCH | `/api/v1/professional/availability-blocks/{id}` | Cambia horario o sede y **regenera los slots** |
| DELETE | `/api/v1/professional/availability-blocks/{id}` | Elimina el bloque y sus slots (`204`) |
| GET | `/api/v1/professional/me` | Perfil propio: estado, especialidad principal y sedes asignadas |

## Perfil propio

"Mi agenda" lo usa para ofrecer solo las sedes asignadas, mostrar la especialidad principal en la cabecera y avisar si la cuenta está inactiva.

```json
{
  "id": 12, "firstNames": "Laura", "lastNames": "Gómez", "active": true,
  "primarySpecialty": { "id": 4, "name": "Cardiología", "durationMinutes": 60 },
  "sites": [ { "code": "HIC", "name": "Hospital Internacional de Colombia" } ]
}
```

Las sedes salen en el orden del catálogo.

## Crear un bloque

```json
POST /api/v1/professional/availability-blocks
{ "date": "2026-09-24", "startTime": "08:00", "endTime": "12:00", "siteCode": "HIC" }
```

Respuesta `201`:

```json
{ "id": 7, "siteCode": "HIC", "date": "2026-09-24", "startTime": "08:00", "endTime": "12:00", "slots": 8 }
```

`slots` es el número de franjas de 30 minutos que se materializan en la base: 08:00–12:00 son 8.

## Reglas

| Regla | Respuesta si se incumple | Dónde se garantiza |
|---|---|---|
| No publicar en el pasado | `400`, `field: startTime` | Solo en la aplicación: MySQL no admite `NOW()` en un `CHECK` |
| Horas en punto o y media | `400`, `field: startTime` / `endTime` | Dominio + `CHECK` de alineación |
| Fin posterior al inicio | `400`, `field: endTime` | Dominio + `CHECK chk_blocks_range` |
| El bloque no cruza la medianoche | `400`, `field: endTime` | Dominio + `CHECK chk_blocks_same_day` |
| Sin solapamiento con otro bloque propio, **incluso en otra sede** | `400`, `field: startTime` | Aplicación + `uk_slots_professional_start` |
| El profesional está asignado a esa sede | `400`, `field: siteCode` | Aplicación + FK compuesta `fk_blocks_professional_site` |
| El profesional está activo | `400`, `field: professional` | Aplicación |
| No se edita ni elimina un bloque con slots reservados o retenidos (CA-05) | `409 BLOCK_HAS_APPOINTMENTS` | Aplicación + FK `fk_sr_slot` `ON DELETE RESTRICT` desde `slot_reservations` |

Dos bloques **contiguos** (08:00–12:00 y 12:00–14:00) son válidos: no se solapan.

## Errores

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Cualquier regla `400` de la tabla anterior |
| 401 | `UNAUTHORIZED` | Sin access token válido |
| 403 | `FORBIDDEN` | Autenticado sin rol `PROFESSIONAL` |
| 404 | `NOT_FOUND` | Bloque inexistente, o de otro profesional |
| 409 | `BLOCK_HAS_APPOINTMENTS` | El bloque tiene citas (`PATCH` o `DELETE`) |
