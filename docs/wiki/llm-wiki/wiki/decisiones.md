---
tipo: decisiones
actualizado: 2026-09-16
fuentes:
  - raw/2026-09-16-decisiones-hu-001.md
  - raw/RESTRICCIONES_TECNICAS.md
---

# Decisiones

Solo se registran como DECISIÓN los puntos aprobados explícitamente por el usuario. Las propuestas del agente van en *Preguntas abiertas*.

## Decisiones aprobadas

| ID | Fecha | Decisión | Alcance | Fuente |
|---|---|---|---|---|
| D-001 | 2026-09-16 | Dos repos independientes (`citas-api`, `citas-web`) con `main`/`develop`, publicados en GitHub (jmunoz841) | Workspace | `raw/RESTRICCIONES_TECNICAS.md` |
| D-002 | 2026-09-16 | HU-001 (registro + login JWT) es la única HU aprobada para S2 | Scrum | `raw/2026-09-16-decisiones-hu-001.md` |
| D-003 | 2026-09-16 | Refresh token rotado en cada refresh y almacenado como hash en BD | HU-001 | `raw/2026-09-16-decisiones-hu-001.md` |
| D-004 | 2026-09-16 | Contraseña: mínimo 8 caracteres, al menos una letra y un número | HU-001 | `raw/2026-09-16-decisiones-hu-001.md` |
| D-005 | 2026-09-16 | La raíz del workspace es el clon de la plantilla del trainer: no se hacen commits en ella; `citas-api/` y `citas-web/` se ocultan localmente (`.git/info/exclude` + `skip-worktree`) | Workspace | `raw/2026-09-16-decisiones-workspace.md` |
| D-006 | 2026-09-16 | ~~MySQL en `localhost:3307`~~ → reemplazada por D-009 | Entorno | `raw/2026-09-16-decisiones-workspace.md` |
| D-009 | 2026-09-16 | MySQL propio y aislado: `citas-api/docker-compose.yml` (proyecto `jmunoz-citas`, contenedor `jmunoz-citas-mysql`, puerto host 3308). No usar el compose raíz (colisiona con otro grupo en el mismo equipo); `.env` raíz eliminado | Entorno | `raw/2026-09-16-decisiones-entorno-aislado.md` |
| D-010 | 2026-09-16 | `citas-api` escucha en el puerto 8081 (`API_PORT`), porque 8080 lo ocupa el otro grupo | Entorno | `raw/2026-09-16-decisiones-entorno-aislado.md` |
| D-007 | 2026-09-16 | Fechas/horas almacenadas en `America/Bogota` (JDBC, sesión MySQL, Hibernate y Jackson) | Global | `raw/2026-09-16-decisiones-normalizacion.md` |
| D-008 | 2026-09-16 | Tipos de documento: CC, CE, TI, RC, PA, PPT | HU-001 | `raw/2026-09-16-decisiones-normalizacion.md` |

## Preguntas abiertas

- **Primer ADMIN.** Diferida a HU-008 (sin usuarios en la migración de HU-001).
- **Preguntas del modelo 3FN** Q-01, Q-02, Q-03, Q-05, Q-06, Q-07, Q-10 a Q-13: ver `docs/database/normalizacion-3fn/README.md`; se resuelven al aprobar sus HU.
- **Q-003 — Representación de slots y estrategia contra doble reserva.** Propuesta en `docs/database/normalizacion-3fn/` (slots materializados + `slot_reservations` con PK por slot); pendiente de comparación con la referencia.
- **Q-004 — Framework frontend.** Supuesto React + Vite + TypeScript hasta el export de Stitch/AI Studio.

## Relacionadas

[[arquitectura]] · [[ejecucion]]
