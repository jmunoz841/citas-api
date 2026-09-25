---
tipo: ejecucion
actualizado: 2026-09-25
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
| 9 | HU-012, HU-013 y HU-014 Búsqueda y reserva (backend) | Hecho | Migración `V6__citas_hu012_hu014.sql` (`appointments`, `slot_reservations` con PK `slot_id`, `appointment_status_history`); `GET /api/v1/availability` y `POST /api/v1/appointments` con `hasRole('USER')`; `SlotPlanner` en el dominio (30/60 min, sin combinar bloques). **LOOP del instructor en verde:** dos reservas simultáneas por HTTP sobre el mismo slot → un 201 y un 409 `SLOT_UNAVAILABLE`, decidido por la PK. Cerrados los diferidos HU-009 CA-02 y HU-010 CA-05 (409 `BLOCK_HAS_APPOINTMENTS`). Corregido el 500 al desactivar o reasignar un profesional con agenda. 28 pruebas nuevas; `mvnw clean test` 136/136. Flyway v5 y v6 aplicadas en `jmunoz-citas-mysql`; health `UP`. Contrato `docs/contratos/citas.md` |
| 10 | HU-015 Decisión del ADMIN (backend) | Hecho | Pruebas escritas primero: **Red** 9 pruebas, 7 fallos (404, endpoints inexistentes) → **Green** 13/13. `GET /api/v1/admin/appointments/requests`, `POST .../{id}/approve` y `.../{id}/reject` (motivo obligatorio); rechazar libera los slots; historial `source: ADMIN`. La transición es un `UPDATE ... WHERE status_code = 'REQUESTED'`: aprobar y rechazar a la vez → un 200 y un 409 `INVALID_STATUS_TRANSITION`. `mvnw clean test` 149/149. Contrato `docs/contratos/citas.md` |
| 11 | Catálogo de especialidades (HU-012) | Hecho | `GET /api/v1/catalogs/specialties`, público, solo activas; lo necesita el paso 1 del modal de reserva. Red (404) → Green; `mvnw clean test` 150/150. Prompts de Stitch para ADMIN, PROFESSIONAL y USER entregados al usuario |
| 12 | Diseño de las áreas autenticadas (Stitch) | Hecho | v3 revisada (fondo azulado, contenido inventado, tablas cortadas) → v4 **aprobada** por el Product Owner (D-026) con 5 correcciones de implementación. `citas-web` `08c65c4`: `docs/diseno/APROBACION.md` y `DESIGN.md` § Áreas autenticadas |
| 13 | Contrato para las vistas (backend) | Hecho | `GET /api/v1/auth/session` añade `firstNames`/`lastNames` y nuevo `GET /api/v1/professional/me` (estado, especialidad principal, sedes). Aditivos, prueba primero; `citas-api` `6813dc8`, `mvnw clean test` 151/151 |
| 14 | Pasada de frontend de S3 | Hecho | `citas-web` `fa704a4` (sesión por rol, cliente con refresh, estructura común) y `576db18` (Solicitudes, Especialidades, Profesionales, Mi agenda, Inicio y modal de reserva). 95 pruebas Vitest, lint y build en verde. Verificado contra la API real con datos sintéticos y capturas autenticadas (Edge por CDP) frente a Stitch v4; las 5 correcciones obligatorias aplicadas (acciones visibles a 1280 px, badge real, sin textos inventados, subtítulo de rechazo) |
| 15 | Decisiones abiertas | Hecho | D-027 a D-030 confirmadas; D-028 implementada (`409 ASSIGNMENT_IN_USE`), `citas-api` `1076f6d` |
| 16 | Evidencia de cierre | Hecho | [[evidencia-s3]]: matriz de las 10 HU (43 CA y DoD en `Cumple`), épicas sincronizadas con el estado real de las 25 HU, demo del hook en ambos repos (secreto ficticio bloqueado, prueba en rojo bloqueada, corregida permitida: `citas-web` `6e8d9aa`, `citas-api` `a321231`). `mvnw clean test` 153/153; `npm test` 96/96 |

Pendiente de S3: solo decisiones del Product Owner (ver punto 4).

## Punto de retoma (actualizado 2026-09-25)

### 1. Preparar el equipo

`git pull` en los tres repos, JDK 21 portable en `%USERPROFILE%\.jdks\temurin-21`, `.env` en raíz, `citas-api` y `citas-web`. Docker Desktop está instalado **por usuario** en `%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe`, no en `Program Files`. Luego `docker compose up -d` desde `citas-api/` (MySQL en 3308).

Verificar con `$env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21"; .\mvnw.cmd clean test` → deben pasar **153** pruebas; en `citas-web`, `npm run lint`, `npm test` (96), `npm run typecheck` y `npm run build`.

**Usar `clean test`, no solo `test`.** En este equipo los archivos quedan con horas de modificación incoherentes: el editor los guarda unas 5 horas "en el futuro" y otras herramientas con la hora real. La compilación incremental de Maven puede entonces tomar una fuente por más antigua que su `.class` y ejecutar código viejo. El mismo desfase de reloj provoca a veces que MySQL de Testcontainers presente un certificado TLS "todavía no válido" (`CertificateNotYetValidException`): varias clases de integración fallan al arrancar el contexto. Se resuelve relanzando.

### 2. GitHub

Credencial local `credential.https://github.com.username = jmunoz841` configurada en los tres repos (no global). Hay que autenticarse una vez por clase desde una terminal propia; hasta entonces el repo raíz (privado) no admite `pull` ni `push`.

### 3. Lo que falta de S3

Nada de desarrollo ni de evidencia: ver [[evidencia-s3]].

### 4. Decisiones del Product Owner pendientes

- Pasar a `Completada` las 10 HU de S3 (todas con CA y DoD en `Cumple`).
- Merge `develop` → `main` y etiqueta `s3` en ambos repos.
- Push: autenticarse con la cuenta `jmunoz841` (credencial local ya configurada).


## Relacionadas

[[arquitectura]] · [[decisiones]]
