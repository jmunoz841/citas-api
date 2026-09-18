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
| D-005 | 2026-09-16 | ~~Reemplazada por D-011~~ La raíz del workspace es el clon de la plantilla del trainer: no se hacen commits en ella; `citas-api/` y `citas-web/` se ocultan localmente (`.git/info/exclude` + `skip-worktree`) | Workspace | `raw/2026-09-16-decisiones-workspace.md` |
| D-006 | 2026-09-16 | ~~MySQL en `localhost:3307`~~ → reemplazada por D-009 | Entorno | `raw/2026-09-16-decisiones-workspace.md` |
| D-009 | 2026-09-16 | MySQL propio y aislado: `citas-api/docker-compose.yml` (proyecto `jmunoz-citas`, contenedor `jmunoz-citas-mysql`, puerto host 3308). No usar el compose raíz (colisiona con otro grupo en el mismo equipo); `.env` raíz eliminado | Entorno | `raw/2026-09-16-decisiones-entorno-aislado.md` |
| D-010 | 2026-09-16 | `citas-api` escucha en el puerto 8081 (`API_PORT`), porque 8080 lo ocupa el otro grupo | Entorno | `raw/2026-09-16-decisiones-entorno-aislado.md` |
| D-011 | 2026-09-18 | La raíz del workspace se versiona en un repo **privado** `jmunoz841/FCV_Proyecto_Citas_v1` (reemplaza D-005). Incluye `database/reference/db.sql`. `citas-api/` y `citas-web/` quedan en su `.gitignore` y se clonan dentro. El remoto del trainer queda como `upstream` | Workspace | `raw/2026-09-18-decisiones-repo-raiz.md` |
| D-012 | 2026-09-18 | `users.password_hash VARCHAR(255)` (BCrypt o Argon2) con CHECK ≥ 60 | HU-001 / V1 | `raw/2026-09-18-decisiones-comparacion-referencia.md` |
| D-013 | 2026-09-18 | Doble reserva: slots materializados + `slot_reservations` con PK por slot; bloques sin solapamiento por UNIQUE `(professional_id, start_at)` | Modelo de datos | `raw/2026-09-18-decisiones-comparacion-referencia.md` |
| D-014 | 2026-09-18 | Diseño de login/registro "CitaClara" v2 aprobado; fuente de verdad `citas-web/docs/diseno/DESIGN.md`. Consentimiento Ley 1581 fuera de S2 | Frontend | `citas-web/docs/diseno/APROBACION.md` |
| D-015 | 2026-09-18 | Frontend React 19 + Vite + TypeScript estricto + Tailwind 4 en el puerto **5174** (5173 ocupado por otro grupo); `FRONTEND_ORIGIN` de `citas-api` pasa a `http://localhost:5174`. Tokens: access en memoria, refresh en sessionStorage | Frontend / CORS | Recomendación del plan S2 + importación del paso 13 |
| D-016 | 2026-09-18 | Panel de sedes plegable en escritorio (riel de 72px); implementado directo en código sin iteración en Stitch, por decisión del usuario; especificado en `citas-web/docs/diseno/DESIGN.md` | Frontend | `citas-web/docs/diseno/APROBACION.md` |
| D-007 | 2026-09-16 | Fechas/horas almacenadas en `America/Bogota` (JDBC, sesión MySQL, Hibernate y Jackson) | Global | `raw/2026-09-16-decisiones-normalizacion.md` |
| D-008 | 2026-09-16 | Tipos de documento: CC, CE, TI, RC, PA, PPT | HU-001 | `raw/2026-09-16-decisiones-normalizacion.md` |

## Preguntas abiertas

- **Primer ADMIN.** Diferida a HU-008 (sin usuarios en la migración de HU-001).
- **Preguntas del modelo 3FN** Q-01, Q-02, Q-03, Q-05, Q-06, Q-07, Q-10 a Q-13: ver `docs/database/normalizacion-3fn/README.md`; se resuelven al aprobar sus HU.
- ~~Q-003 — Representación de slots y doble reserva~~ → resuelta por D-013.
- **Comparación con la referencia** C-02, C-03 (→ HU-004) y C-04 (→ HU-013).
- **Riesgo:** Flyway (versión gestionada por Spring Boot 3.5.16) avisa que su soporte probado de MySQL llega a 8.1; la V1 se aplicó sin errores en 8.4.11. Revisar si aparece algún fallo en migraciones futuras.
- **Q-004 — Framework frontend.** Supuesto React + Vite + TypeScript hasta el export de Stitch/AI Studio.

## Relacionadas

[[arquitectura]] · [[ejecucion]]
