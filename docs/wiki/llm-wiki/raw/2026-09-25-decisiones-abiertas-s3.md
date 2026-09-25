---
tipo: fuente
fecha: 2026-09-25
origen: Confirmación del Product Owner (Juan Muñoz) — "confirmo las decisiones"
---

# Decisiones sobre las preguntas abiertas de S3

El agente propuso una recomendación para cada pregunta y el Product Owner las confirmó todas.

1. **Endpoint de reserva.** Se mantiene un solo `POST /api/v1/appointments` para HU-013 y HU-014; la especialidad decide si la cita nace `APPROVED` o `REQUESTED`. Sustituye a los dos endpoints del plan inicial.
2. **Quitar una asignación en uso.** Si el ADMIN quita a un profesional una especialidad con citas, o una sede con bloques de disponibilidad o citas, la API responde `409 ASSIGNMENT_IN_USE` con un mensaje que dice qué no se puede quitar, y no cambia nada. Antes respondía `500`.
3. **Desactivar un profesional con citas.** Se conservan sus citas; desactivar solo lo excluye de la búsqueda y de nuevas reservas. Cancelar citas queda para S4 (cancelación e historial).
4. **Solicitudes sin resolver.** Las citas `REQUESTED` no expiran en S3. Si se necesita, se evaluará en S4 o S5 (automatización con n8n).
