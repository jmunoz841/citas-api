---
tipo: ejecucion
actualizado: 2026-09-23
fuentes:
  - raw/RESTRICCIONES_TECNICAS.md
---

# Ejecución

Estado del avance por sesión. Evidencia mínima según `EVIDENCIAS_Y_TRAZABILIDAD.md` (raíz del workspace).

## S2 — Especificar, inicializar y construir el primer incremento

HU objetivo: [[HU-001-registro-e-inicio-de-sesion-jwt]] (`Aprobada`).

| # | Actividad | Estado | Evidencia |
|---|---|---|---|
| 1 | Preparar repositorios | Hecho | `citas-api` 56fc6b9, `citas-web` 0efa66a; `main` y `develop` publicados en GitHub |
| 2 | Especificación Scrum | Hecho | `docs/wiki/scrum/` — 8 épicas, 25 HU |
| 3 | Definir alcance S2 | Hecho | HU-001 `Aprobada`; resto `Borrador` |
| 4 | AGENTS raíz y wiki | Hecho | `AGENTS.md` raíz; `docs/wiki/llm-wiki/` inicializada |
| 5 | Normalización 3FN delegada | Hecho | Diseño propio (23 tablas) en `docs/database/normalizacion-3fn/`; `schema.sql` cargado sin errores en MySQL 8.4.11 + 9 pruebas de restricciones de HU-001 OK; `comparacion-referencia.md`; decisiones D-012, D-013 |
| 6 | Inicializar backend Spring Boot | Hecho | `pom.xml` (Boot 3.5.16, Java 21), Maven Wrapper 3.9.16, paquetes hexagonales, `HexagonalArchitectureTest`; `mvnw test` → 2 tests, BUILD SUCCESS |
| 7 | MySQL + Flyway | Hecho | `V1__identidad_hu001.sql` aplicada en `jmunoz-citas-mysql` (3308): `flyway_schema_history` v1 success; tablas roles, document_types, users, user_roles, refresh_tokens; seeds 3 roles + 6 tipos de documento; 0 usuarios; API arrancó en 8081 |
| 8 | Vertical slice de autenticación | Hecho | Registro, login, refresh con rotación, logout, `/api/auth/session`, errores ProblemDetail, CORS; humo manual 20/20 contra MySQL; contrato `docs/contratos/autenticacion.md`; HU-001 `En desarrollo` (T-01…T-07, T-09) |
| 9 | Verificación backend (`mvn test`) | Hecho | 54 pruebas / 0 fallos: dominio (19), `AuthServiceTest` (9), `JwtTokenProviderTest` (6), `AuthApiIntegrationTest` (18, Testcontainers MySQL 8.4 + Flyway), ArchUnit (2). Requiere Docker Desktop abierto |
| 10 | `citas-api/AGENTS.md` | Hecho | `AGENTS.md` (+ `CLAUDE.md` que lo importa) generado desde `PROMPT_AGENT_CITAS_API.md` con la estructura, comandos y convenciones reales; sustituye a `AGENTS.md.template` |
| 11 | Diseño Stitch login/registro | Hecho | "CitaClara", estilo señalética hospitalaria serena; v1 revisada (7 problemas) → v2 corregida y **aprobada** 2026-09-18; fuente de verdad `citas-web/docs/diseno/DESIGN.md` + `stitch-v2/`; registro en `APROBACION.md` |
| 12 | Handoff AI Studio e importación | Hecho | Prompt de handoff desde el diseño v2; export de AI Studio revisado e importado en `citas-web` sin restos de plantilla (Gemini, Express, dotenv, motion, lucide, metadata) |
| 13 | Frontend ejecutable | Hecho | React 19 + Vite 8 + TS estricto + Tailwind 4; `npm run typecheck`/`build` en verde; puerto 5174; E2E contra `citas-api` 8081: CORS 5174 OK (5173 rechazado), registro/login/session/refresh/logout OK; capturas headless escritorio/tablet/móvil fieles a v2 |
| 14 | `citas-web/AGENTS.md` | Hecho | Generado desde `PROMPT_AGENT_CITAS_WEB.md` con stack, estructura, mapeo de errores, tokens y verificación reales (+ `CLAUDE.md`); no existía versión previa (el historial solo tenía `AGENTS.md.template`) |
| 15 | GOAL_01 sobre HU-001 | Hecho | `/goal` ejecutado por el usuario en otra sesión de Claude Code: las 6 condiciones verificadas; agregó la prueba de refresh expirado en BD (CA-10) → `mvnw test` 55/55; commit `a5f583e` |
| 16 | Cierre: evidencia, wiki, README, commits `feat(s2): bootstrap specs auth and frontend baseline` | Hecho | HU-001 `Completada` con matriz de evidencia (11 CA + 7 DoD); `mvnw test` 54/54; [[evidencia-s2]]; commits de cierre `bc79eaa` (api) y `ee952eb` (web) |

## S3 — Core de agendamiento (desde 2026-09-23)

| # | Paso | Estado | Evidencia |
|---|---|---|---|
| 1 | Cierre de S2 y apertura de S3 | Hecho | Merge `develop` → `main` y etiqueta `s2` en ambos repos (**sin push**: las credenciales de Git del equipo son de otra cuenta de GitHub). Diez HU `Aprobadas`; HU-004 recortada a afiliación opcional; decisiones D-018 a D-024 |
| 2 | Puertas de calidad | Hecho | `.githooks/pre-commit` versionado en ambos repos (D-024): detector de secretos en dos niveles siempre, más `mvnw test` (backend) o lint/pruebas/build (frontend). Vitest + Testing Library + oxlint en `citas-web` (D-023). **Demo FAIL/PASS real:** commit con `apiKey` falsa bloqueado; commit legítimo aceptado (`7eecf11`) |
| 3 | HU-005 Catálogos fijos | Hecho | Migración `V2__catalogos_fijos_hu005.sql` (regimes, sites, appointment_statuses, reschedule_statuses + seeds del PRD); 6 endpoints GET públicos bajo `/api/v1/catalogs`; 9 pruebas de integración (CA-01…CA-03); `mvnw test` 64/64. Contrato `docs/contratos/catalogos.md`. Verificado contra MySQL real: `flyway_schema_history` v1 y v2 con `success=1` |
| 4 | Versionado de API (D-018) | Hecho | `/api/auth/*` → `/api/v1/auth/*` en controlador, `SecurityConfig`, pruebas, contrato y cliente del frontend |
| 5 | Frontend sin mock | Hecho | Eliminado `mockAuthApi` y su *fallback* silencioso (servía una cuenta de demo con contraseña en el código si faltaba `VITE_API_URL`); cliente de catálogos y `useDocumentTypes`; el select de tipo de documento ya viene de la API; 15 pruebas Vitest |
| 6 | HU-004 Afiliación opcional (GOAL del instructor) | Hecho | Migración `V3__afiliacion_hu004.sql` (eps, eps_plans, user_affiliations + seed sintético con un plan inactivo y una EPS inactiva); `GET /api/v1/catalogs/insurance-plans`; registro con `insurancePlanId` y `regimeCode` en pareja; sección "Afiliación (opcional)" en el registro de `citas-web`. `mvnw test` 78/78 y `npm test` 22/22. Flyway v3 `success=1` en MySQL real |
| 7 | HU-006, HU-008 y HU-009 Oferta administrable (backend) | Hecho | Migración `V4__oferta_administrable_hu006_hu008.sql` con los dos índices funcionales del diseño 3FN (una sola especialidad general; una sola primaria por profesional), semilla de `Medicina General` y **ADMIN inicial** (D-021). Endpoints `/api/v1/admin/**` con `hasRole('ADMIN')`; sin DELETE de especialidades. 17 pruebas de integración; `mvnw test` 95/95. Humo contra MySQL real: login del ADMIN sembrado, duración 45 rechazada, 401 sin token, Flyway v1–v4 `success=1`. Contrato `docs/contratos/administracion.md` |
| 8 | HU-010 Disponibilidad (backend) | Hecho | Migración `V5__disponibilidad_hu010.sql`; slots materializados de 30 min; 4 endpoints bajo `/api/v1/professional/**` con `hasRole('PROFESSIONAL')` y pertenencia por token; 13 pruebas de integración; `mvnw test` 108/108. Contrato `docs/contratos/disponibilidad.md`. CA-05 pendiente hasta HU-013 |

Pendiente de S3: las **vistas de ADMIN y de PROFESSIONAL** en `citas-web` (HU-006 T-04, HU-008 T-03, HU-009 T-02, HU-010 T-04), HU-012/013/014 (búsqueda y reserva, con el LOOP de doble reserva) y HU-015 (decisión administrativa). HU-009 CA-02 (exclusión de la búsqueda) y HU-010 CA-05 (bloque con slots comprometidos) solo serán verificables con HU-012 y HU-013.

## Punto de retoma (fin de clase 2026-09-23)

### 0. Publicar en GitHub — bloqueante, primero que todo

Nada de S2 ni de S3 está publicado. En este equipo el Administrador de credenciales de Windows guarda el token de **otra cuenta** (`christtobar-land`), que no tiene permiso sobre `jmunoz841/*`: el push falla con `403`. Los commits sí están firmados correctamente como `Juan Munoz <jmunoz841@unab.edu.co>`; el problema es solo la credencial de red.

Ya quedó configurado en los tres repos, en `.git/config` local (no global, para no romper la sesión del otro estudiante):

```text
credential.https://github.com.username = jmunoz841
```

Con eso Git pide una credencial nueva bajo la clave `jmunoz841@github.com` en vez de reutilizar la ajena. Falta autenticarse una vez, desde una terminal propia (abre navegador o pide token):

```powershell
cd "...\FCV_Proyecto_Citas_v1\citas-api"
git push origin develop
git push origin main
git push origin s2
cd ..\citas-web
git push origin develop; git push origin main; git push origin s2
```

Si pide contraseña en vez de abrir el navegador, generar un **Personal Access Token** en GitHub (`Settings → Developer settings → Tokens`) con permiso `repo` y pegarlo como contraseña. Nunca escribir el token en la URL del remoto ni en un archivo versionado.

Pendiente de publicar: `citas-api` 13 commits en `develop`, 17 en `main`, tag `s2`; `citas-web` 4 commits en `develop`, 7 en `main`, tag `s2`. El repo raíz ya está sincronizado.

### 1. Preparar el equipo

Igual que en el punto de retoma anterior: `git pull` en los tres repos, JDK 21 portable en `%USERPROFILE%\.jdks\temurin-21`, `.env` en raíz, `citas-api` y `citas-web`, Docker Desktop abierto y `docker compose up -d` desde `citas-api/` (proyecto `jmunoz-citas`, MySQL en 3308). Verificar con `$env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21"; .\mvnw.cmd test` → deben pasar 108 pruebas.

### 2. Lo que falta de S3

| Orden | Trabajo | Alcance | Desbloquea |
|---|---|---|---|
| 1 | [[HU-012-consultar-disponibilidad]], [[HU-013-reservar-cita-general]], [[HU-014-solicitar-cita-especializada]] | Migración `V6`: `appointments`, `slot_reservations`, `appointment_status_history`. Búsqueda de disponibilidad, reserva general `APPROVED` y solicitud especializada `REQUESTED` | El **LOOP de doble reserva** del instructor: dos reservas sobre el mismo slot → una `201`, otra `409`, garantizado por clave primaria en la base |
| 2 | [[HU-015-resolver-cita-especializada]] | Decisión del ADMIN con motivo obligatorio y liberación de slots al rechazar | Cierra el flujo de cita especializada |
| 3 | Pasada de frontend | Vistas de ADMIN (especialidades, profesionales, asignaciones, solicitudes pendientes), vista de PROFESSIONAL (calendario de bloques) y modal de reserva en 4 pasos para USER | Tareas [[HU-006-gestionar-especialidades]] T-04, [[HU-008-crear-profesional]] T-03, [[HU-009-activar-desactivar-profesional]] T-02, [[HU-010-gestionar-bloques-de-disponibilidad]] T-04 |
| 4 | Evidencia de cierre de S3 | Matriz por CA y DoD, demo Red→Green del hook, trazabilidad final | Entregable de la sesión |

### 3. Criterios diferidos a propósito

Dos criterios quedaron en `Pendiente` con la razón escrita en su HU; no son deuda olvidada, esperan a que exista la tabla que los hace verificables:

- [[HU-009-activar-desactivar-profesional]] CA-02 — un profesional inactivo no aparece en la búsqueda. Necesita HU-012.
- [[HU-010-gestionar-bloques-de-disponibilidad]] CA-05 — no se edita ni elimina un bloque con slots comprometidos. Necesita `slot_reservations` de HU-013. El guardián ya existe vacío y documentado en `AvailabilityService.requireNoCommittedSlots`.

### 4. Estado al cerrar

Backend de S3 completo hasta disponibilidad: migraciones `V1`–`V5` aplicadas, 108 pruebas en verde, contratos documentados en `docs/contratos/`. El frontend solo tiene login y registro con afiliación opcional (22 pruebas). Diez HU en estado `Aprobada`.


## Relacionadas

[[arquitectura]] · [[decisiones]]
