# Decisiones de diseño

Responde las 9 decisiones de `database/REQUISITOS_NORMALIZACION_3FN.md`. Cada punto separa **HECHO** (requisito explícito del PRD, una HU o una decisión aprobada), **INFERENCIA** y **SUPUESTO** (numerados en el [README](README.md#supuestos)).

---

## 1. Claves primarias y únicas

### Criterio general
- **Catálogos fijos** (`roles`, `document_types`, `regimes`, `sites`, `appointment_statuses`, `reschedule_statuses`): **PK natural `code`** (VARCHAR). HECHO: son sembrados y de solo lectura (RF-05), así que el código es inmutable. Ventajas:
  1. Permite `CHECK` sobre códigos en tablas hijas (`status_code <> 'REJECTED' OR reason IS NOT NULL`). Con un id numérico, el CHECK dependería de ids sembrados, que son frágiles.
  2. Se lee directamente en los roles de Spring Security y en los estados del dominio.
- **Catálogos configurables y entidades transaccionales**: **PK sustituta `BIGINT UNSIGNED AUTO_INCREMENT`**. Los nombres son editables por ADMIN, así que no pueden ser PK.
- **Tablas puente**: PK compuesta con las dos FK.
- **Subtipo** `professionals`: PK = FK `user_id` (relación 1:0..1 con `users`).

### Claves únicas (reglas de negocio)

| Restricción | Regla | Origen |
|---|---|---|
| `users.uk_users_email` sobre columna con collation `utf8mb4_0900_as_ci` | Email único sin distinguir mayúsculas; sí distingue acentos | HECHO (HU-001) + SUPUESTO S-04 (la app además normaliza a minúsculas) |
| `users.uk_users_document (document_type_code, document_number)` | Documento único por tipo + número | HECHO |
| `refresh_tokens.uk_refresh_tokens_hash`, `password_reset_tokens.uk_…_hash` | Búsqueda por hash; colisión = error | HECHO (D-003) + S-07 |
| `refresh_tokens.uk_refresh_tokens_replaced_by` | Cadena de rotación lineal | INFERENCIA |
| `eps.uk_eps_name`, `eps_plans.uk_eps_plans_eps_name` | Nombre de EPS único; plan único dentro de su EPS | HECHO (HU-007) |
| `user_affiliations.uk_user_affiliations_combo` | Sin afiliaciones duplicadas | HECHO (HU-004) |
| `specialties.uk_specialties_name` | Nombre único | HECHO (HU-006) |
| `specialties.uk_specialties_single_general` (índice funcional) | Como máximo una especialidad general | SUPUESTO S-09 |
| `professionals.uk_professionals_code`, `uk_professionals_license` | Código y matrícula únicos | HECHO (HU-008) |
| `professional_specialties.uk_prof_specialties_one_primary` (índice funcional) | Como máximo una primaria por profesional | HECHO (HU-008); el mínimo de una lo valida la app |
| `availability_blocks.uk_blocks_professional_start`, `availability_slots.uk_slots_professional_start` | Sin bloques solapados del mismo profesional | HECHO (RF-08, HU-010) |
| `slot_reservations.pk_slot_reservations (slot_id)` | Sin doble reserva | HECHO (RN-01) |
| `reschedule_requests.uk_rr_one_pending_per_appointment` (índice funcional) | Como máximo una reprogramación `PENDING` por cita | SUPUESTO S-14 (HU-018) |
| `uk_*_id_professional`, `uk_rr_id_appointment` | Superclaves técnicas, destino de FK compuestas | Requisito de MySQL 8.4 (`restrict_fk_on_non_standard_key`) |

Las "únicas parciales" usan el patrón de **índice funcional con `CASE … ELSE NULL`**: InnoDB permite múltiples NULL en un índice UNIQUE, así que la unicidad solo aplica a las filas que cumplen la condición.

## 2. Cardinalidades

Detalladas en [erd.md](erd.md#cardinalidades-explicadas). Resumen:

- N:M: usuario–rol, profesional–especialidad, profesional–sede (HECHO).
- 1:0..1: usuario–profesional (HECHO: el profesional es un usuario especializado).
- 1:N: EPS–plan (HECHO), usuario–afiliación (SUPUESTO S-08; pregunta Q-02), bloque–slots (HECHO), cita–historial (HECHO), cita–reprogramaciones (HECHO, con máximo 1 pendiente por SUPUESTO).
- 1:0..1: slot–reserva (HECHO RN-01).
- Las cotas mínimas (≥1 rol, ≥1 especialidad, ≥1 sede, 1 o 2 slots por cita) **no** se pueden expresar con FK/UNIQUE en MySQL; son invariantes de dominio validadas en la transacción de la aplicación y cubiertas por pruebas.

## 3. Catálogos fijos vs configurables

| Catálogo | Tipo | Tabla | PK | Borrado |
|---|---|---|---|---|
| Roles | Fijo (HECHO RF-05) | `roles` | `code` | No hay API de escritura; FK RESTRICT |
| Estados de cita | Fijo (HECHO) | `appointment_statuses` | `code` | Ídem |
| Estados de reprogramación | Fijo (HECHO) | `reschedule_statuses` | `code` | Ídem |
| Regímenes | Fijo (HECHO) | `regimes` | `code` | Ídem |
| Sedes | Fijo (HECHO) | `sites` | `code` | Ídem |
| Tipos de documento | Fijo (SUPUESTO S-01; HU-001 lo deja abierto) | `document_types` | `code` | Ídem |
| Fuente de cambio (SYSTEM/USER/ADMIN) | Fijo, **sin tabla** | `CHECK` en `appointment_status_history.source` | — | No hay atributos dependientes → una tabla no aporta nada (INFERENCIA) |
| EPS | Configurable (HECHO RF-06) | `eps` | `id` | `is_active`; FK RESTRICT impide borrar si está referenciada |
| Planes | Configurable (HECHO) | `eps_plans` | `id` | Ídem |
| Especialidades | Configurable (HECHO), duración 30/60 | `specialties` | `id` | Ídem |

HECHO (RF-06): "No se permite borrar físicamente un catálogo referenciado". En la BD lo garantizan `ON DELETE RESTRICT` y las FK sin acción referencial (NO ACTION). La desactivación usa `is_active`. **Evitar seleccionar un elemento inactivo** en nuevas afiliaciones o citas es validación de la app (un CHECK no puede leer otra tabla).

Las asignaciones `professional_specialties` y `professional_sites` también llevan `is_active` (SUPUESTO S-10). Si tienen citas o bloques, la FK RESTRICT impide borrarlas, así que la única forma de "quitar" la asignación es desactivarla.

## 4. Cómo se evita la doble reserva (restricción de BD + concurrencia)

### Estructura
1. **Slots materializados** (`availability_slots`): al crear un bloque, la app inserta una fila por cada slot de 30 min (HU-010 CA-01: 08:00–12:00 → 8 filas).
2. **Ocupación como fila** (`slot_reservations`) con **`PRIMARY KEY (slot_id)`**. Un slot está ocupado (reservado o retenido) si y solo si existe su fila. **Liberar = `DELETE`**.
3. `CHECK ((appointment_id IS NULL) XOR (reschedule_request_id IS NULL))`: cada ocupación tiene exactamente un titular, sea la cita (franja vigente) o una reprogramación pendiente (franja provisional).
4. FK compuesta `(slot_id, professional_id) → availability_slots(id, professional_id)` y `(appointment_id, professional_id) → appointments(id, professional_id)`: la BD impide ocupar el slot de otro profesional con una cita.

### Por qué es seguro ante concurrencia (HU-013 CA-03)
- Dos transacciones que insertan el mismo `slot_id`: InnoDB toma un bloqueo sobre la entrada del índice único. La segunda **espera**; si la primera confirma, la segunda recibe **`ER_DUP_ENTRY` (1062)** y la app responde "horario ya no disponible" (409). Si la primera hace rollback, la segunda continúa. Esto funciona en cualquier nivel de aislamiento, porque no depende de un `SELECT` previo.
- Una cita de 60 min inserta sus 2 filas en la **misma transacción**. Si una falla, se revierte todo: no quedan reservas parciales.
- **Orden determinista**: insertar siempre en orden ascendente de `slot_id`, para minimizar los interbloqueos entre reservas de 60 min que se cruzan. Ante un deadlock (1213), la app reintenta de forma acotada o responde 409.
- **Borrado de bloque concurrente**: la inserción de la reserva valida la FK con un bloqueo compartido sobre el slot padre; el `DELETE` del bloque (que borra en cascada sus slots) queda bloqueado por `ON DELETE RESTRICT` desde `slot_reservations`. Así, **HU-010 CA-05 también queda garantizado en la BD**: no se puede eliminar un bloque con slots ocupados.
- La verificación previa (`SELECT … LEFT JOIN slot_reservations`) sirve solo para la experiencia de usuario. La garantía es la PK.

### Solapamiento de bloques
`UNIQUE (professional_id, start_at)` en `availability_slots`: como todos los límites están alineados a :00/:30 (CHECK), dos bloques del mismo profesional que se solapen comparten al menos un inicio de slot, **incluso en sedes distintas** (HU-010 CA-03). La inserción del segundo bloque falla en la BD, también con concurrencia.

### Lo que sigue en la aplicación
- No reservar en el pasado (RN-06): un CHECK no puede usar `NOW()`.
- Profesional o especialidad activos (RN-08), slot dentro de un bloque de la sede elegida y cita con los slots correctos (ver §5).

## 5. Citas de 60 minutos

- HECHO: 60 min = 2 slots consecutivos (RF-09, RN-05). La duración la define la especialidad, no el profesional.
- `appointments.duration_minutes` (CHECK 30|60) se copia de `specialties.duration_minutes` al reservar (snapshot).
- `appointments.end_at` es columna generada: `start_at + INTERVAL duration_minutes MINUTE`.
- La ocupación son **2 filas** en `slot_reservations` (slots con `start_at` y `start_at + 30 min`), insertadas en una sola transacción.
- **Consecutividad y mismo bloque** (SUPUESTO S-22, supuesto de HU-012: no se combinan slots de bloques contiguos): la app selecciona los slots con
  `WHERE professional_id = ? AND start_at IN (?, ? + 30 min)` y verifica que sean 2 filas con el mismo `block_id`. Como `(professional_id, start_at)` es única, esa consulta es determinista.
- Límite honesto: MySQL no ofrece un CHECK entre filas, así que "la cita tiene exactamente N slots contiguos" es invariante de la aplicación (con prueba de integración). Lo que la BD sí garantiza: ningún slot se comparte, todos pertenecen al mismo profesional de la cita, y la duración solo puede ser 30 o 60 con horario alineado.
- Disponibilidad de 60 min (HU-012 CA-02): un inicio es válido si existen los slots `t` y `t+30` del mismo bloque y ninguno tiene fila en `slot_reservations`. Se resuelve con un self-join sobre `uk_slots_professional_start`.

## 6. Conservar la cita original durante una reprogramación pendiente

HECHO (RF-15, RN-10): la nueva franja se retiene y la original se mantiene hasta que ADMIN decide.

| Momento | `appointments` | `slot_reservations` | `reschedule_requests` |
|---|---|---|---|
| Solicitud (USER) | Sin cambios (`APPROVED`, `start_at` original) | Filas originales con `appointment_id = A`, **más** filas nuevas con `reschedule_request_id = R` | `R` en `PENDING` con snapshots `original_*` y `requested_*` |
| Aprobación (ADMIN), 1 transacción | `start_at`, `site_code` ← nuevos; `version`+1 | `DELETE … WHERE appointment_id = A`; `UPDATE … SET appointment_id = A, reschedule_request_id = NULL WHERE reschedule_request_id = R` | `APPROVED`, `decided_by`, `decided_at` |
| Rechazo (ADMIN) | Sin cambios | `DELETE … WHERE reschedule_request_id = R` | `REJECTED` + `decision_reason` (CHECK obligatorio) |
| Cancelación de la cita con `R` pendiente (SUPUESTO S-15) | `CANCELLED` | Borra las filas de `A` y de `R` | `CANCELLED`, `decided_at` |

Garantías de BD:
- Ambas franjas coexisten **sin duplicar slots**: cada slot tiene un único titular (PK + XOR).
- "Nunca quedan ambas franjas asignadas ni ninguna" (HU-019): se cumple por atomicidad transaccional. El `UPDATE` de titular no puede chocar con la PK porque no cambia `slot_id`.
- Solo una solicitud `PENDING` por cita (UK funcional).
- CHECKs de coherencia: `PENDING` ⇒ sin decisor; `APPROVED/REJECTED` ⇒ con decisor y fecha; `REJECTED` ⇒ motivo no vacío; `requested_start_at <> original_start_at`.
- La franja original queda como snapshot (`original_start_at`, `original_site_code`) aunque la cita se sobrescriba.
- Concurrencia entre "ADMIN aprueba" y "USER cancela": ambos actualizan `appointments` con `version` (bloqueo optimista) o con `SELECT … FOR UPDATE` sobre la cita. El segundo en llegar falla y se reevalúa el estado.

Pregunta abierta Q-05: qué pasa si llega la fecha de la cita original con la solicitud aún `PENDING` (¿expiración automática? ¿estado `EXPIRED`?). No se sembró ningún estado para eso.

## 7. Auditoría de estados

- Tabla `appointment_status_history`, **solo inserciones**. HECHO RF-19: cita, estado nuevo, actor si existe, fuente `SYSTEM/USER/ADMIN`, fecha/hora y motivo opcional.
- Restricciones:
  - `CHECK source IN ('SYSTEM','USER','ADMIN')` (HECHO).
  - `CHECK source = 'SYSTEM' OR actor_user_id IS NOT NULL` (SUPUESTO S-17).
  - `CHECK status_code <> 'REJECTED' OR reason` no vacío (HECHO RN-04). Por eso el motivo de rechazo vive aquí y "Mis citas" lo obtiene del registro `REJECTED`.
  - FK `ON DELETE RESTRICT` desde `appointments`: no se puede borrar una cita con historial.
- **Inmutabilidad (RN-12)**: la API no expone UPDATE ni DELETE (HECHO HU-021). Como defensa en profundidad hay triggers `BEFORE UPDATE/DELETE` que hacen `SIGNAL`. Alternativa: dar al usuario de la app solo `SELECT, INSERT` sobre esta tabla. Ver Q-10 por los privilegios con binlog.
- Cambio de horario por reprogramación aprobada (HU-019 "se registra historial de la cita cuando cambia su horario"): se inserta un registro con `status_code = 'APPROVED'` (el estado no cambia), `source = 'ADMIN'` y `reschedule_request_id = R`. La FK compuesta `(reschedule_request_id, appointment_id)` impide enlazar la solicitud de otra cita.
- Cierre por el profesional (RF-17): el PRD no define la fuente `PROFESSIONAL`. **SUPUESTO S-16**: se registra `source = 'USER'` con `actor_user_id` = el profesional (el rol del actor se deduce de `user_roles`). Pregunta Q-01.
- **Redundancia declarada**: `appointments.status_code` duplica el último estado del historial. Se mantiene por rendimiento (bandeja y agenda filtran por estado con índice) y para bloquear la fila en las transiciones. La regla de escritura es: toda transición actualiza `appointments.status_code` **e** inserta el historial en la misma transacción.
- Reprogramaciones: su ciclo de vida queda en la propia fila (`status_code`, `decided_by_user_id`, `decided_at`, `decision_reason`). El PRD solo exige historial de *citas* (HU-021 fuera de alcance: otras entidades).

## 8. Snapshot vs FK

**Criterio:** se usa FK cuando el dato debe reflejar el valor *actual* de otra entidad. Se usa snapshot cuando el dato es un **hecho histórico** de la transacción y el valor de origen puede cambiar o dejar de estar enlazado.

| Dato | Tratamiento | Justificación |
|---|---|---|
| Paciente, profesional, especialidad, sede, estado de la cita | **FK** | Identidad estable; los nombres se obtienen por JOIN (3FN) |
| Nombre de especialidad, sede, profesional o EPS en la cita | **No se copia** (JOIN) | SUPUESTO S-18: renombrar un catálogo se refleja en las citas históricas; el PRD no pide congelar nombres y el laboratorio no tiene historia clínica |
| `appointments.duration_minutes` | **Snapshot** | La duración de la especialidad es editable; los slots ya ocupados no cambian (S-19) |
| `appointments.start_at`, `site_code` | Hecho propio de la cita (con FK para la sede) | Tras cancelar o rechazar se borran las reservas; la cita debe seguir diciendo cuándo y dónde era |
| `reschedule_requests.original_start_at`, `original_site_code` | **Snapshot** | Al aprobar se sobrescribe la cita; sin esto se perdería la franja original |
| `reschedule_requests.requested_start_at`, `requested_site_code` | **Snapshot** | Tras rechazar se liberan los slots retenidos; la solicitud debe conservar qué se pidió |
| `appointment_status_history.*` | **Snapshot inmutable** | Registro de auditoría |
| `users.password_hash`, hashes de tokens | Valor propio | Solo se guarda el hash (HECHO) |
| EPS de la afiliación | **Ni FK ni copia**: se deriva de `plan_id` | Evita la dependencia transitiva |
| Afiliación en la cita | **No se relaciona** | El PRD no vincula la cita con la afiliación (fuera de alcance: facturación). Pregunta Q-11 |

## 9. Índices para consultas de agenda

Los índices de PK, UNIQUE y FK sirven también a estas consultas. Todas las fechas son un rango `[desde, hasta)` sobre `start_at`.

| Consulta (HU) | Índice(s) |
|---|---|
| Disponibilidad por profesional y fecha (HU-012) | `availability_slots.uk_slots_professional_start (professional_id, start_at)` + PK de `slot_reservations` para el anti-join |
| Disponibilidad por sede y fecha (HU-012, HU-010) | `availability_blocks.idx_blocks_site_start (site_code, start_at)` → slots por `idx_slots_block_professional (block_id, professional_id)` |
| Profesionales con especialidad X (HU-012) | `professional_specialties.idx_prof_specialties_specialty (specialty_id, professional_id)` |
| Profesionales en sede X (HU-012) | `professional_sites.idx_prof_sites_site (site_code, professional_id)` |
| Calendario de bloques propio (HU-010) | `availability_blocks.uk_blocks_professional_start (professional_id, start_at)` |
| Agenda del profesional por día o semana (HU-011) | `appointments.idx_appt_professional_start (professional_id, start_at)`; los filtros por estado y sede se aplican sobre el rango |
| Mis citas por estado o fecha (HU-016) | `appointments.idx_appt_patient_start (patient_user_id, start_at)` |
| Bandeja ADMIN: `REQUESTED` por fecha (HU-015, HU-022) | `appointments.idx_appt_status_start (status_code, start_at)`; también `idx_appt_site_start`, `idx_appt_specialty_start` e `idx_appt_professional_start` según el filtro |
| Bandeja ADMIN: `PENDING` (HU-019, HU-022) | `reschedule_requests.idx_rr_status_requested_start (status_code, requested_start_at)` + JOIN por PK a `appointments` |
| Recordatorios `APPROVED` próximos (HU-023) y resumen diario por sede/estado (HU-025) | `idx_appt_status_start`; `idx_appt_site_start` |
| Historial de una cita (HU-021) | `appointment_status_history.idx_ash_appointment_changed (appointment_id, changed_at)` |
| Motivo de rechazo en "Mis citas" (HU-016) | `idx_ash_appointment_changed` (filtrando `status_code = 'REJECTED'`) |
| Reservas de una cita o solicitud (cancelar, aprobar, rechazar) | `slot_reservations.idx_sr_appointment_professional`, `idx_sr_reschedule_request` |
| Sesiones o tokens de un usuario (limpieza, logout global) | `refresh_tokens.idx_refresh_tokens_user_expires`, `password_reset_tokens.idx_password_reset_tokens_user_expires` |

No se añaden índices "por si acaso". Antes de añadir compuestos de tres columnas (p. ej. `(status_code, site_code, start_at)`) hay que medir con `EXPLAIN ANALYZE` sobre datos sintéticos de volumen realista.

---

## Validaciones estáticas realizadas sobre `schema.sql` (sin ejecución)

- Orden de creación compatible con las FK: catálogos fijos → identidad → catálogos configurables → profesionales → agenda → citas → reprogramaciones → reservas → historial. No hay ciclos entre tablas; la única autorreferencia es `refresh_tokens`.
- Tipos PK/FK idénticos (`BIGINT UNSIGNED` ↔ `BIGINT UNSIGNED`; `VARCHAR` con el mismo charset y collation de tabla `utf8mb4_0900_ai_ci`).
- Toda FK referencia una PK o UNIQUE con exactamente esas columnas y en ese orden (requisito de MySQL 8.4).
- Ningún CHECK referencia columnas `AUTO_INCREMENT` ni funciones no deterministas. Por ejemplo, **no** existe `CHECK (replaced_by_token_id <> id)` porque `id` es AUTO_INCREMENT.
- Las columnas usadas en CHECK tienen FK **sin** `ON DELETE/ON UPDATE`. `refresh_tokens.replaced_by_token_id` usa `ON DELETE SET NULL`, por eso no participa en ningún CHECK.
- Índices funcionales con doble paréntesis cuando son la única parte (`((CASE …))`) y con paréntesis propios cuando se combinan con una columna (`(professional_id, (CASE …))`).
- La columna generada `end_at` es STORED y determinista, y sus columnas base no tienen FK con acciones.
- Riesgo pendiente de verificación real: ejecución de triggers con binlog activo y usuario sin privilegios elevados (Q-10). **Este DDL no se ha ejecutado**; debe validarse en un MySQL 8.4 desechable antes de convertirlo en migraciones Flyway.
