---
tipo: fuente
fecha: 2026-09-23
origen: Indicación del instructor (documento "S3 — Core de agendamiento real sin mocks") + decisiones del Product Owner (Juan Muñoz)
---

# Decisiones de apertura de S3

## Alcance entregado por el instructor

El documento del instructor define el corte de S3: catálogos de solo lectura, afiliación opcional en el registro, oferta administrable (especialidades, profesionales, asignaciones), disponibilidad con slots de 30 minutos, búsqueda y reserva con decisión administrativa de solicitudes especializadas, y frontend sin datos simulados. Aplaza explícitamente el CRUD de EPS/planes, "mis citas" y la bandeja administrativa completa.

**Discrepancia registrada:** el documento usa una numeración de HU ajena a este backlog (cita HU-031 y HU-033; el nuestro termina en HU-025). Se conserva la numeración propia y se adopta el alcance funcional. Equivalencias: su HU-011 = nuestra HU-004; su HU-012/HU-013 = HU-007; su HU-021 = HU-013; su HU-025 = HU-016; su HU-031 = HU-022.

## Decisiones del Product Owner (2026-09-23)

1. **Versionado de API.** Todos los endpoints pasan a `/api/v1/...`, incluida la autenticación ya entregada (`/api/auth/*` → `/api/v1/auth/*`). Se actualizan contrato, `SecurityConfig`, pruebas de HU-001 y el cliente REST de `citas-web`.
2. **HU-002.** No se marca `Completada`. El instructor pidió cerrar "HU-001 y HU-002", pero su HU-002 no corresponde a la nuestra (recuperar contraseña), que sigue sin implementar y permanece en S4. La evidencia de infraestructura de S2 se mantiene en `evidencia-s2.md`.
3. **Cierre de S2.** Merge de `develop` a `main` y etiqueta `s2` en ambos repositorios.
4. **Primer ADMIN.** Se crea como seed sintético en la migración de HU-008, con hash BCrypt de una contraseña temporal documentada en `README.md`. Se descarta el endpoint de bootstrap por ser superficie de ataque fuera del PRD.
5. **HU aprobadas para S3.** HU-004 (con alcance recortado), HU-005, HU-006, HU-008, HU-009, HU-010, HU-012, HU-013, HU-014 y HU-015.
