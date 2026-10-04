# LOOP-03 — Reto independiente: reconciliar el contrato REST entre `citas-api` y `citas-web`

Diseño propio según `prompts/goal-loop/LOOP_03_RETO_INDEPENDIENTE.md`.

**Estado:** diseñado y **pendiente de revisión y ejecución por el equipo**. Reemplaza la fila
"LOOP-03 propio" de `evidencia-s4`, que solo registraba el resultado sin los 10 elementos exigidos.

## Problema real que lo justifica

Cada HU cambia DTOs en Java (records `*Response` de los controladores) y el frontend los copia a mano
en interfaces TypeScript (`features/*/api/*.ts`). En la reprogramación (HU-018) la vista
`AppointmentViewResponse` ganó `professionalId`, `specialtyId` y `reschedule`, y hubo que replicarlos
en `PatientAppointment`. Ninguna prueba detecta si los dos lados divergen: las pruebas del frontend
usan respuestas simuladas escritas a mano y las del backend no conocen los tipos TypeScript.

## Los 10 elementos

| # | Elemento | Definición |
|---|---|---|
| 1 | **Disparador** | Un commit en `citas-api` que modifica un record `*Response`/`*Request` de `infrastructure/adapters/in/web/**` o un archivo de `docs/contratos/`. |
| 2 | **Meta verificable** | Para cada endpoint consumido por `citas-web`, el conjunto de campos JSON del DTO Java es igual al de la interfaz TypeScript correspondiente (nombre y nulabilidad), y los ejemplos de `docs/contratos/*.md` usan exactamente esos campos. Las suites de ambos repos siguen en verde. |
| 3 | **Estado observado / persistente** | `docs/loops/LOOP-03-reconciliacion-contrato/estado.json`: lista de endpoints con `{ruta, dtoJava, tipoTs, camposSoloJava[], camposSoloTs[], estado}`; se reescribe al final de cada iteración. |
| 4 | **Alcance del Builder** | Solo puede editar tipos TypeScript (`features/*/api/*.ts`), simulaciones de pruebas del frontend y ejemplos de `docs/contratos/`. **No** cambia DTOs Java ni lógica de negocio: si el backend está mal, escala. |
| 5 | **Evidencia del Verifier** | (a) tabla de diferencias regenerada leyendo los records Java y las interfaces TS; (b) `npm run lint`, `npm test`, `npm run typecheck`, `npm run build`; (c) `.\mvnw.cmd test`; (d) diff limitado a los archivos permitidos. El Verifier no edita. |
| 6 | **Presupuesto de iteraciones** | 3. |
| 7 | **Condición de parada** | `camposSoloJava` y `camposSoloTs` vacíos en todos los endpoints, más las cuatro verificaciones del frontend y la del backend en verde → PASS. |
| 8 | **Escalamiento humano** | Un campo existe solo en TypeScript y la UI depende de él (el backend tendría que cambiar); una diferencia de semántica (no solo de nombre); o se agota el presupuesto. |
| 9 | **Log de ejecución** | `iteracion-NN.json` con `{iteration, diffsAntes, diffsDespues, archivosTocados, frontend, backend, verifier, result}`. |
| 10 | **Por qué no basta un prompt** | La comparación abarca 6 contratos y unos 30 DTO; corregir un tipo puede romper simulaciones de pruebas que solo se detectan al ejecutarlas, y esas fallas generan nuevas correcciones. Hace falta un Verifier independiente, que vuelva a medir las diferencias y los tests después de cada cambio, y un tope de iteraciones para no dar vueltas indefinidamente. |

## Prompt sugerido

```text
/goal Opera un ciclo Builder/Verifier para reconciliar el contrato REST entre citas-api y citas-web.
Presupuesto: 3 iteraciones. BUILDER solo edita tipos TS en citas-web/src/features/*/api, mocks de
pruebas del frontend y ejemplos de citas-api/docs/contratos; nunca DTOs Java ni lógica. VERIFIER no
implementa: regenera la tabla de diferencias DTO Java ↔ interfaz TS, ejecuta lint/test/typecheck/build
del frontend y mvnw test del backend, y revisa que el diff solo toque archivos permitidos. PASS solo
si no quedan diferencias y todo está en verde. Si un campo solo existe en TS y la UI depende de él,
o hay diferencia de semántica, escala a humano. Guarda estado.json y un iteracion-NN.json por iteración.
```
