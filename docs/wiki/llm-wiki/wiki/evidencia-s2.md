---
tipo: ejecucion
actualizado: 2026-09-18
fuentes:
  - EVIDENCIAS_Y_TRAZABILIDAD.md (plantilla de registro)
---

# Evidencia S2

Plantilla de `EVIDENCIAS_Y_TRAZABILIDAD.md`, una por repositorio.

## citas-api

```text
Sesión: S2
Repo: github.com/jmunoz841/citas-api
Branch: develop
Commit hash: ver commit de cierre "feat(s2): bootstrap specs auth and frontend baseline" (historial completo en develop)
HU abordadas: HU-001 Registro e inicio de sesión con sesión JWT — Completada
Criterios completados: CA-01…CA-11 y DoD-01…DoD-07 en "Cumple" (matriz en la HU)
Pruebas ejecutadas: mvnw test 2026-09-18 → 54 pruebas, 0 fallos (dominio 19, AuthServiceTest 9, JwtTokenProviderTest 6, AuthApiIntegrationTest 18 con Testcontainers MySQL 8.4 + Flyway, ArchUnit 2)
Qué quedó pendiente: ejecución del ejercicio GOAL_01 con /goal (la HU ya está implementada: actuará como verificación); merge develop → main cuando el usuario lo decida
Evidencia adicional: Scrum (8 épicas, 25 HU), LLM Wiki, diseño 3FN propio + comparación con la referencia, contrato REST, AGENTS.md, MySQL aislado (docker-compose propio)
```

## citas-web

```text
Sesión: S2
Repo: github.com/jmunoz841/citas-web
Branch: develop
Commit hash: ver commit de cierre "feat(s2): bootstrap specs auth and frontend baseline"
HU abordadas: HU-001 (pantallas de login y registro integradas con citas-api)
Criterios completados: diseño Stitch v2 aprobado; frontend React importado de AI Studio y reconciliado; E2E contra citas-api (CORS 5174, registro, login, sesión, refresh, logout)
Pruebas ejecutadas: npm run typecheck y npm run build en verde; capturas headless escritorio/tablet/móvil; sin pruebas automatizadas de frontend todavía
Qué quedó pendiente: elegir herramienta de pruebas de frontend (pregunta abierta); pantallas restantes del PRD en S3–S4
Evidencia adicional: docs/diseno (v1, v2, DESIGN.md, APROBACION.md), AGENTS.md, panel plegable (D-016)
```

## Relacionadas

[[ejecucion]] · [[decisiones]]
