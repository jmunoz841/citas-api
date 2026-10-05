# Plan S5–S6 — Automatizaciones n8n con agente conectado por MCP

Fuentes: `GUIA_SESIONES_S2_S6.md` (S5, S6), `PRD.md` §10, `RESTRICCIONES_TECNICAS.md` (§ n8n y § Variables
de entorno), HU-023, HU-024 y HU-025, y las indicaciones del profesor:

1. terminar el backend para conocer los endpoints;
2. pedir el plan y tres prompts, uno por automatización, que se ejecutan por MCP;
3. crear en `.env` las tres variables que usan esos workflows;
4. exportar el JSON de n8n a `automations/n8n/` y revisar que se pueda publicar en GitHub.

## Decisiones del equipo (2026-10-04)

| Tema | Decisión |
|---|---|
| Instancia n8n | La del profesor (central). n8n → API requiere un túnel HTTPS hacia `localhost:8081` (p. ej. `cloudflared tunnel --url http://localhost:8081`); API → n8n usa la URL pública del webhook |
| Destinatario de prueba | Paciente sintético registrado con el Gmail del estudiante con alias (`usuario+paciente@gmail.com`); el resto de datos son ficticios |
| Alcance | HU-023, HU-024 y HU-025 aprobadas (HU-025 es bonus) |
| Identificación en la instancia compartida | Todos los workflows llevan **"JuanCarlos Muñoz"** en el nombre (p. ej. `WF-001 Recordatorios de citas — JuanCarlos Muñoz`) y la ruta del webhook de WF-002 usa el prefijo propio `jcmunoz-` para no chocar con los de otros estudiantes |

## Arquitectura de la integración

```text
            (túnel HTTPS)                               (URL pública del webhook)
n8n WF-001/WF-003 ──X-Api-Key──▶ citas-api /api/v1/integrations/**
citas-api (tras commit) ──POST firmado HMAC──▶ n8n WF-002 webhook ──▶ Gmail
```

### Variables de entorno (`citas-api/.env`, nunca versionado)

| Variable | Lado | Uso |
|---|---|---|
| `INTEGRATION_API_KEY` | n8n → API | Clave que n8n envía en `X-Api-Key`; da solo el rol `INTEGRATION`, válido únicamente en `/api/v1/integrations/**` (privilegio mínimo). Vacía = integración deshabilitada |
| `N8N_WEBHOOK_URL` | API → n8n | URL de producción del webhook de WF-002. Vacía = no se emiten eventos |
| `N8N_WEBHOOK_SECRET` | API → n8n | Secreto para firmar el cuerpo (`X-Citas-Signature: sha256=<HMAC>`); WF-002 lo verifica |

En n8n, la clave y el secreto viven en **credenciales** (Header Auth / Crypto), nunca en el JSON del workflow.

### Endpoints nuevos (backend)

| HU | Método y ruta | Qué devuelve |
|---|---|---|
| HU-023 | `GET /api/v1/integrations/reminders?hours=24` | Citas `APPROVED` que empiezan en las próximas `hours` horas y aún no tienen recordatorio para ese horario |
| HU-023 | `POST /api/v1/integrations/reminders/{appointmentId}/sent` | Marca el recordatorio como enviado (evita duplicados; si la cita se reprograma, el nuevo horario vuelve a ser recordable) |
| HU-024 | Webhook saliente (no es endpoint) | Eventos `SPECIALIZED_APPROVED`, `SPECIALIZED_REJECTED`, `RESCHEDULE_APPROVED`, `RESCHEDULE_REJECTED`, `APPOINTMENT_CANCELLED` tras confirmar la transacción; un fallo no revierte la transición |
| HU-025 | `GET /api/v1/integrations/daily-summary?date=` | Conteos del día por sede, estado y especialidad, más solicitudes y reprogramaciones pendientes; sin datos personales |

## Secuencia

| # | Paso | Quién | Sesión |
|---|---|---|---|
| 1 | Backend de integración (endpoints, webhook, migración, pruebas, contrato) | Agente | S5 |
| 2 | Variables en `.env` con valores generados (sin mostrarlos) | Agente | S5 |
| 3 | Prompts WF-001, WF-002, WF-003 (`automations/n8n/prompts/`) | Agente | S5 |
| 4 | Acceso a la instancia del profesor, API key de n8n y MCP en Claude Code | Estudiante + agente | S5 |
| 5 | Credencial Gmail OAuth propia y credenciales Header Auth / secreto en n8n | Estudiante | S5 |
| 6 | Túnel hacia la API y WF-001 por MCP; ejecución controlada; exportar JSON | Agente vía MCP | S5 |
| 7 | Seguridad: contenido no confiable (issue, comentario de revisión, README de dependencia, respuesta MCP), demo de issue envenenado, riesgos residuales | Agente + estudiante | S5 |
| 8 | WF-002 por MCP (webhook firmado) y WF-003 (bonus); exportar JSON | Agente vía MCP | S6 |
| 9 | Revisión de publicabilidad de los JSON (sin credenciales, IDs ni datos personales) | Agente | S5/S6 |
| 10 | Commits S5 y S6, merge a `main` cuando el estudiante decida | Agente | S5/S6 |

No se activa ningún workflow sin una ejecución controlada que muestre la salida esperada.
