# Contrato REST — Autenticación (HU-001)

- **Base URL (local):** `http://localhost:8081` (`API_PORT`, D-010)
- **Formato:** JSON UTF-8. Errores en `application/problem+json` (RFC 9457).
- **CORS:** solo el origen `FRONTEND_ORIGIN` (por defecto `http://localhost:5173`); cabeceras `Authorization` y `Content-Type`.
- **Autenticación:** `Authorization: Bearer <accessToken>` en rutas protegidas.
- **Implementación:** `infrastructure/adapters/in/web/auth/AuthController.java`.

## Endpoints

| Método | Ruta | Auth | Éxito |
|---|---|---|---|
| POST | `/api/auth/register` | Pública | `201` + usuario |
| POST | `/api/auth/login` | Pública | `200` + tokens |
| POST | `/api/auth/refresh` | Pública (requiere refresh token) | `200` + tokens nuevos |
| POST | `/api/auth/logout` | Pública (requiere refresh token) | `204` sin cuerpo |
| GET | `/api/auth/session` | Access token | `200` + datos de sesión |
| GET | `/actuator/health` | Pública | `200` |

### POST `/api/auth/register`

```json
{
  "firstNames": "Ana",
  "lastNames": "Pérez",
  "documentType": "CC",
  "documentNumber": "1234567",
  "email": "ana@example.com",
  "phone": "3001234567",
  "password": "Segura123"
}
```

Reglas:
- `documentType`: `CC`, `CE`, `TI`, `RC`, `PA`, `PPT`.
- `documentNumber`: se normaliza (sin puntos, guiones ni espacios, en mayúsculas); 3–30 letras o dígitos.
- `email`: se normaliza a minúsculas; único sin distinguir mayúsculas.
- `phone`: 7–20 caracteres entre dígitos y `+ ( ) -` o espacio.
- `password`: mínimo 8 caracteres, al menos una letra y un número, máximo 72 bytes.
- El rol asignado siempre es `USER`; no se acepta rol desde el cliente.

Respuesta `201`:

```json
{
  "id": 1,
  "firstNames": "Ana",
  "lastNames": "Pérez",
  "documentType": "CC",
  "documentNumber": "1234567",
  "email": "ana@example.com",
  "phone": "3001234567",
  "roles": ["USER"]
}
```

### POST `/api/auth/login`

```json
{ "email": "ana@example.com", "password": "Segura123" }
```

Respuesta `200` (también para `/refresh`):

```json
{
  "tokenType": "Bearer",
  "accessToken": "<JWT>",
  "expiresIn": 900,
  "refreshToken": "<JWT>",
  "refreshExpiresIn": 604800
}
```

- `expiresIn` / `refreshExpiresIn` en segundos (`JWT_ACCESS_MINUTES`, `JWT_REFRESH_DAYS`).
- Claims del access token: `sub` (id de usuario), `email`, `roles`, `typ=access`, `iss=citas-api`, `jti`, `iat`, `exp`.
- El refresh token lleva `typ=refresh` y se firma con otro secreto; el servidor solo guarda su hash SHA-256.

### POST `/api/auth/refresh`

```json
{ "refreshToken": "<JWT>" }
```

Rotación: devuelve un par nuevo y revoca el refresh token recibido. Reutilizar un refresh token ya rotado o revocado → `401`.

### POST `/api/auth/logout`

```json
{ "refreshToken": "<JWT>" }
```

Revoca el refresh token. Idempotente: responde `204` aunque el token no exista o ya esté revocado. El access token emitido sigue siendo válido hasta su expiración (máx. `JWT_ACCESS_MINUTES`); el cliente debe descartarlo.

### GET `/api/auth/session`

Respuesta `200`:

```json
{ "userId": 1, "email": "ana@example.com", "roles": ["USER"] }
```

## Errores

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Datos inválidos",
  "instance": "/api/auth/register",
  "code": "VALIDATION_ERROR",
  "errors": [{ "field": "password", "message": "La contraseña debe contener al menos un número" }]
}
```

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Campos faltantes o inválidos (`errors` por campo) |
| 400 | `MALFORMED_REQUEST` | Cuerpo que no es JSON válido |
| 401 | `INVALID_CREDENTIALS` | Email inexistente, contraseña incorrecta o cuenta inactiva (mismo mensaje) |
| 401 | `INVALID_REFRESH_TOKEN` | Refresh token inexistente, expirado, revocado, rotado o de tipo access |
| 401 | `UNAUTHORIZED` | Ruta protegida sin access token válido |
| 403 | `FORBIDDEN` | Autenticado sin el rol requerido |
| 409 | `EMAIL_ALREADY_REGISTERED` | Email ya registrado |
| 409 | `DOCUMENT_ALREADY_REGISTERED` | Tipo + número de documento ya registrado |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `Content-Type` distinto de JSON |
| 500 | `INTERNAL_ERROR` | Error no controlado (sin detalles internos) |
