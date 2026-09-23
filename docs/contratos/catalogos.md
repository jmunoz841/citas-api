# Contrato REST — Catálogos fijos (HU-005)

- **Base URL (local):** `http://localhost:8081` (`API_PORT`, D-010)
- **Formato:** JSON UTF-8. Errores en `application/problem+json` (RFC 9457).
- **Versionado:** todos los endpoints bajo `/api/v1/` (D-018).
- **Autenticación:** **no requerida**. Son datos de referencia no sensibles y el formulario de registro necesita los tipos de documento y las sedes antes de que exista una sesión.
- **Solo lectura:** no existen POST, PUT, PATCH ni DELETE. Un intento de escritura con sesión válida obtiene `405`; sin sesión, `401`. Los valores solo cambian con una migración Flyway.
- **Implementación:** `infrastructure/adapters/in/web/catalog/CatalogController.java`.

## Endpoints

| Método | Ruta | Contenido |
|---|---|---|
| GET | `/api/v1/catalogs/sites` | Sedes de atención (HIC, ICV) |
| GET | `/api/v1/catalogs/document-types` | Tipos de documento (CC, CE, TI, RC, PA, PPT) |
| GET | `/api/v1/catalogs/regimes` | Regímenes de afiliación |
| GET | `/api/v1/catalogs/insurance-plans` | Planes de EPS seleccionables (HU-004) |
| GET | `/api/v1/catalogs/roles` | Roles del sistema |
| GET | `/api/v1/catalogs/appointment-statuses` | Estados del ciclo de vida de una cita |
| GET | `/api/v1/catalogs/reschedule-statuses` | Estados de una solicitud de reprogramación |

Todas responden `200` con la misma envoltura: un objeto con la clave `items`, para poder añadir metadatos más adelante sin romper el contrato. Los elementos vienen ordenados por `code`.

## Formatos de respuesta

### Catálogos de código y nombre

`document-types`, `regimes` y `roles`:

```json
{
  "items": [
    { "code": "CC", "name": "Cédula de ciudadanía" },
    { "code": "CE", "name": "Cédula de extranjería" }
  ]
}
```

### Sedes

```json
{
  "items": [
    {
      "code": "HIC",
      "name": "Hospital Internacional de Colombia",
      "address": "Km 7 Autopista Bucaramanga–Piedecuesta, Valle de Menzulí, Santander"
    },
    {
      "code": "ICV",
      "name": "Fundación Cardiovascular de Colombia / Instituto Cardiovascular",
      "address": "Calle 155A No. 23-58, Urbanización El Bosque, Floridablanca, Santander"
    }
  ]
}
```

### Planes de EPS

Solo aparecen los planes **seleccionables**: el plan debe estar activo **y** su EPS también. Un plan vigente de una EPS dada de baja no se ofrece. El identificador es numérico, no un código.

```json
{
  "items": [
    { "id": 1, "name": "Plan Básico", "epsId": 1, "epsName": "EPS Salud Sintética" }
  ]
}
```

Los datos son sintéticos: ninguna EPS real de Colombia. Su CRUD administrativo llega con HU-007.

### Estados

`appointment-statuses` y `reschedule-statuses` añaden `terminal`, que indica si el estado admite transiciones posteriores. El cliente lo usa para decidir qué acciones ofrecer.

```json
{
  "items": [
    { "code": "APPROVED", "name": "Aprobada", "terminal": false },
    { "code": "CANCELLED", "name": "Cancelada", "terminal": true }
  ]
}
```

## Valores sembrados

| Catálogo | Valores | Migración |
|---|---|---|
| `roles` | `USER`, `PROFESSIONAL`, `ADMIN` | V1 |
| `document_types` | `CC`, `CE`, `TI`, `RC`, `PA`, `PPT` | V1 |
| `regimes` | `CONTRIBUTIVO`, `SUBSIDIADO` | V2 |
| `sites` | `HIC`, `ICV` | V2 |
| `appointment_statuses` | `REQUESTED`, `APPROVED` (no terminales); `REJECTED`, `CANCELLED`, `COMPLETED`, `NO_SHOW` (terminales) | V2 |
| `reschedule_statuses` | `PENDING` (no terminal); `APPROVED`, `REJECTED`, `CANCELLED` (terminales) | V2 |

## Errores

| HTTP | `code` | Cuándo |
|---|---|---|
| 401 | `UNAUTHORIZED` | Método de escritura sin sesión válida |
| 404 | — | Catálogo inexistente |
| 405 | — | Método de escritura sobre un catálogo existente, con sesión válida |
