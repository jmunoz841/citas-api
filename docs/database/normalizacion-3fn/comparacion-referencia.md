# Comparación con el modelo de referencia del trainer

**Actividad:** "comparación posterior contra el modelo de referencia del trainer" (`database/REQUISITOS_NORMALIZACION_3FN.md`, Entregables).
**Diseño propio evaluado:** esta carpeta (`README.md`, `erd.md`, `dependencias-funcionales.md`, `normalizacion-1fn-3fn.md`, `decisiones-de-diseno.md`, `schema.sql`).
**Referencia:** `database/reference/db.sql`, `README_DB.md`, `erd.mmd`.
**Criterios:** `agents/agente-normalizacion-mysql-3fn/01_Normalizacion_MySQL_3FN.md`, PRD, HU aprobadas y decisiones D-003, D-004, D-007, D-008 (y el primer ADMIN diferido a HU-008).
**Método:** análisis estático de los dos DDL. No se ejecutó SQL. Se distingue **HECHO** (lo que dice el DDL o el PRD), **INFERENCIA** (una deducción razonada) y **SUPUESTO** (los S-xx del README propio).
**Neutralidad:** que la referencia sea del trainer no le da prioridad. Cada diferencia se decide por dependencias funcionales (DF), integridad declarativa y requisitos.

---

## 1. Resumen ejecutivo

Los dos modelos están **muy alineados**. Resuelven las mismas entidades con la misma estructura: usuarios con roles N:M, el profesional como subtipo de usuario, puentes profesional–especialidad y profesional–sede, EPS → plan → afiliación, bloques con slots de 30 minutos materializados, cita con FK compuestas a los puentes, reprogramación con snapshots y un historial de estados. 21 de las 23 tablas propias tienen un equivalente directo.

**Fortalezas del diseño propio:**
- Garantiza la no-doble-reserva y el no-solapamiento con restricciones declarativas (PK y UNIQUE). La referencia usa triggers, y en el caso de los bloques la protección no es segura ante concurrencia.
- Guarda la duración de la cita como snapshot. En la referencia, un trigger compara cada UPDATE de la cita con la duración **actual** de la especialidad: si ADMIN cambia la duración, las citas antiguas ya no se pueden actualizar.
- Tiene un catálogo `document_types` (D-008). La referencia guarda el tipo de documento como texto libre.
- Declara con CHECK los invariantes de reprogramación y de auditoría: motivo obligatorio al rechazar, una sola solicitud `PENDING` por cita y actor obligatorio.

**Principales diferencias:**
- Dónde vive el régimen: en la afiliación (propio) o en el plan (referencia).
- Si la afiliación es única o múltiple.
- Si la cita queda vinculada a una afiliación.
- La referencia admite hashes de contraseña más largos, y eso es lo único que conviene adoptar para HU-001.

---

## 2. Mapeo de tablas

| # | Propio | Referencia | Relación |
|---|---|---|---|
| 1 | `roles` (PK `code`) | `roles` (PK `id` SMALLINT, `code` UNIQUE) | Equivalente |
| 2 | `document_types` | — (el tipo va como `users.document_type` VARCHAR libre) | **Solo en propio** |
| 3 | `regimes` | `insurance_regimes` | Equivalente (cambia a quién se asocia, ver §3.3) |
| 4 | `sites` | `locations` | Equivalente |
| 5 | `appointment_statuses` | `appointment_statuses` | Equivalente |
| 6 | `reschedule_statuses` (+ `is_terminal`) | `reschedule_request_statuses` | Equivalente |
| 7 | `users` | `users` | Equivalente (difieren tipos y restricciones, §3.1) |
| 8 | `user_roles` (+ `assigned_at`) | `user_roles` | Equivalente |
| 9 | `refresh_tokens` (+ `replaced_by_token_id`) | `refresh_tokens` | Equivalente (el propio guarda la cadena de rotación) |
| 10 | `password_reset_tokens` (+ `revoked_at`) | `password_reset_tokens` | Equivalente |
| 11 | `eps` | `eps` (+ `code`) | Equivalente |
| 12 | `eps_plans` | `eps_plans` (+ `code`, + `regime_id`) | Equivalente con diferencia de DF (§3.3) |
| 13 | `specialties` | `specialties` (+ `code`, + `requires_admin_approval`) | Equivalente con diferencia (§3.2) |
| 14 | `user_affiliations` (+ `regime_code`) | `user_insurance_affiliations` (+ `membership_number`, `is_current`) | Equivalente con diferencias (§3.3) |
| 15 | `professionals` (PK = `user_id`) | `professionals` (PK `id`, `user_id` UNIQUE) | Equivalente |
| 16 | `professional_specialties` (+ `is_active`) | `professional_specialties` | Equivalente |
| 17 | `professional_sites` (+ `is_active`) | `professional_locations` | Equivalente |
| 18 | `availability_blocks` (`start_at`/`end_at` DATETIME) | `availability_blocks` (`available_date` + `start_time`/`end_time`, `active`) | Equivalente |
| 19 | `availability_slots` | `professional_slots` (parte estructural) | Equivalente |
| 20 | `slot_reservations` | — (la ocupación va como columnas `appointment_id` / `reschedule_request_id` en `professional_slots`) | **Solo en propio** (otra forma de modelar la ocupación) |
| 21 | `appointments` | `appointments` (+ `insurance_affiliation_id`, `scheduled_end_at` almacenado; sin `duration_minutes` ni `version`) | Equivalente con diferencias (§3.5–3.7) |
| 22 | `reschedule_requests` | `reschedule_requests` (+ `requested_by_user_id`, `*_end_at`, `patient_action_after_rejection`; sin sede original) | Equivalente con diferencias (§3.8) |
| 23 | `appointment_status_history` | `appointment_status_history` | Equivalente |

**Solo en la referencia (como tabla):** ninguna. Lo que la referencia añade son columnas: `membership_number`, `is_current`, `insurance_affiliation_id`, `requires_admin_approval`, `requested_by_user_id`, `patient_action_after_rejection` y los `code` de EPS, planes y especialidades.

---

## 3. Diferencias relevantes

Clasificaciones: `ADOPTAR de la referencia` · `MANTENER propio` · `EQUIVALENTE` · `DECISIÓN DEL USUARIO`.

### 3.1 Identidad y tokens

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| PK de `roles` | `code` natural | `id` SMALLINT + `code` UNIQUE | Las dos cumplen 3FN (`code ↔ name`, ambas claves candidatas). Con PK natural, `user_roles` guarda directamente el código que usa Spring Security y la tabla tiene un nivel menos de indirección. El surrogate no aporta nada en un catálogo fijo que nunca se renombra (RF-05) | EQUIVALENTE → MANTENER propio |
| Tipo de documento | FK a `document_types` | `document_type VARCHAR(20)` libre, sin FK | HECHO: la referencia acepta cualquier texto ("CC", "cc", "C.C."). Eso rompe la unicidad efectiva de `(document_type, document_number)` y contradice D-008, que fija una lista cerrada. No viola 3FN en sentido estricto porque el valor es atómico, pero sí la integridad y el requisito de "evitar textos divergentes" | **MANTENER propio** |
| `users.email` | collation `utf8mb4_0900_as_ci` explícita | hereda `utf8mb4_0900_ai_ci` de la BD | HECHO: las dos UNIQUE ignoran mayúsculas, así que las dos cumplen HU-001 CA-03. INFERENCIA: `ai_ci` además ignora acentos ("josé@" = "jose@"), lo que puede dar falsos conflictos. `as_ci` es más precisa | MANTENER propio |
| `users.password_hash` | `VARCHAR(100)` + CHECK ≥ 60 | `VARCHAR(255)` | PRD §8 admite "BCrypt/Argon2". INFERENCIA (cálculo): el formato por defecto de `Argon2PasswordEncoder` de Spring Security con el prefijo `{argon2}` de `DelegatingPasswordEncoder` ronda los 106 caracteres y **no cabe en 100**. Pasar a 255 no cuesta nada y evita una migración futura | **ADOPTAR de la referencia** |
| `users.phone` | `NOT NULL`, 20 | `NULL`, 30 | HECHO: RF-01 y HU-001 exigen el teléfono entre los datos mínimos, así que lo correcto es `NOT NULL`. La referencia es más laxa que el requisito | MANTENER propio |
| Longitudes (`document_number` 30 frente a 40, nombres 100 en ambos) | — | — | Sin impacto en 3FN. 30 caracteres alcanzan para los documentos colombianos (S-24) | EQUIVALENTE |
| Nombres de columnas (`first_names`/`is_active` frente a `first_name`/`active`) | — | — | Convención | EQUIVALENTE |
| CHECK de no-vacío en nombres y de longitud del hash | Sí | No | El CHECK es una defensa en profundidad y no cuesta nada | MANTENER propio |
| `user_roles.assigned_at` | Sí, con DEFAULT | No | `assigned_at` depende de la clave completa, así que cumple 2FN. Con `@ManyToMany` la columna la rellena el DEFAULT | MANTENER propio |
| Borrado en cascada desde `users` | `CASCADE` en `user_roles` y los tokens | NO ACTION (por defecto) | Los usuarios no se borran físicamente (S-21), así que en la práctica no hay diferencia | EQUIVALENTE |
| `refresh_tokens.token_hash` | `CHAR(64) ascii_bin` | `CHAR(64)` con collation `ai_ci` | Un hash hexadecimal se compara byte a byte. `ascii_bin` es exacta y ocupa menos | MANTENER propio |
| Cadena de rotación | `replaced_by_token_id` (autorreferencia, UNIQUE, `ON DELETE SET NULL`) | No | D-003 exige rotar y revocar, y las dos lo cumplen con `revoked_at`. La cadena propia permite además detectar la reutilización de un token rotado (buena práctica). HECHO (manual de MySQL): una autorreferencia con `ON DELETE SET NULL` está permitida | MANTENER propio |
| CHECK temporales (`expires_at > issued_at`, `revoked_at ≥ issued_at`) | Sí | No | Defensa declarativa | MANTENER propio |
| Índice de tokens por usuario | `(user_id, expires_at)` | `(user_id, expires_at, revoked_at)` | La diferencia es marginal: las dos sirven para buscar por usuario y para la limpieza | EQUIVALENTE |
| `password_reset_tokens.revoked_at` | Sí (S-26) | No | Fuera de HU-001. Sirve para invalidar tokens anteriores | MANTENER propio |
| Seed de usuarios | Ninguno (primer ADMIN diferido a HU-008) | `README_DB.md` describe una contraseña compartida para usuarios sintéticos | Contradice la decisión vigente y la regla de no poner contraseñas en migraciones. No se adopta | MANTENER propio |

### 3.2 Catálogos

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| PK de los catálogos fijos | `code` natural | `id` SMALLINT + `code` | El PK natural permite CHECK sobre códigos: `status_code <> 'REJECTED' OR reason…` y la UNIQUE funcional de `PENDING`. Con un id numérico esos CHECK dependerían de ids sembrados, que son frágiles. Por eso la referencia no tiene esos CHECK | MANTENER propio |
| `specialties.requires_admin_approval` | No existe; se deriva de `is_general` | Existe, con `CHECK (is_general ⇒ NOT requires_admin_approval)` | HECHO (RN-02 y RN-03): general implica aprobación automática y especializada implica aprobación de ADMIN. Por lo tanto `is_general → requires_admin_approval`, una DF entre dos atributos no primos: **dependencia transitiva**. Además, el CHECK solo cubre una dirección: admite `is_general = FALSE, requires_admin_approval = FALSE`, es decir, una especializada sin aprobación, lo que viola RN-03 | **MANTENER propio** |
| Número de especialidades generales | Como máximo una (UNIQUE funcional, S-09) | Sin límite | El PRD habla de "Medicina General" en singular. Es un supuesto propio y no se exige a la referencia | MANTENER propio (S-09) |
| `code` en `eps`, `eps_plans` y `specialties` | No | Sí, UNIQUE | Es un identificador estable, útil para integraciones o seeds idempotentes. El PRD no lo pide y cumple 3FN en ambos casos. Solo se justifica si una HU lo necesita | EQUIVALENTE (opcional) |
| `reschedule_statuses.is_terminal` | Sí | No | Es un dato menor que permite validar transiciones sin código fijo | MANTENER propio |
| Sedes | `sites` con PK `code` | `locations` con PK `id` | Mismo contenido público del PRD | EQUIVALENTE |

### 3.3 Afiliación (EPS, plan, régimen)

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| Ubicación del régimen | `user_affiliations.regime_code` (S-08: el plan no determina el régimen) | `eps_plans.regime_id` (el plan determina el régimen) | **Las dos cumplen 3FN, pero bajo DF distintas.** Si `plan_id → regime`, guardarlo en la afiliación sería transitivo y la referencia tendría razón. Si no existe esa DF, ponerlo en el plan impide representar a un afiliado de un plan en el otro régimen. HECHO: HU-004 describe la afiliación como la asociación de "EPS, plan y régimen" y prohíbe duplicados sobre la terna "misma EPS/plan/régimen". Esa redacción supone que el régimen se elige por separado, lo que favorece al diseño propio | **DECISIÓN DEL USUARIO** (con evidencia a favor del propio) |
| Cardinalidad | Varias afiliaciones por usuario, UNIQUE `(user, plan, régimen)` | Varias históricas y **una sola vigente** (`is_current` + columna generada UNIQUE) | HU-004 prohíbe duplicados pero no dice si puede haber varias vigentes (Q-02). Las dos técnicas (columna generada o índice funcional) son equivalentes | **DECISIÓN DEL USUARIO** (Q-02) |
| `membership_number` | No | `NOT NULL` y parte de la UNIQUE | Ni el PRD ni HU-004 lo mencionan. Adoptarlo sería inventar un requisito | MANTENER propio (salvo decisión) |
| EPS en la afiliación | Se deriva de `plan_id` | Se deriva de `plan_id` | Idéntico: las dos evitan `eps_id` transitivo | EQUIVALENTE |

### 3.4 Profesionales

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| PK de `professionals` | `user_id` (subtipo 1:0..1) | `id` propio + `user_id` UNIQUE | Las dos cumplen 3FN. La referencia añade una clave candidata más sin beneficio funcional | EQUIVALENTE → MANTENER propio |
| Una sola especialidad primaria | Índice funcional `(professional_id, CASE…)` | Columna generada `primary_professional_id` UNIQUE | Técnicas equivalentes | EQUIVALENTE |
| `is_active` en los puentes | Sí (S-10) | No | HECHO: la FK RESTRICT desde citas y bloques impide borrar una asignación que ya se usó. Sin `is_active`, la referencia no puede "retirar" una especialidad o sede de un profesional que tiene historial (RN-08, RF-07) | MANTENER propio |

### 3.5 Agenda y slots

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| Representación del bloque | `start_at` / `end_at` DATETIME + CHECK de mismo día y alineación a :00/:30 | `available_date` + `start_time` / `end_time` TIME + `active` | La referencia impide por estructura que un bloque cruce la medianoche, pero **no exige alineación**: admite 08:15–12:00, que no se discretiza en slots de 30 minutos. El propio sí la exige | MANTENER propio |
| Bloques solapados | UNIQUE `(professional_id, start_at)` en `availability_slots` (declarativo) | Trigger `BEFORE INSERT/UPDATE` con `EXISTS` | INFERENCIA: el `SELECT` del trigger es una lectura consistente sin bloqueo, así que dos inserciones concurrentes de bloques solapados pueden pasar las dos. La UNIQUE propia es segura ante concurrencia (HU-010 CA-03) | **MANTENER propio** |
| Slot contenido en su bloque | Aplicación, más FK compuesta al bloque | Trigger | La referencia protege más en la BD, a cambio de depender de triggers (Q-10: privilegios con binlog en Flyway) | EQUIVALENTE (con costes distintos) |
| `end_at` del slot | Derivado, no se guarda | Se guarda, con `CHECK = start + 30` | En la referencia es una DF `start_at → end_at` entre no primos, controlada por el CHECK: redundancia sin anomalía | EQUIVALENTE |
| Baja de bloques | Borrado físico, bloqueado por `slot_reservations` | `active = FALSE` | Las dos cumplen RF-08 | EQUIVALENTE |

### 3.6 Doble reserva

| Propio | Referencia |
|---|---|
| Tabla `slot_reservations` con `PRIMARY KEY (slot_id)` y CHECK XOR de titular. Ocupar un slot es un INSERT y liberarlo es un DELETE. La 2.ª inserción concurrente falla con 1062 | Columnas `appointment_id` / `reschedule_request_id` en `professional_slots`. Ocupar es un UPDATE. El trigger `bu_professional_slots_integrity` impide pasar un slot ocupado a otro titular y el CHECK impide dos titulares |

**Evaluación:** los dos son seguros ante concurrencia para las reservas. INFERENCIA: en la referencia, el UPDATE bloquea la fila y el trigger ve el `OLD` ya confirmado, así que el segundo intento falla. La diferencia está en **cómo** se garantiza: el propio usa una restricción declarativa y no depende de triggers. La referencia depende de un trigger que mira `OLD`/`NEW`, que es fácil de romper en un refactor y requiere privilegios de creación de triggers. La referencia tiene una tabla menos. **MANTENER propio.** Esta comparación respalda la propuesta abierta Q-003.

### 3.7 Citas de 60 minutos

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| Duración | `duration_minutes` como snapshot + `end_at` como columna generada | `scheduled_end_at` almacenado; los triggers exigen que coincida con la duración **actual** de la especialidad | HECHO (DDL): `bu_appointments_integrity` vuelve a leer `specialties.appointment_duration_minutes` en **cada** UPDATE de `appointments`. Si ADMIN cambia la duración (RF-06), cualquier UPDATE posterior de una cita antigua (cancelar, aprobar, cerrar) falla con `SIGNAL`. Es un defecto funcional. El snapshot propio lo evita (S-19) | **MANTENER propio** |
| Dos slots consecutivos | 2 filas en `slot_reservations`; la contigüidad la valida la aplicación | 2 slots con el mismo `appointment_id`; el trigger valida que estén contenidos en la franja | Ninguno garantiza en la BD que haya exactamente N slots contiguos. La referencia valida en la BD que estén contenidos; el propio valida en la BD el profesional (FK compuesta) | EQUIVALENTE |
| Alineación de la hora de inicio | CHECK | No | — | MANTENER propio |
| Bloqueo optimista | `version` | No | Es útil ante la carrera "ADMIN aprueba / USER cancela" | MANTENER propio |

### 3.8 Reprogramación

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| Snapshot de la franja original | `original_start_at` + `original_site_code` | `previous_start_at` / `previous_end_at`, **sin sede original** | HECHO: la referencia permite `requested_location_id` distinta. Al aprobar, la cita cambia de sede y **la sede original se pierde**. El propio conserva el dato (decisión 6 de los requisitos) | **MANTENER propio** |
| `*_end_at` almacenados | No (se derivan de start + duración de la cita) | Sí | Es una redundancia de la referencia entre tablas | MANTENER propio |
| Una sola solicitud `PENDING` por cita | UNIQUE funcional (S-14) | No | El propio es más estricto (supuesto de HU-018) | MANTENER propio |
| Coherencia de estados | CHECK (`PENDING` sin decisor; `APPROVED`/`REJECTED` con decisor; `REJECTED` con motivo) | Ninguno | RN-04 queda garantizada en la BD solo en el propio | MANTENER propio |
| `requested_by_user_id` | No | Sí | RF-15 dice que la solicita el USER dueño de la cita. INFERENCIA: si siempre es el paciente, `appointment_id → patient → requested_by` es transitiva. Solo aportaría algo si ADMIN pudiera solicitar en nombre del paciente, y no hay HU que lo diga | MANTENER propio |
| `patient_action_after_rejection` | No; conservar la cita = no hacer nada, cancelar = transición registrada en el historial | `VARCHAR(30)` libre | RF-15 no exige registrar la elección. Como texto libre sin catálogo ni CHECK, genera los "textos divergentes" que prohíben los requisitos | MANTENER propio |
| Retención de la franja nueva | Filas en `slot_reservations` con `reschedule_request_id` | Columna `reschedule_request_id` en el slot | Mismo concepto de titular exclusivo | EQUIVALENTE |

### 3.9 Auditoría

| Tema | Propio | Referencia | Evaluación | Clasificación |
|---|---|---|---|---|
| Fuente del cambio | `VARCHAR` + CHECK | `ENUM('SYSTEM','USER','ADMIN')` | Mismo dominio cerrado | EQUIVALENTE |
| Actor obligatorio si la fuente es `USER` o `ADMIN` | CHECK (S-17) | No | — | MANTENER propio |
| Motivo obligatorio en `REJECTED` | CHECK | No, y además la referencia no tiene otra columna para el motivo de rechazo de la cita | RN-04 queda garantizada en la BD solo en el propio | **MANTENER propio** |
| Inmutabilidad (RN-12) | Triggers opcionales o `GRANT` (Q-10) | Nada | — | MANTENER propio |
| Enlace a la reprogramación aprobada | `reschedule_request_id` con FK compuesta | No | Da trazabilidad del cambio de horario (HU-019) | MANTENER propio |

### 3.10 Índices

Cobertura equivalente para las consultas de agenda, bandeja, "Mis citas" e historial. La referencia tiene `ix_slots_available (block, appointment, reschedule, start)`. En el propio, ese papel lo cumple el anti-join contra la PK de `slot_reservations`. El propio añade índices por sede y por especialidad en `appointments` y por estado en `reschedule_requests`. **EQUIVALENTE.** Antes de añadir más índices hay que medirlos con `EXPLAIN ANALYZE`.

### 3.11 Tipos y collations

| Tema | Propio | Referencia | Clasificación |
|---|---|---|---|
| Charset/collation | Explícitos en cada tabla | Heredados de `CREATE DATABASE` | **MANTENER propio.** Flyway no crea la base de datos: si la BD del contenedor tuviera otra collation por defecto, las tablas de la referencia la heredarían en silencio |
| Precisión temporal | `DATETIME(6)` en auditoría y tokens; `DATETIME` en agenda | `DATETIME` | EQUIVALENTE. Las dos son compatibles con D-007 (hora civil de America/Bogota) |
| PK de catálogos | `VARCHAR` natural | `SMALLINT` | Ver §3.2 |
| Uso de triggers | Solo los de inmutabilidad, y son opcionales | 7 triggers de integridad con `DELIMITER` | MANTENER propio: menos dependencia de privilegios con binlog y de cómo Flyway procesa `DELIMITER` |

---

## 4. Verificación 3FN de la referencia

| # | Hallazgo | Tipo | ¿Viola 3FN? | ¿El propio lo resuelve mejor? |
|---|---|---|---|---|
| 1 | `specialties.requires_admin_approval` está determinado por `is_general` (RN-02/RN-03) | La regla es un HECHO; la DF es una INFERENCIA directa | **Sí**: `id → is_general → requires_admin_approval`, con los dos no primos. Además, el CHECK solo cubre una dirección | Sí: el propio lo deriva de `is_general` |
| 2 | `appointments.insurance_affiliation_id → patient_user_id`, porque la afiliación pertenece a un usuario | HECHO (FK + trigger) | Formalmente sí: una DF entre no primos. Está controlada por un trigger, así que no hay anomalía | El propio no tiene esa columna (Q-11). Si se añadiera, lo haría con FK compuesta `(affiliation_id, patient_user_id)`, de forma declarativa y sin trigger. Es el mismo patrón que el propio ya declara en `appointment_status_history` |
| 3 | `reschedule_requests.requested_by_user_id` es derivable de la cita si solo solicita el paciente | INFERENCIA | Depende de la regla. Con RF-15 tal como está, es redundante | Sí (lo omite) |
| 4 | `professional_slots.end_at` = `start_at` + 30 | HECHO (CHECK) | Es una redundancia controlada, sin anomalía | Lo deriva |
| 5 | `reschedule_requests.previous_end_at` / `requested_end_at` son derivables de la hora de inicio y de la duración de la cita | INFERENCIA | Es una redundancia entre tablas, no una violación intra-tabla | Sí |
| 6 | `users.document_type` como texto libre | HECHO | No: el valor es atómico. Pero rompe la integridad y D-008 | Sí (catálogo + FK) |
| 7 | `patient_action_after_rejection` como texto libre | HECHO | No, pero va contra "estados coherentes" | Sí (no lo necesita) |
| 8 | Duración de la cita validada contra el valor **actual** de la especialidad | HECHO (trigger) | No es un tema de 3FN; es un defecto de integridad temporal: no hay snapshot | Sí (snapshot) |
| 9 | Falta la sede original en `reschedule_requests` | HECHO | No; es una pérdida de información histórica | Sí |

Lo que la referencia hace bien y el propio también cubre: el EPS se deriva del plan, los puentes N:M tienen PK compuesta, las FK compuestas `(professional, specialty)` y `(professional, location)` están en citas y bloques, y las unicidades condicionales se implementan con columnas generadas. Esto último es válido según los criterios del agente (§6).

**Conclusión:** la referencia cumple 3FN en casi todo. La única violación real y evitable es el hallazgo 1. Los hallazgos 2 a 5 son redundancias protegidas o derivables. La mayor diferencia de calidad no está en la normalización, sino en la **integridad declarativa**: CHECK frente a nada, UNIQUE frente a triggers y snapshot de la duración.

---

## 5. Impacto en la migración inicial de HU-001

Tablas: `roles`, `document_types`, `users`, `user_roles`, `refresh_tokens`.

### Cambios recomendados antes de escribir la V1 de Flyway

| # | Tabla.columna | Cambio | Justificación |
|---|---|---|---|
| 1 | `users.password_hash` | `VARCHAR(100)` → **`VARCHAR(255)`**. Se mantiene `chk_users_password_hash_len` (≥ 60) | PRD §8 admite BCrypt **o** Argon2. INFERENCIA (cálculo): un hash Argon2 de Spring Security con el prefijo `{argon2}` ronda los 106 caracteres y no cabe en 100. HU-001 usa BCrypt, que ocupa 60 o 68 con prefijo y cabe en los dos tamaños, pero ampliar ahora evita un `ALTER` futuro sin coste de almacenamiento (es un `VARCHAR`). Es la única diferencia en la que la referencia es mejor para estas 5 tablas |

### Qué no cambia (se mantiene el diseño propio)

| Tabla | Se mantiene | Motivo |
|---|---|---|
| `roles` | PK `code VARCHAR(20)` | Catálogo fijo; el código es el que usa Spring Security; no necesita surrogate |
| `document_types` | Tabla + FK desde `users`, seed CC/CE/TI/RC/PA/PPT | D-008; la referencia no tiene equivalente y es más débil |
| `users` | `email` con `utf8mb4_0900_as_ci`, UNIQUE `(document_type_code, document_number)`, `phone NOT NULL`, CHECK de no-vacío | CA-03, CA-04, RF-01 |
| `user_roles` | PK `(user_id, role_code)`, `assigned_at` con DEFAULT, CASCADE desde `users` | Cumple 2FN; compatible con `@ManyToMany` |
| `refresh_tokens` | `token_hash CHAR(64) ascii_bin` UNIQUE, `replaced_by_token_id` autorreferenciada con `ON DELETE SET NULL` y UNIQUE, CHECK temporales, índice `(user_id, expires_at)` | D-003 (rotación + hash); permite detectar reutilización |
| Todas | `ENGINE=InnoDB`, charset y collation explícitos por tabla, seeds sin `ON DUPLICATE KEY` (una migración versionada se ejecuta una sola vez), **ningún usuario sembrado** | Primer ADMIN diferido a HU-008; nada de contraseñas en migraciones |

### Puntos a verificar al implementar (no son cambios de diseño)

- **Validación de Hibernate:** si se usa `spring.jpa.hibernate.ddl-auto=validate`, comprobar que acepta `CHAR(64)` para un `String` (según la versión puede esperar `VARCHAR`). Si falla, usar `columnDefinition = "char(64)"` en la entidad. No hay que cambiar el DDL. INFERENCIA, pendiente de verificar con `mvn test`.
- **Cascada con autorreferencia:** HECHO según el manual de MySQL 8.4: se admite `ON DELETE SET NULL` en una FK autorreferenciada. Aun así, hay que ejecutar la V1 contra `jmunoz-citas-mysql` (puerto 3308) antes de declarar la DoD-02.
- **Hash del token:** la aplicación debe generar el SHA-256 en hexadecimal **en minúsculas** siempre, porque `ascii_bin` distingue mayúsculas.

---

## 6. Preguntas para el usuario

| ID | Pregunta | Opciones | Afecta a |
|---|---|---|---|
| C-01 | ¿Apruebas ampliar `users.password_hash` a `VARCHAR(255)` en la V1? | Sí (recomendado) / No, dejar `VARCHAR(100)` y cerrar la puerta a Argon2 | V1 de HU-001 |
| C-02 | ¿El régimen lo determina el plan (referencia) o se elige en la afiliación (propio, S-08)? | Plan → `eps_plans.regime_code` / Afiliación → se mantiene | HU-004, HU-007 (no afecta a HU-001) |
| C-03 | ¿Un usuario tiene una sola afiliación vigente (referencia, `is_current`) o puede tener varias (propio)? Es la misma pregunta que Q-02 | Una vigente / Varias | HU-004 |
| C-04 | ¿La cita debe registrar con qué afiliación se atendió (referencia, `insurance_affiliation_id`)? Es la misma pregunta que Q-11. Si la respuesta es sí, se implementaría con FK compuesta `(affiliation_id, patient_user_id)` | Sí / No | HU-013, HU-014 |
| C-05 | ¿Se aprueba como DECISIÓN la estrategia propia contra la doble reserva (`availability_slots` + `slot_reservations` con PK por slot), cerrando la pregunta abierta Q-003? | Aprobar / Adoptar el modelo de columnas en el slot con trigger de la referencia | HU-010, HU-013 |

No se plantean como preguntas `membership_number`, `requested_by_user_id`, `patient_action_after_rejection` ni `requires_admin_approval`: el PRD no los pide o contradicen 3FN y RN-03, así que se mantiene el diseño propio salvo que el usuario diga lo contrario.
