# Índice — LLM Wiki `citas`

Punto de entrada de la memoria global. Convenciones: [SCHEMA](../schema/SCHEMA.md). Registro de operaciones: [[log]].

## Páginas

| Página | Tipo | Contenido |
|---|---|---|
| [[dominio]] | dominio | Actores, sedes, estados y reglas de negocio esenciales |
| [[arquitectura]] | arquitectura | Repos, stack, capas hexagonales, integración y entorno |
| [[decisiones]] | decisiones | Decisiones aprobadas y preguntas abiertas |
| [[ejecucion]] | ejecucion | Plan S2, estado de avance y evidencia por sesión |
| [[evidencia-s2]] | ejecucion | Registro de evidencia S2 por repositorio (plantilla del curso) |

## Fuentes (`raw/`)

| Fuente | Descripción |
|---|---|
| `raw/PRD.md` | PRD v1.0 del laboratorio |
| `raw/RESTRICCIONES_TECNICAS.md` | Restricciones técnicas y Definition of Architecture |
| `raw/REQUISITOS_NORMALIZACION_3FN.md` | Actividad de normalización 3FN |
| `raw/2026-09-16-decisiones-hu-001.md` | Aprobación de HU-001 y decisiones de sesión JWT |
| `raw/2026-09-16-decisiones-workspace.md` | Raíz sin commits y puerto MySQL 3307 |
| `raw/2026-09-16-decisiones-normalizacion.md` | Zona horaria Bogotá, tipos de documento, primer ADMIN diferido |
| `raw/2026-09-16-decisiones-entorno-aislado.md` | MySQL propio en 3308, aislado de otro grupo |
| `raw/2026-09-18-decisiones-repo-raiz.md` | Raíz del workspace en repo privado |
| `raw/2026-09-18-decisiones-comparacion-referencia.md` | password_hash 255 y estrategia contra doble reserva |
| `raw/2026-09-25-decisiones-reserva.md` | Plan de HU-012/013/014: V6, doble reserva por PK, historial sin triggers, LOOP por HTTP |
| `raw/2026-09-25-aprobacion-diseno-s3.md` | Aprobación del diseño Stitch v4 de las áreas autenticadas de S3 |

## Diseño de base de datos

Modelo 3FN propio: `docs/database/normalizacion-3fn/README.md` (23 tablas, ERD, DF, DDL) y su comparación con la referencia: `comparacion-referencia.md`. Migraciones de la app: `src/main/resources/db/migration/`.

## Contratos REST

- Autenticación (HU-001): `docs/contratos/autenticacion.md`.

## Especificación Scrum

Índice Scrum: `docs/wiki/scrum/README.md` (8 épicas, 25 HU).
