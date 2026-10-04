# LOOP-02 — Reprogramación completa (Builder / Verifier)

Ejecución del prompt guiado `prompts/goal-loop/LOOP_02_GUIADO_AVANZADO.md` sobre HU-018 y HU-019,
con la bandeja HU-022 como extensión del mismo incremento.

| Elemento | Valor |
|---|---|
| Meta verificable | HU-018 CA-01..04 y HU-019 CA-01..04 con evidencia; pruebas backend en verde; lint, pruebas, typecheck y build del frontend en verde; contrato REST actualizado |
| Reglas innegociables | La cita original sigue vigente mientras la reprogramación está `PENDING`; la nueva franja queda retenida; `APPROVED` libera los slots anteriores y confirma los nuevos; `REJECTED` libera la retención y conserva la cita; solo ADMIN decide; el frontend refleja estado y motivo |
| Presupuesto | Máximo 4 iteraciones |
| Builder | Puede modificar `citas-api` y `citas-web` dentro de HU-018, HU-019 y HU-022 |
| Verifier | No implementa. Valida criterios, diff, pruebas backend, verificaciones frontend y contrato REST |
| Condición de parada | PASS solo si todos los criterios obligatorios tienen evidencia ejecutada |
| Escalamiento humano | Cambio de dependencia o migración no prevista; herramienta de verificación no disponible |
| Estado persistente | Un JSON por iteración en esta carpeta (`iteracion-NN.json`) |

## Iteraciones

| # | Builder | Backend | Frontend | Verifier | Resultado |
|---|---|---|---|---|---|
| 1 | V9 + dominio, servicios, adaptadores, endpoints, contrato; vistas de paciente y bandeja | Compila; 37 pruebas unitarias y ArchUnit en verde; **integración no ejecutada** (sin Docker en el equipo) | lint, 112 pruebas, typecheck y build en verde | BLOCKED: falta la evidencia de integración contra MySQL | Escalado al humano: instalar Docker Desktop y reanudar en la iteración 2 |
| 2 | Tras PASS del Verifier, corrige sus 3 hallazgos menores: orden de bloqueo, 409 ante interbloqueo, aviso `CANCELLED` en la UI, regla de no solapamiento en el contrato | 197/197 antes y después de las correcciones; V9 aplicada en Testcontainers y en la base local; prueba de humo HTTP en verde | lint, 113 pruebas, typecheck y build en verde | **PASS** (subagente aislado) | **COMPLETED** |

Detalle: [iteracion-01.json](iteracion-01.json), [iteracion-02.json](iteracion-02.json).

**Condición de parada alcanzada en la iteración 2 de 4:** todos los CA de HU-018, HU-019 y HU-022 tienen evidencia ejecutada.

## Migración

`V9__reprogramacion_hu018_hu019.sql` estaba prevista en el diseño 3FN propio (`reschedule_requests`,
titular exclusivo en `slot_reservations`), así que no requiere escalar por "migración no prevista".
Sí se escaló el bloqueo de herramienta en la iteración 1: sin Docker, Testcontainers no podía
levantar MySQL 8.4. En la iteración 2, V9 se aplicó sin errores en Testcontainers y en la base local.

## Cierre

Las matrices de HU-018, HU-019 y HU-022 están en `Cumple`. El Product Owner confirmó el cierre el
2026-10-04: las tres HU están `Completada` y las propuestas P-036 a P-039 quedaron aprobadas como
D-036 a D-039.
