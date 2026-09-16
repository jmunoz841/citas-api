# Decisiones aprobadas — HU-001 (S2)

- **Fecha:** 2026-09-16
- **Aprobado por:** Juan Muñoz (Product Owner / estudiante)
- **Contexto:** revisión de la especificación Scrum generada con `scrum-spec-orchestrator`.

## Decisiones

1. **HU-001 aprobada** para desarrollo en S2: registro de USER + login con JWT access/refresh, refresh y logout/revocación. Es la única HU aprobada en S2; el resto permanece en `Borrador`.
2. **Refresh token:** se rota en cada refresh (se emite uno nuevo y se revoca el usado) y se almacena en base de datos como hash, nunca en texto plano.
3. **Política de contraseña:** mínimo 8 caracteres, con al menos una letra y al menos un número.
