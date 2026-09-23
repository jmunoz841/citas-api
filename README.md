# citas-api

API REST del sistema académico de agendamiento de citas **CitaClara**: Java 21, Spring Boot 3.5, Maven, arquitectura hexagonal, MySQL 8.4 + Flyway, Spring Security con JWT access/refresh.

- Instrucciones para agentes: [`AGENTS.md`](AGENTS.md)
- Contrato REST: [`docs/contratos/`](docs/contratos/)
- Especificación Scrum: [`docs/wiki/scrum/`](docs/wiki/scrum/)
- LLM Wiki global del workspace: [`docs/wiki/llm-wiki/`](docs/wiki/llm-wiki/)
- Diseño de base de datos (3FN): [`docs/database/normalizacion-3fn/`](docs/database/normalizacion-3fn/)
- Automatizaciones n8n (S5/S6): `automations/n8n/`

## Requisitos

- JDK 21 (en el equipo del laboratorio: `%USERPROFILE%\.jdks\temurin-21`)
- Docker Desktop (MySQL local y Testcontainers en las pruebas)

## Configuración: `.env`

La configuración vive en un único archivo **`.env`** en esta carpeta. Es local y **no se versiona** (contiene secretos). En un equipo nuevo, créalo con estas variables:

| Variable | Valor en desarrollo | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de MySQL |
| `DB_PORT` | `3308` | Puerto de MySQL en el host (`docker-compose.yml` lo publica ahí) |
| `DB_NAME` | `citas_fcv_training` | Base de datos |
| `DB_USER` | `citas_app` | Usuario de la aplicación |
| `DB_PASSWORD` | *(secreto)* | Contraseña del usuario de la aplicación |
| `MYSQL_ROOT_PASSWORD` | *(secreto)* | Contraseña de root del contenedor MySQL |
| `JWT_ACCESS_SECRET` | *(secreto, ≥ 32 bytes)* | Firma de los access tokens |
| `JWT_REFRESH_SECRET` | *(secreto, ≥ 32 bytes, distinto del anterior)* | Firma de los refresh tokens |
| `JWT_ACCESS_MINUTES` | `15` | Vigencia del access token |
| `JWT_REFRESH_DAYS` | `7` | Vigencia del refresh token |
| `FRONTEND_ORIGIN` | `http://localhost:5174` | Origen permitido por CORS (`citas-web`) |
| `API_PORT` | `8081` | Puerto HTTP de la API |

Para generar secretos aleatorios en PowerShell:

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b) -replace '[+/=]',''
```

Las contraseñas de MySQL solo se aplican al crear el volumen `jmunoz-citas_mysql_data`. Si cambias `DB_PASSWORD` o `MYSQL_ROOT_PASSWORD` con un volumen existente, recréalo con `docker compose down -v` (borra los datos locales).

## Ejecutar

```powershell
$env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21"; $env:Path="$env:JAVA_HOME\bin;$env:Path"
docker compose up -d          # MySQL propio en localhost:3308
.\mvnw.cmd spring-boot:run    # API en http://localhost:8081 (aplica migraciones Flyway)
.\mvnw.cmd test               # pruebas (requiere Docker Desktop: Testcontainers)
```

**No** uses el `docker-compose.yml` de la raíz del workspace: sus contenedores `fcv-citas-*` chocan con otro grupo que comparte el equipo.

## Cuenta ADMIN de laboratorio

La migración `V4` siembra un ADMIN inicial, porque sin él nadie podría crear el primer profesional (D-021):

| Campo | Valor |
|---|---|
| Email | `admin@citas.local` |
| Contraseña | `Admin.Lab2026` |

Son credenciales **de laboratorio con datos sintéticos**, pensadas para que cualquiera pueda levantar el proyecto y reproducir las pruebas. La contraseña se guarda solo como hash BCrypt. **Antes de exponer la API fuera de tu máquina, cámbiala**: este repositorio es público y cualquiera puede leer este README.

## Hooks de calidad

Los hooks viven en `.githooks/` y están versionados, pero Git no los activa solo. Una vez por clon:

```powershell
git config core.hooksPath .githooks
```

`pre-commit` hace dos cosas:

1. **Detector de secretos** sobre los archivos preparados, en cada commit. Bloquea siempre los archivos `.env`, las claves privadas y las credenciales de nube o de conexión. Los patrones más genéricos (asignaciones de contraseñas y JWT) admiten exenciones por ruta en `.githooks/secrets-allowlist.txt`, donde cada entrada explica por qué ese contenido es ficticio.
2. **`mvnw test`**, solo si el commit toca `src/` o `pom.xml`. Requiere Docker Desktop porque las pruebas de integración levantan MySQL con Testcontainers. Un commit de documentación no paga ese coste.

Si necesitas saltártelo en una emergencia, `git commit --no-verify`, y deja constancia del motivo.
