---
tipo: fuente
fecha: 2026-09-25
origen: Plan de HU-012, HU-013 y HU-014 aprobado por el Product Owner (Juan Muñoz) antes de implementar
---

# Decisiones de la reserva de citas (S3)

Plan presentado por el agente y aprobado por el Product Owner el 2026-09-25 ("arranca").

1. **Migración V6.** `appointments`, `slot_reservations` y `appointment_status_history` como subconjunto del diseño 3FN propio. `reschedule_requests` queda fuera (reprogramación no está en S3).
2. **Doble reserva.** Se aplica D-013: la clave primaria `slot_id` de `slot_reservations` rechaza la segunda ocupación. La aplicación no la sustituye con bloqueos propios.
3. **Historial sin triggers.** No se crean los triggers de inmutabilidad de `appointment_status_history` que el diseño marca como opcionales (riesgo `log_bin_trust_function_creators`, Q-10). La inmutabilidad la garantiza la aplicación: solo inserta en esa tabla.
4. **Prueba del LOOP.** La prueba de doble reserva es de punta a punta por HTTP, con dos peticiones simultáneas sobre el mismo slot, y no solo contra el repositorio.
5. **Criterios diferidos.** HU-009 CA-02 y HU-010 CA-05 se cierran en este mismo incremento.
