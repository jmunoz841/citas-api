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
