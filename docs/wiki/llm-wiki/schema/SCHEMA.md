# SCHEMA — Convenciones de la LLM Wiki

Wiki global del workspace `citas`, mantenida por el agente orquestador (ver `AGENTS.md` raíz). Adaptación del patrón LLM Wiki: fuentes inmutables (`raw/`), síntesis mantenida (`wiki/`) y convenciones (`schema/`).

## Capas

| Capa | Contenido | Quién escribe | Regla |
|---|---|---|---|
| `raw/` | Fuentes curadas: PRD, restricciones, decisiones aprobadas, contratos validados | Usuario o agente con aprobación | Inmutable: una fuente nueva o corregida es un archivo nuevo con fecha |
| `wiki/` | Páginas de síntesis | Agente | Se reescribe al integrar conocimiento; siempre enlaza sus fuentes |
| `schema/` | Este archivo | Agente con aprobación | Cambios registrados en `wiki/log.md` |

## Nombres

- Archivos en kebab-case, sin tildes: `arquitectura.md`, `contrato-autenticacion.md`.
- Fuentes en `raw/` con prefijo de fecha cuando no son documentos base: `2026-09-16-decisiones-hu-001.md`.
- Enlaces internos con wikilinks de Obsidian: `[[arquitectura]]`. Para Scrum: `[[HU-001-registro-e-inicio-de-sesion-jwt]]`.

## Estructura de una página `wiki/`

```markdown
---
tipo: dominio | arquitectura | contrato | decisiones | ejecucion | riesgos | sintesis
actualizado: AAAA-MM-DD
fuentes:
  - raw/...
---

# Título

Contenido sintetizado. Cada afirmación relevante es trazable a una fuente o a evidencia del repositorio.

## Preguntas abiertas
```

## Clasificación de conocimiento (LEARN)

- **HECHO:** verificable en código, especificación o ejecución. Citar evidencia.
- **DECISIÓN:** aprobada explícitamente por el usuario. Registrar fecha y alcance en [[decisiones]].
- **PREFERENCIA:** forma de trabajar indicada por el usuario.
- **PREGUNTA ABIERTA:** no se resuelve por inferencia; se lista hasta que el usuario decida.

Una propuesta del agente no es decisión hasta que el usuario la apruebe.

## Operaciones

1. **INGEST:** leer fuente → integrar en páginas existentes (crear página solo si no encaja) → enlazar → actualizar `index.md` → añadir entrada a `log.md`.
2. **QUERY:** `index.md` → páginas → verificar contra repositorio → responder separando evidencia e inferencia.
3. **LEARN:** persistir solo conocimiento durable, clasificado.
4. **LINT:** contradicciones, claims obsoletos, duplicados, huérfanas, enlaces rotos, decisiones no aprobadas, contenido sensible.

## Formato de `log.md`

Append-only, una entrada por operación:

```markdown
## AAAA-MM-DD — OPERACION — resumen breve
- Fuentes: ...
- Páginas afectadas: ...
```

## Prohibido persistir

Contraseñas, tokens, secretos, contenido de `.env`, PII, conversaciones completas, datos reales de FCV.
