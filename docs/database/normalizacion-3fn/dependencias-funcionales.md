# Dependencias funcionales relevantes

Notación: `X → Y` significa que X determina Y. Etiquetas:

- **[E]** Explícita: sale de una PK/UK del DDL respaldada por un HECHO del PRD, una HU o una decisión aprobada.
- **[I]** Inferencia razonable del dominio.
- **[S]** SUPUESTO de diseño (ver lista en [README](README.md#supuestos)).
- **[N]** DF que **no** se cumple a propósito (se documenta porque justifica una decisión).

"Atributo primo" = pertenece a alguna clave candidata. En 3FN, para toda DF no trivial `X → A`, X es superclave **o** A es primo.

---

## Catálogos fijos

| Tabla | Claves candidatas | Dependencias |
|---|---|---|
| `roles` | `code`, `name` | [E] `code → name`; [I] `name → code` |
| `document_types` | `code`, `name` | [S] `code → name`; `name → code` |
| `regimes` | `code`, `name` | [S] `code → name`; `name → code` |
| `sites` | `code`, `name` | [E] `code → name, address` (PRD §3); [I] `name → code, address` |
| `appointment_statuses` | `code`, `name` | [E] `code → name`; [I] `code → is_terminal` (HU-017 lista los terminales) |
| `reschedule_statuses` | `code`, `name` | [E] `code → name`; [S] `code → is_terminal` |

Ninguna tabla tiene atributos no primos que dependan de otro atributo no primo: 3FN.

## Identidad

### `users`
- Claves candidatas: `id`; `email` [E: único, sin distinguir mayúsculas]; `(document_type_code, document_number)` [E: documento único por tipo + número].
- [E] `id → first_names, last_names, document_type_code, document_number, email, phone, password_hash, is_active, created_at, updated_at`
- [E] `email → id` (y por tanto todos los atributos)
- [E] `(document_type_code, document_number) → id`
- [N] `document_type_code → nombre del tipo` **no** se almacena aquí: vive en `document_types` (evita la dependencia transitiva `id → document_type_code → name`).
- [N] No existe `eps_name`, `plan_name` ni `regime_name` en el usuario: la afiliación va aparte.

### `user_roles`
- Clave: `(user_id, role_code)`.
- [E] `(user_id, role_code) → assigned_at`. No hay dependencia parcial: `assigned_at` describe la asignación, no al usuario ni al rol. Cumple 2FN.

### `refresh_tokens`
- Claves candidatas: `id`, `token_hash`; `replaced_by_token_id` es única pero admite NULL (no es clave candidata estricta).
- [E] `id → user_id, token_hash, issued_at, expires_at, revoked_at, replaced_by_token_id`
- [E] `token_hash → id` (se guarda solo el hash y se busca por él; D-003)
- [I] "vigente" = `revoked_at IS NULL AND expires_at > ahora`. **No** se almacena como columna (dependería del tiempo y de otras columnas).

### `password_reset_tokens`
- Claves candidatas: `id`, `token_hash`.
- [E] `id → user_id, token_hash, created_at, expires_at, used_at, revoked_at`
- [E] `token_hash → id`

## Catálogos configurables y afiliación

### `eps`
- Claves: `id`, `name` [E: nombre de EPS único, HU-007].
- [E] `id → name, is_active, created_at, updated_at`

### `eps_plans`
- Claves: `id`, `(eps_id, name)` [E: nombre único dentro de su EPS].
- [E] `id → eps_id, name, is_active, …`
- [E] `(eps_id, name) → id`

### `user_affiliations`
- Claves: `id`, `(user_id, plan_id, regime_code)` [E: sin afiliaciones duplicadas, HU-004].
- [E] `id → user_id, plan_id, regime_code, created_at, updated_at`
- [E] `plan_id → eps_id` se cumple **en `eps_plans`**, no aquí. Por eso la afiliación **no** guarda `eps_id`: si lo guardara, `id → plan_id → eps_id` sería una dependencia transitiva con `eps_id` no primo (violación de 3FN) y podría desincronizarse (plan de otra EPS).
- [S] `plan_id ↛ regime_code`: se supone que el régimen no está determinado por el plan (S-08). Si el negocio confirma lo contrario, `regime_code` debe moverse a `eps_plans`.

### `specialties`
- Claves: `id`, `name` [E: nombre único, HU-006].
- [E] `id → name, duration_minutes, is_general, is_active, …`
- [E] `duration_minutes ∈ {30, 60}` (RF-09) — restricción de dominio, no DF.
- [S] Como máximo una fila con `is_general = 1` (S-09).

## Profesionales

### `professionals`
- Claves: `user_id`, `professional_code` [E], `license_number` [E] (HU-008).
- [E] `user_id → professional_code, license_number, is_active, …`
- [N] Nombres, email y documento **no** se repiten: `user_id → first_names…` vive en `users`.
- [S] `is_active` del profesional es independiente de `users.is_active` (S-06): no hay DF entre ambos.

### `professional_specialties`
- Clave: `(professional_id, specialty_id)`.
- [E] `(professional_id, specialty_id) → is_primary` (RF-07: se marca la primaria *del profesional* entre *sus* especialidades → depende de la clave completa).
- [S] `(professional_id, specialty_id) → is_active` (S-10).
- [N] `specialty_id → specialty_name, duration_minutes` **no** se repite aquí (evita dependencia parcial y viola 2FN si se incluyera).

### `professional_sites`
- Clave: `(professional_id, site_code)`.
- [S] `(professional_id, site_code) → is_active` (S-10).

## Agenda

### `availability_blocks`
- Claves: `id`; `(professional_id, start_at)` [I: con bloques sin solapamiento, un profesional no puede iniciar dos bloques a la misma hora].
- [E] `id → professional_id, site_code, start_at, end_at, …`
- [I] `(professional_id, start_at) → id`
- [N] "fecha del bloque" no se guarda aparte: `start_at → DATE(start_at)` la haría derivada.

### `availability_slots`
- Claves candidatas: `id`; `(professional_id, start_at)` [E: RF-08, sin solapamiento]. `(id, professional_id)` es superclave (existe solo como destino de FK).
- [E] `id → block_id, professional_id, start_at`
- [I] `block_id → professional_id`. **No viola 3FN**: `professional_id` es atributo **primo** (forma parte de la clave candidata `(professional_id, start_at)`). Viola BCNF, y se acepta porque es lo que permite la UNIQUE que evita solapamientos en la BD. La FK compuesta `(block_id, professional_id)` impide que la copia diverja.
- [I] `block_id → site_code` existe, y por eso `site_code` **no** se almacena en el slot (sería no primo → violación de 3FN).
- [I] `end_at = start_at + 30 min`: derivable, no se almacena.

## Citas

### `appointments`
- Claves: `id`. `(id, professional_id)` es superclave (destino de FK).
- [E] `id → patient_user_id, professional_id, specialty_id, site_code, start_at, duration_minutes, status_code, version, created_at, updated_at`
- [N] `specialty_id ↛ duration_minutes` **en esta tabla**: la duración es un *snapshot* al reservar (S-19). Si ADMIN cambia la duración de la especialidad, las citas existentes conservan la suya. No hay dependencia transitiva.
- [I] `(start_at, duration_minutes) → end_at`: dependencia entre no primos, **resuelta con columna generada STORED** (el motor la calcula; no hay anomalía de actualización posible). Decisión declarada.
- [N] `(professional_id, start_at)` **no** es clave: una cita cancelada y otra nueva pueden compartir profesional y hora. La exclusividad temporal vive en `slot_reservations`.
- [I] `status_code` es el estado actual; también se puede derivar del último registro del historial → redundancia declarada (ver decisiones §7).
- [N] No se almacenan `specialty_name`, `site_name`, `professional_name` ni `rejection_reason` (el motivo vive en el historial).

### `reschedule_requests`
- Claves: `id`. `(id, appointment_id)` es superclave (destino de FK).
- [E] `id → appointment_id, original_start_at, original_site_code, requested_start_at, requested_site_code, status_code, decided_by_user_id, decided_at, decision_reason, …`
- [N] `appointment_id ↛ original_start_at`: después de aprobar, la cita cambia su `start_at`; el valor original solo existe como snapshot aquí.
- [E/S] Para `status_code = 'PENDING'`, `appointment_id → id` (máximo una pendiente por cita, S-14), garantizado con UK funcional.

### `slot_reservations`
- Clave: `slot_id` [E: RN-01, un slot no puede estar ocupado dos veces].
- [E] `slot_id → professional_id, appointment_id, reschedule_request_id, created_at`
- [I] `slot_id → professional_id` depende directamente de la clave (y la FK compuesta lo iguala con el slot y la cita). No es transitiva.
- [I] Exclusión mutua: exactamente uno de `appointment_id` / `reschedule_request_id` no es nulo. Por eso **no** existe `reschedule_request_id → appointment_id` dentro de la tabla (si ambos se guardaran, la dependencia sería transitiva).

### `appointment_status_history`
- Clave: `id`.
- [E] `id → appointment_id, status_code, source, actor_user_id, reason, changed_at` (RF-19)
- [I] `id → reschedule_request_id`
- [I] `reschedule_request_id → appointment_id` (cuando no es nulo). Es una **redundancia declarada**: `appointment_id` es obligatorio para todas las filas y el enlace a la solicitud es opcional; la FK compuesta `(reschedule_request_id, appointment_id)` hace imposible la inconsistencia (no hay anomalía de actualización porque el historial es inmutable).
- [N] No se almacena el "estado anterior": se deriva del registro previo de la misma cita.
