# Decisiones aprobadas — Comparación con el modelo de referencia (S2)

- **Fecha:** 2026-09-18
- **Aprobado por:** Juan Muñoz
- **Contexto:** `docs/database/normalizacion-3fn/comparacion-referencia.md` (diseño propio vs `database/reference/`).

## Decisiones

1. **C-01 → `users.password_hash VARCHAR(255)`** (antes 100), conservando `CHECK (CHAR_LENGTH(password_hash) >= 60)`. Admite BCrypt y Argon2 con prefijo de Spring Security. Único cambio adoptado de la referencia para las tablas de HU-001.
2. **C-05 → Estrategia contra doble reserva:** slots de 30 min materializados (`availability_slots`) y ocupación exclusiva en `slot_reservations` con `PRIMARY KEY (slot_id)`; bloques sin solapamiento mediante `UNIQUE (professional_id, start_at)` en los slots. Se mantiene el diseño propio frente a los triggers de la referencia.

## Diferidas a su HU

- C-02 (régimen determinado por el plan o elegido en la afiliación) y C-03 (una o varias afiliaciones vigentes) → HU-004.
- C-04 (la cita registra la afiliación con que se atendió) → HU-013.
