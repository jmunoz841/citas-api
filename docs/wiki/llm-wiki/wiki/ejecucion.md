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
| 8 | Vertical slice de autenticación | Pendiente | — |
| 9 | Verificación backend (`mvn test`) | Pendiente | — |
| 10 | `citas-api/AGENTS.md` | Pendiente | — |
| 11 | Diseño Stitch login/registro | Pendiente | — |
| 12 | Handoff AI Studio e importación | Pendiente | — |
| 13 | Frontend ejecutable | Pendiente | — |
| 14 | `citas-web/AGENTS.md` | Pendiente | — |
| 15 | GOAL_01 sobre HU-001 | Pendiente | — |
| 16 | Cierre: evidencia, wiki, README, commits `feat(s2): bootstrap specs auth and frontend baseline` | Pendiente | — |

## Punto de retoma (fin de clase 2026-09-16)

1. **Preparar el equipo** (desde `citas-api/`):
   - **Sincronizar primero:** puede haber trabajo hecho en otro equipo. `git -C citas-api checkout develop; git -C citas-api pull` y lo mismo en `citas-web`. En un equipo nuevo: clonar el repo privado `FCV_Proyecto_Citas_v1` y, dentro de él, `citas-api` y `citas-web` (instrucciones en el `README.md` de la raíz, sección "Trabajar desde otro PC"). Hacer `git pull` en los tres repos.
   - Si no existe `%USERPROFILE%\.jdks\temurin-21`, instalar JDK 21 portable (Temurin).
   - Si no existe `.env`, copiar `.env.example` a `.env` y generar contraseñas/secretos nuevos. Si el volumen `jmunoz-citas_mysql_data` ya existe, las contraseñas deben coincidir con las originales o hay que recrearlo (`docker compose down -v`).
   - Estado al cerrar en el PC del laboratorio (2026-09-16): `.env`, contenedor y volumen de `jmunoz-citas` eliminados; JDK 21 portable conservado.
   - Abrir Docker Desktop → `docker compose up -d`.
   - Verificar: `$env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21"; .\mvnw.cmd test`.
2. ~~Paso 5 (comparación con la referencia)~~ y ~~paso 7 (V1 Flyway)~~: hechos el 2026-09-18.
3. **Paso 8:** vertical slice de autenticación de HU-001 (pasar HU-001 a `En desarrollo` al empezar).
4. Continuar con los pasos 9 a 16 de la tabla.

Recordatorios: no usar el compose raíz ni tocar recursos `fcv-citas-*` (otro grupo); API en 8081; HU-001 sigue `Aprobada` (pasar a `En desarrollo` al iniciar el paso 8).

## Relacionadas

[[arquitectura]] · [[decisiones]]
