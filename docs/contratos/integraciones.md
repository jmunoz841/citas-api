# Contrato REST — Integración con n8n (HU-023, HU-024, HU-025)

- **Base URL:** la de la API. Para la instancia n8n del profesor, la URL pública de un túnel HTTPS
  hacia `localhost:8081` (p. ej. `cloudflared tunnel --url http://localhost:8081`).
- **Fechas y horas:** `YYYY-MM-DD` y `HH:mm`, hora local de `America/Bogota` (D-007).
- **Implementación:** `infrastructure/adapters/in/web/integration/IntegrationController.java`,
  `infrastructure/adapters/out/notification/N8nWebhookNotificationAdapter.java`.
- **Decisiones:** D-040 a D-043.

## Autenticación de n8n → API (D-042)

| Elemento | Valor |
|---|---|
| Cabecera | `X-Api-Key: <INTEGRATION_API_KEY>` |
| Alcance | Solo `/api/v1/integrations/**` (rol `INTEGRATION`). En cualquier otra ruta la cabecera se ignora |
| Sin cabecera o clave errónea | `401 UNAUTHORIZED` |
| Access token de persona (USER, ADMIN, PROFESSIONAL) | `403 FORBIDDEN` |
| `INTEGRATION_API_KEY` vacía | Integración deshabilitada: siempre `401` |

En n8n la clave se guarda en una credencial **Header Auth** (nombre `X-Api-Key`), nunca en el JSON.

## Recordatorios — WF-001 (HU-023)

### Listar citas por recordar

```http
GET /api/v1/integrations/reminders?hours=24
X-Api-Key: ***
```

`hours` es opcional (por defecto 24, entre 1 y 72; fuera de rango → `400`, `field: hours`). Devuelve
las citas `APPROVED` que empiezan en las próximas `hours` horas y **todavía no tienen recordatorio
para su horario actual**, la más próxima primero:

```json
{
  "items": [
    {
      "appointmentId": 31, "patientFirstNames": "Ana", "patientEmail": "usuario+paciente@gmail.com",
      "professionalName": "Laura Gómez", "specialtyName": "Cardiología",
      "siteCode": "HIC", "siteName": "Hospital Internacional de Colombia",
      "siteAddress": "Km 7 Autopista Bucaramanga–Piedecuesta, Valle de Menzulí, Santander",
      "date": "2026-10-06", "startTime": "08:00", "endTime": "09:00"
    }
  ]
}
```

No incluye citas `REQUESTED`, `REJECTED`, `CANCELLED`, `COMPLETED` ni `NO_SHOW`, ni citas fuera de la
ventana (HU-023 CA-02).

### Marcar un recordatorio como enviado

```http
POST /api/v1/integrations/reminders/31/sent
X-Api-Key: ***
```

Respuesta `200`: `{ "appointmentId": 31, "date": "2026-10-06", "startTime": "08:00", "alreadySent": false }`.

- Idempotente: repetirlo responde `200` con `alreadySent: true` y no duplica nada (tabla
  `appointment_reminders`, clave cita + horario, V10).
- Si la cita se reprograma, su nuevo horario vuelve a aparecer en el listado.
- Cita inexistente → `404 NOT_FOUND`; cita que no está `APPROVED` → `409 REMINDER_NOT_APPLICABLE`.

**Orden recomendado en WF-001:** listar → por cada cita enviar el Gmail → solo si el envío fue
exitoso, marcarla. Si Gmail falla, la cita sigue en el listado y se reintenta en la próxima ejecución.

## Webhook de cambio de estado — WF-002 (HU-024)

La API llama a n8n; no es un endpoint de la API.

| Elemento | Valor |
|---|---|
| Destino | `POST <N8N_WEBHOOK_URL>` (URL de producción del nodo Webhook de WF-002) |
| Cuándo | Después de confirmar la transacción de: aprobar o rechazar una cita especializada, aprobar o rechazar una reprogramación, cancelar una cita |
| Cabeceras | `Content-Type: application/json`, `X-Citas-Event: <eventType>`, `X-Citas-Event-Id: <uuid>`, `X-Citas-Signature: sha256=<hex>`, `X-Citas-Token: <N8N_WEBHOOK_SECRET>` |
| Autenticación en n8n | El nodo Webhook usa *Header Auth* con una credencial (nombre `X-Citas-Token`, valor `N8N_WEBHOOK_SECRET`): el secreto vive en la credencial y no en el JSON exportado. Por eso la URL debe ser HTTPS |
| Firma (integridad) | HMAC-SHA256 del **cuerpo crudo** con `N8N_WEBHOOK_SECRET`, en hexadecimal minúsculas. Verificable por cualquier receptor que tenga el secreto fuera del workflow |
| Timeouts | Conexión 3 s, respuesta 5 s; en un hilo aparte |
| Fallo de n8n | La transición ya está confirmada y no se revierte; se registra `id` y `tipo` del evento, sin datos personales (CA-02). No hay reintento desde la API |
| `N8N_WEBHOOK_URL` vacía | No se emite nada |

### Cuerpo

```json
{
  "eventId": "3f0c7a52-0b3e-4d0e-9f5e-2b1f9f0f6a11",
  "eventType": "RESCHEDULE_REJECTED",
  "occurredAt": "2026-10-04T16:20:11.123",
  "appointment": {
    "id": 31, "status": "APPROVED", "specialtyName": "Cardiología", "professionalName": "Laura Gómez",
    "siteCode": "HIC", "siteName": "Hospital Internacional de Colombia",
    "date": "2026-10-06", "startTime": "08:00", "endTime": "09:00"
  },
  "patient": { "firstNames": "Ana", "email": "usuario+paciente@gmail.com" },
  "reason": "El profesional no puede ese día",
  "requested": { "date": "2026-10-08", "startTime": "14:00", "siteCode": "ICV" },
  "previous": null
}
```

| `eventType` | `appointment.status` | `reason` | `requested` | `previous` |
|---|---|---|---|---|
| `SPECIALIZED_APPROVED` | `APPROVED` | `null` | `null` | `null` |
| `SPECIALIZED_REJECTED` | `REJECTED` | motivo | `null` | `null` |
| `RESCHEDULE_APPROVED` | `APPROVED` (ya en la nueva franja) | `null` | `null` | franja anterior |
| `RESCHEDULE_REJECTED` | `APPROVED` (conserva su franja) | motivo | franja pedida | `null` |
| `APPOINTMENT_CANCELLED` | `CANCELLED` | `null` | `null` | `null` |

El cuerpo nunca incluye tokens, contraseñas ni documentos de identidad.

### Verificación en WF-002

1. Autenticación del Webhook con la credencial *Header Auth* `X-Citas-Token`: n8n responde `403` sin
   ejecutar el workflow si la cabecera no coincide.
2. Validar que estén `eventId`, `eventType`, `appointment.id` y `patient.email`; si falta algo, `400`.
3. Responder `200` de forma determinista solo cuando el evento se procesó.

## Resumen diario — WF-003 (HU-025)

```http
GET /api/v1/integrations/daily-summary?date=2026-10-06
X-Api-Key: ***
```

`date` es opcional (por defecto hoy). Solo conteos, sin datos personales (CA-02):

```json
{
  "date": "2026-10-06", "total": 3,
  "byStatus": { "REQUESTED": 1, "APPROVED": 1, "REJECTED": 0, "CANCELLED": 1, "COMPLETED": 0, "NO_SHOW": 0 },
  "sites": [
    { "siteCode": "HIC", "siteName": "Hospital Internacional de Colombia", "total": 2,
      "byStatus": { "REQUESTED": 1, "APPROVED": 1, "REJECTED": 0, "CANCELLED": 0, "COMPLETED": 0, "NO_SHOW": 0 } },
    { "siteCode": "ICV", "siteName": "Fundación Cardiovascular de Colombia / Instituto Cardiovascular", "total": 1,
      "byStatus": { "REQUESTED": 0, "APPROVED": 0, "REJECTED": 0, "CANCELLED": 1, "COMPLETED": 0, "NO_SHOW": 0 } }
  ],
  "specialties": [ { "specialtyName": "Cardiología", "total": 3 } ],
  "pendingRequests": 4, "pendingReschedules": 1
}
```

- `sites` siempre incluye todas las sedes, aunque tengan 0.
- `pendingRequests`: citas especializadas `REQUESTED` futuras, de cualquier día.
- `pendingReschedules`: reprogramaciones `PENDING`, de cualquier día.

## Errores

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `hours` fuera de 1–72 o `date` con formato inválido |
| 401 | `UNAUTHORIZED` | Sin `X-Api-Key` válida |
| 403 | `FORBIDDEN` | Access token de persona en `/api/v1/integrations/**` |
| 404 | `NOT_FOUND` | Marcar el recordatorio de una cita inexistente |
| 409 | `REMINDER_NOT_APPLICABLE` | Marcar el recordatorio de una cita que no está `APPROVED` |
