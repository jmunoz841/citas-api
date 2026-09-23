---
tipo: ejecucion
actualizado: 2026-09-16
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

Pendiente de S3: las **vistas de ADMIN** en `citas-web` (HU-006 T-04, HU-008 T-03, HU-009 T-02), HU-010 (disponibilidad), HU-012/013/014 (búsqueda y reserva, con el LOOP de doble reserva) y HU-015 (decisión administrativa). HU-009 CA-02 (exclusión de la búsqueda) solo será verificable cuando exista HU-012.

## Punto de retoma (fin de clase 2026-09-16)

1. **Preparar el equipo** (desde `citas-api/`):
   - **Sincronizar primero:** puede haber trabajo hecho en otro equipo. `git -C citas-api checkout develop; git -C citas-api pull` y lo mismo en `citas-web`. En un equipo nuevo: clonar el repo privado `FCV_Proyecto_Citas_v1` y, dentro de él, `citas-api` y `citas-web` (instrucciones en el `README.md` de la raíz, sección "Trabajar desde otro PC"). Hacer `git pull` en los tres repos.
   - Si no existe `%USERPROFILE%\.jdks\temurin-21`, instalar JDK 21 portable (Temurin).
   - Si no existe `.env`, crearlo con las variables de `README.md` (en `citas-api`, `citas-web` y la raíz) y secretos nuevos. Si el volumen `jmunoz-citas_mysql_data` ya existe, las contraseñas deben coincidir con las originales o hay que recrearlo (`docker compose down -v`).
   - Estado al cerrar en el PC del laboratorio (2026-09-16): `.env`, contenedor y volumen de `jmunoz-citas` eliminados; JDK 21 portable conservado.
   - Abrir Docker Desktop → `docker compose up -d`.
   - Verificar: `$env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21"; .\mvnw.cmd test`.
2. ~~Paso 5 (comparación con la referencia)~~ y ~~paso 7 (V1 Flyway)~~: hechos el 2026-09-18.
3. ~~Paso 8~~: hecho el 2026-09-18.
4. ~~Paso 9~~: hecho el 2026-09-18 (`mvn test` exige Docker Desktop abierto por Testcontainers).
5. ~~Paso 10~~: hecho el 2026-09-18.
6. ~~Paso 11~~: diseño v2 aprobado el 2026-09-18.
7. ~~Pasos 12 y 13~~: frontend importado y ejecutable (2026-09-18).
8. ~~Paso 14~~: `citas-web/AGENTS.md` creado (2026-09-18).
9. ~~Pasos 15 y 16~~: **S2 cerrada** (2026-09-18). Siguiente: S3 según `GUIA_SESIONES_S2_S6.md` (aprobar las HU del Sprint 2 antes de desarrollar; decidir herramienta de pruebas del frontend; opcional: merge `develop` → `main`).

Recordatorios: no usar el compose raíz ni tocar recursos `fcv-citas-*` (otro grupo); API en 8081; HU-001 sigue `Aprobada` (pasar a `En desarrollo` al iniciar el paso 8).

## Relacionadas

[[arquitectura]] · [[decisiones]]
