# Evidencia S4 — ciclo de vida de citas

Estado: evidencia técnica preparada; las HU conservan su estado actual hasta confirmación explícita del Product Owner.

| HU | Evidencia verificable |
|---|---|
| HU-016 | `MyAppointmentsApiIntegrationTest`: listado, filtros, detalle y aislamiento del paciente; UI `MyAppointmentsPage`. |
| HU-017 | La misma integración verifica cancelación futura, liberación de slots, historial y pertenencia. |
| HU-011 | `MyAppointmentsApiIntegrationTest#hu011_elProfesionalVeSoloSusCitasAprobadasEnElRangoYSedeSolicitados`: agenda propia, rango, sede y validación. |
| HU-020 | `MyAppointmentsApiIntegrationTest#hu020_hu021_elProfesionalCierraSuCitaIniciadaYElHistorialEsDeSoloLectura` y `#hu020_noPermiteCerrarUnaCitaFuturaONoPropia`: cierre desde inicio, actor profesional y aislamiento. |
| HU-021 | Los endpoints USER, ADMIN y PROFESSIONAL devuelven historial ordenado; la prueba verifica propiedad, completitud y ausencia de `PATCH` (`405`). |
| HU-002 | `PasswordRecoveryApiIntegrationTest`: respuesta indistinguible para solicitud, cambio con token vigente, rechazo de token reutilizado/expirado y conservación de la cuenta; UI con rutas de solicitud y restablecimiento. |

## Ciclos Builder / Verifier

| Iteración | Builder | Verifier | Resultado |
|---|---|---|---|
| LOOP-01 guiado | Se reutilizó la reserva concurrente ya protegida por `pk_slot_reservations` (S3). | Prueba de integración de reservas existente. | PASS (evidencia S3). |
| LOOP-02 equivalente | Se implementó transición profesional `APPROVED → COMPLETED/NO_SHOW`, con migración V7 y fuente `PROFESSIONAL`. | Integración focalizada: 14 pruebas, 0 fallos. | PASS. |
| LOOP-03 propio | Se incorporó consulta inmutable de historial para los tres actores, con autorización por propiedad. | Integración focalizada: actor, orden, 404 ajeno y `PATCH` 405. | PASS. |

## Verificaciones de esta sesión

- Backend: `mvnw.cmd -Dtest=MyAppointmentsApiIntegrationTest test -q` con Java 21 y Testcontainers: 14 pruebas, 0 fallos.
- Backend completo: `mvnw.cmd clean test` con Java 21 y Testcontainers: 167 pruebas, 0 fallos, 0 errores y 0 omitidas.
- HU-002: backend completo posterior a V8: 171 pruebas, 0 fallos, 0 errores y 0 omitidas; frontend: lint, 99 pruebas, typecheck y build correctos.
- Frontend: `npm run lint`, `npm test -- --run`, `npm run typecheck`, `npm run build`: lint/typecheck/build correctos. La prueba de agenda verifica la consulta semanal; el flujo de cierre queda cubierto en la integración del backend.

Pendiente antes del cierre formal: completar la matriz Scrum/DoD sin cambiar el estado de las HU.
