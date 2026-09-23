# Contrato REST — Administración de la oferta (HU-006, HU-008, HU-009)

- **Base URL (local):** `http://localhost:8081` (`API_PORT`, D-010)
- **Versionado:** todos los endpoints bajo `/api/v1/` (D-018).
- **Autorización:** **todo `/api/v1/admin/**` exige rol `ADMIN`.** Sin token → `401 UNAUTHORIZED`; con token de otro rol → `403 FORBIDDEN`.
- **Implementación:** `infrastructure/adapters/in/web/admin/`.

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/v1/admin/specialties?onlyActive=` | Lista especialidades; por defecto incluye las inactivas |
| POST | `/api/v1/admin/specialties` | Crea una especialidad (`201`) |
| PATCH | `/api/v1/admin/specialties/{id}` | Cambia nombre o duración |
| PATCH | `/api/v1/admin/specialties/{id}/active` | Activa o desactiva |
| GET | `/api/v1/admin/professionals` | Lista profesionales con sus asignaciones |
| POST | `/api/v1/admin/professionals` | Crea el profesional y su cuenta (`201`) |
| PUT | `/api/v1/admin/professionals/{id}/specialties` | Reemplaza sus especialidades |
| PUT | `/api/v1/admin/professionals/{id}/sites` | Reemplaza sus sedes |
| PATCH | `/api/v1/admin/professionals/{id}/active` | Activa o desactiva |

**No existe DELETE.** Una especialidad referenciada por profesionales o citas no se borra: se desactiva, para que el historial siga siendo legible (HU-006, CA-04). Un intento de DELETE devuelve `405`.

> La ruta de sedes es `/sites`, no `/locations`: el modelo propio llama `sites` a esa entidad y el catálogo ya se publica como `/api/v1/catalogs/sites`.

## Especialidades

```json
POST /api/v1/admin/specialties
{ "name": "Cardiología", "durationMinutes": 60 }
```

Respuesta `201`:

```json
{ "id": 4, "name": "Cardiología", "durationMinutes": 60, "general": false, "active": true }
```

Reglas:
- `durationMinutes`: **solo 30 o 60**. La agenda se discretiza en slots de 30 minutos, así que no hay otras duraciones. Otro valor → `400` con `field: durationMinutes`.
- `name` único → `409 SPECIALTY_NAME_ALREADY_REGISTERED`.
- `general` no se puede fijar desde la API: la única especialidad general es `Medicina General`, sembrada por la migración V4 y protegida por un índice funcional en la base.
- `PATCH /{id}` acepta campos parciales: lo que no se envía no cambia.

## Profesionales

```json
POST /api/v1/admin/professionals
{
  "firstNames": "Carlos", "lastNames": "Rivera",
  "documentType": "CC", "documentNumber": "1098765432",
  "email": "carlos@ejemplo.local", "phone": "3001234567",
  "temporaryPassword": "Temporal123",
  "professionalCode": "PRO-001", "licenseNumber": "MAT-001",
  "specialties": [{ "specialtyId": 4, "primary": true }],
  "siteCodes": ["HIC", "ICV"]
}
```

Respuesta `201` (nunca incluye la contraseña ni su hash):

```json
{
  "id": 12, "firstNames": "Carlos", "lastNames": "Rivera", "email": "carlos@ejemplo.local",
  "professionalCode": "PRO-001", "licenseNumber": "MAT-001", "active": true,
  "specialties": [{ "specialtyId": 4, "primary": true }],
  "siteCodes": ["HIC", "ICV"]
}
```

Reglas:
- Crea a la vez la cuenta con rol `PROFESSIONAL` y los datos del profesional, en una sola transacción. El profesional puede iniciar sesión con su contraseña temporal, que cumple la misma política que cualquier otra (mínimo 8, con letra y número).
- **Al menos una especialidad y exactamente una primaria.** Ninguna o varias primarias → `400` con `field: specialties`. La base también impide más de una primaria con un índice funcional.
- **Al menos una sede.** Ninguna → `400` con `field: siteCodes`.
- Las especialidades deben existir y estar **activas**; las sedes deben existir en el catálogo.
- `professionalCode` y `licenseNumber` se normalizan a mayúsculas y son únicos.
- `PUT /specialties` y `PUT /sites` **reemplazan** el conjunto completo, no añaden.

## Activación

```json
PATCH /api/v1/admin/professionals/{id}/active
{ "active": false }
```

Desactivar conserva datos, asignaciones e historial: solo excluye al profesional de la oferta (HU-009).

## Errores

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Duración inválida, sin primaria, varias primarias, sin sedes, especialidad inactiva o sede inexistente |
| 401 | `UNAUTHORIZED` | Sin access token válido |
| 403 | `FORBIDDEN` | Autenticado sin rol `ADMIN` |
| 404 | `NOT_FOUND` | Especialidad o profesional inexistente |
| 405 | `METHOD_NOT_ALLOWED` | DELETE sobre una especialidad |
| 409 | `SPECIALTY_NAME_ALREADY_REGISTERED` | Nombre de especialidad repetido |
| 409 | `PROFESSIONAL_CODE_ALREADY_REGISTERED` | Código profesional repetido |
| 409 | `LICENSE_ALREADY_REGISTERED` | Matrícula repetida |
| 409 | `EMAIL_ALREADY_REGISTERED` / `DOCUMENT_ALREADY_REGISTERED` | Email o documento ya registrados |
