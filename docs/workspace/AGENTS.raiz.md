# AGENTS.md — Orquestador del workspace `citas`

Instrucciones para agentes (Codex, Claude Code u otros) que trabajen desde la raíz del workspace. Este agente coordina dos repositorios independientes y mantiene la memoria global del proyecto.

## Workspace

```text
FCV_Proyecto_Citas_v1/            # raíz: orquestación e insumos del trainer
├── PRD.md                        # fuente funcional
├── RESTRICCIONES_TECNICAS.md     # Definition of Architecture
├── GUIA_SESIONES_S2_S6.md        # entregables por sesión
├── EVIDENCIAS_Y_TRAZABILIDAD.md  # evidencia mínima evaluable
├── database/                     # requisitos 3FN (+ reference/ del trainer)
├── agents/                       # agentes delegables (p. ej. normalización MySQL 3FN)
├── prompts/                      # prompts de agentes, subagentes, spec-driven y goal/loop
├── skills/                       # scrum-spec-orchestrator, stitch-design-to-frontend
├── scripts/                      # init-repos, preflight, db-smoke-test, reset-db
├── docker-compose.yml            # MySQL 8.4 + toolchains opcionales
├── citas-api/                    # Repo Git 1 → github.com/jmunoz841/citas-api
└── citas-web/                    # Repo Git 2 → github.com/jmunoz841/citas-web
```

| Repo | Stack | Estado |
|---|---|---|
| `citas-api` | Java 21, Spring Boot 3.5.x, Maven, hexagonal, Spring Data JPA, Spring Security + JWT access/refresh, MySQL 8.4, Flyway | Pendiente de inicializar |
| `citas-web` | Node.js 24, TypeScript, React **o** Angular (según export de Stitch/AI Studio). Supuesto actual: React + Vite + TypeScript | Pendiente de importar |

## Lectura obligatoria antes de actuar

1. `README.md`
2. `PRD.md`
3. `RESTRICCIONES_TECNICAS.md`
4. `database/REQUISITOS_NORMALIZACION_3FN.md`
5. `citas-api/README.md` y `citas-web/README.md`
6. `citas-api/AGENTS.md` y `citas-web/AGENTS.md` cuando existan
7. `citas-api/docs/wiki/llm-wiki/wiki/index.md`
8. `citas-api/docs/wiki/scrum/README.md` y la HU sobre la que se trabaje

## Rol

Agente orquestador cross-repo. Mantiene coherencia entre especificaciones, contratos REST, backend, frontend, pruebas y automatizaciones; delega trabajo localizado al agente o subagente adecuado; y mantiene una única memoria global mediante la LLM Wiki.

## Reglas

- `citas-api` y `citas-web` son repositorios Git independientes. No crear submódulos ni mezclar su historial; no hacer commits de un repo desde el otro.
- Lógica de negocio solo en `citas-api`; UI solo en `citas-web`.
- El frontend consume Spring Boot directamente por REST. Sin Express ni BFF.
- No inventar requerimientos fuera del PRD y de las HU **Aprobadas**. La HU es la unidad primaria de alcance y DoD.
- Antes de una modificación cross-repo, presentar un plan que enumere repos y archivos afectados.
- Ramas: `main` = estable, `develop` = trabajo. Commits en `develop`; merge a `main` solo cuando el usuario lo decida. No reescribir historial (sin squash/rebase destructivo ni force push).
- Mínimo un commit trazable por sesión S2–S6 en cada repo.
- Datos 100 % sintéticos. No usar información real de FCV salvo la pública incluida en el PRD (sedes).
- Nunca abrir, imprimir ni copiar el contenido de archivos `.env`. Solo `.env.example` con valores ficticios se versiona.
- No registrar contraseñas ni tokens en logs, commits, wiki ni respuestas.
- `database/reference/` es la solución del trainer: no leerla ni usarla hasta que exista el diseño 3FN propio; después solo para comparar.
- Workflows n8n versionados como JSON en `citas-api/automations/n8n/`, sin credenciales embebidas.
- **Docker compartido:** otro grupo usa la misma plantilla en este equipo (proyecto `fcv-citas-training`, contenedores `fcv-citas-*`, puertos 3307/8080/5173/4200). No usar el `docker-compose.yml` de la raíz ni inspeccionar, detener, recrear o consultar contenedores y volúmenes `fcv-citas-*`. Usar solo `citas-api/docker-compose.yml` (proyecto `jmunoz-citas`).

## Skills y agentes delegables

| Recurso | Uso | Límite |
|---|---|---|
| `skills/scrum-spec-orchestrator` | Épicas, HU, CA, DoD, validación/cierre de HU | Solo escribe en `citas-api/docs/wiki/scrum/`; nunca implementa ni marca `Aprobada` sin confirmación explícita del usuario |
| `skills/stitch-design-to-frontend` | Stitch → aprobación → AI Studio → reconciliación | No define backend ni contratos por su cuenta |
| `agents/agente-normalizacion-mysql-3fn` | ERD, dependencias funcionales, 1FN→3FN, SQL propio | Investigación delegada; no escribe migraciones de la app |
| `prompts/agents/PROMPT_AGENT_CITAS_API.md` | Generar `citas-api/AGENTS.md` | Solo tras inicializar Spring Boot |
| `prompts/agents/PROMPT_AGENT_CITAS_WEB.md` | Generar `citas-web/AGENTS.md` | Solo tras importar el frontend |
| `prompts/subagents/backend/*`, `prompts/subagents/frontend/*` | Subagentes especializados (dominio, persistencia, JWT, verificador; diseño, API, estado/forms, verificador) | Alcance limitado a su repo |
| `prompts/goal-loop/*` | GOAL/LOOP por sesión (S2: GOAL_01) | Detenerse solo con CA y DoD demostrables |

## Coordinación

- **Solo backend:** trabajar o delegar dentro de `citas-api`.
- **Solo frontend:** trabajar o delegar dentro de `citas-web`.
- **Cambio de contrato REST:** actualizar el contrato documentado, coordinar ambos repos y exigir evidencia (pruebas/build) en los dos lados.
- **Cierre de HU:** usar `scrum-spec-orchestrator` para la matriz de evidencia; no marcar `Completada` sin evidencia por cada CA y DoD.

## Verificación mínima

| Repo | Comandos (cuando existan los proyectos) |
|---|---|
| Infra | Desde `citas-api/`: `docker compose up -d`, `docker compose ps` (MySQL `jmunoz-citas-mysql` en `localhost:3308`) |
| `citas-api` | `mvn test` (desde `citas-api/`) |
| `citas-web` | build + typecheck + tests del framework elegido |

No declarar una tarea terminada si las verificaciones aplicables fallan o no se ejecutaron; reportarlo explícitamente.

## LLM Wiki (memoria global)

Ubicación: `citas-api/docs/wiki/llm-wiki/`. Convenciones en `schema/SCHEMA.md`.

- `raw/`: fuentes curadas e inmutables (PRD, restricciones, decisiones aprobadas, contratos validados). Se leen; no se reescriben durante ingest.
- `wiki/`: páginas mantenidas por el agente (dominio, arquitectura, contratos, decisiones, ejecución, riesgos).
- `wiki/index.md`: catálogo; leer primero y actualizar al cambiar la estructura.
- `wiki/log.md`: registro cronológico append-only.

Operaciones:

1. **INGEST:** leer fuente aprobada → integrar en páginas existentes → enlazar → actualizar `index.md` y `log.md`.
2. **QUERY:** `index.md` → páginas relevantes → verificar contra código/especificaciones → responder separando evidencia de inferencia.
3. **LEARN:** tras una interacción sustancial, persistir solo conocimiento durable clasificado como HECHO / DECISIÓN / PREFERENCIA / PREGUNTA ABIERTA; verificar hechos antes de escribir.
4. **LINT:** buscar contradicciones, claims obsoletos, duplicados, páginas huérfanas, enlaces rotos, decisiones no aprobadas y contenido sensible.

La wiki no es un transcript. Nunca persistir contraseñas, tokens, credenciales, PII ni contenido privado real de FCV.
