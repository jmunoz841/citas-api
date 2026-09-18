# Log — LLM Wiki `citas`

Registro append-only. Formato definido en `schema/SCHEMA.md`.

## 2026-09-16 — SPEC — Especificación Scrum inicial
- Fuentes: `raw/PRD.md`, `raw/RESTRICCIONES_TECNICAS.md`, `raw/REQUISITOS_NORMALIZACION_3FN.md`
- Resultado: 8 épicas y 25 HU en `docs/wiki/scrum/` generadas con `scrum-spec-orchestrator`.

## 2026-09-16 — INGEST — Documentos base y decisiones de HU-001
- Fuentes: `raw/PRD.md`, `raw/RESTRICCIONES_TECNICAS.md`, `raw/REQUISITOS_NORMALIZACION_3FN.md`, `raw/2026-09-16-decisiones-hu-001.md`
- Páginas afectadas: [[index]], [[dominio]], [[arquitectura]], [[decisiones]], [[ejecucion]] (creadas)

## 2026-09-16 — INGEST — Decisiones de workspace y puerto MySQL
- Fuentes: `raw/2026-09-16-decisiones-workspace.md`
- Páginas afectadas: [[decisiones]] (D-005, D-006; cerradas Q-001 y Q-002), [[arquitectura]], [[index]]

## 2026-09-16 — LEARN — Inicialización del backend (paso 6)
- HECHO: `mvnw test` con JDK 21 → 2 tests ArchUnit, BUILD SUCCESS.
- HECHO: Spring Initializr no ofrece Boot 3.5.x; `pom.xml` manual con 3.5.16.
- Páginas afectadas: [[arquitectura]], [[ejecucion]]

## 2026-09-16 — INGEST — Diseño 3FN propio y decisiones de normalización
- Fuentes: `docs/database/normalizacion-3fn/` (agente de normalización, sin acceso a la referencia), `raw/2026-09-16-decisiones-normalizacion.md`
- Páginas afectadas: [[decisiones]] (D-007, D-008), [[index]]; `application.yml` pasa a America/Bogota

## 2026-09-16 — INGEST — Entorno Docker aislado del otro grupo
- Fuentes: `raw/2026-09-16-decisiones-entorno-aislado.md`
- HECHO: `jmunoz-citas-mysql` healthy; `SELECT VERSION(), @@time_zone` → 8.4.11, America/Bogota, utf8mb4, BD `citas_fcv_training`.
- Páginas afectadas: [[decisiones]] (D-009 reemplaza D-006), [[arquitectura]], [[index]]

## 2026-09-18 — INGEST — Repo privado de la raíz del workspace
- Fuentes: `raw/2026-09-18-decisiones-repo-raiz.md`
- Páginas afectadas: [[decisiones]] (D-011 reemplaza D-005), [[index]]

## 2026-09-18 — INGEST — Comparación con la referencia y migración V1
- Fuentes: `docs/database/normalizacion-3fn/comparacion-referencia.md`, `raw/2026-09-18-decisiones-comparacion-referencia.md`
- HECHO: `schema.sql` propio cargado sin errores en MySQL 8.4.11 (23 tablas, 33 FK, 31 CHECK, 2 triggers) en base desechable; 9 pruebas de restricciones de HU-001 con el resultado esperado.
- HECHO: `V1__identidad_hu001.sql` aplicada con Flyway; aviso de Flyway sobre soporte probado hasta MySQL 8.1.
- Páginas afectadas: [[decisiones]] (D-012, D-013; Q-003 cerrada), [[ejecucion]], [[index]]

## 2026-09-18 — LEARN — Vertical slice de autenticación (paso 8)
- HECHO: API arranca con Flyway `validate` sobre V1; humo manual 20/20 (CA-01…CA-11) contra `jmunoz-citas-mysql`; contraseña guardada como BCrypt `$2a$10$`; refresh tokens como SHA-256 hex; rotación enlazada por `replaced_by_token_id`; 0 contraseñas/tokens en el log.
- Páginas afectadas: [[arquitectura]], [[ejecucion]], [[index]]

## 2026-09-18 — LEARN — Pruebas automatizadas de HU-001 (paso 9)
- HECHO: `mvn test` → 54 pruebas, 0 fallos, BUILD SUCCESS; integración contra `mysql:8.4` de Testcontainers (el contenedor se elimina al terminar; la BD local no cambia).
- DECISIÓN (usuario, 2026-09-18): pruebas de integración con Testcontainers en lugar de la MySQL local.
- Páginas afectadas: [[arquitectura]], [[ejecucion]]

## 2026-09-18 — INGEST — Panel de sedes plegable
- DECISIÓN (usuario): riel delgado de 72px, cambio directo en código (D-016).
- HECHO: typecheck/build en verde; capturas desplegado y plegado correctas; la preferencia persiste entre pantallas (`localStorage`).
- Páginas afectadas: [[decisiones]]

## 2026-09-18 — LEARN — AGENTS del frontend (paso 14)
- HECHO: `citas-web/AGENTS.md` y `CLAUDE.md` creados; `AGENTS.md.template` retirado. PREGUNTA ABIERTA: herramienta de pruebas automatizadas del frontend (hoy solo typecheck/build).
- Páginas afectadas: [[ejecucion]]

## 2026-09-18 — LEARN — Importación del frontend (pasos 12 y 13)
- HECHO: export de AI Studio con buena estructura y accesibilidad; se retiraron dependencias no pedidas y se corrigieron tipos numéricos, rol `USER` en el mock, sesión cerrada por errores de red, resumen de errores que etiquetaba mal los 400 y no se limpiaba, estilos de error y tamaños fuera de `DESIGN.md`.
- HECHO: E2E navegador→API con origen 5174; `mvn test` 54/54 tras cambiar CORS.
- HECHO: Edge headless tiene ancho mínimo ~500px; para móvil se captura dentro de un iframe de 390px.
- Páginas afectadas: [[decisiones]] (D-015), [[ejecucion]], [[arquitectura]]

## 2026-09-18 — INGEST — Diseño aprobado de login y registro (paso 11)
- Fuentes: `citas-web/docs/diseno/APROBACION.md`, `DESIGN.md`, `stitch-v1/`, `stitch-v2/`
- DECISIÓN (usuario): v2 aprobada; Ley 1581 fuera de S2. PREGUNTA ABIERTA: consentimiento de datos como HU futura.
- Páginas afectadas: [[decisiones]] (D-014), [[ejecucion]]

## 2026-09-18 — LEARN — AGENTS del backend (paso 10)
- HECHO: `citas-api/AGENTS.md` generado desde el estado real del repo; `citas-api/CLAUDE.md` lo importa; `AGENTS.md.template` retirado.
- Páginas afectadas: [[ejecucion]]
