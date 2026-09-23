# Decisiones aprobadas — Normalización 3FN (S2)

- **Fecha:** 2026-09-16
- **Aprobado por:** Juan Muñoz
- **Contexto:** revisión de las preguntas abiertas de `docs/database/normalizacion-3fn/README.md` que afectan a HU-001.

## Decisiones

1. **Q-08 → Zona horaria:** todas las fechas/horas se almacenan en hora civil `America/Bogota`. La conexión JDBC y la sesión MySQL fijan esa zona.
2. **Q-04 → Tipos de documento:** catálogo fijo con CC, CE, TI, RC, PA, PPT.
3. **Q-09 → Primer ADMIN:** decisión diferida a la sesión en que se trabaje la gestión de profesionales (HU-008). La migración de HU-001 no crea usuarios.
