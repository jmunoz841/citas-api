# Normalización 1FN → 2FN → 3FN

Explica cómo se llega al modelo de [`schema.sql`](schema.sql) partiendo de una "tabla ingenua" que reúne lo que el PRD pide mostrar. No es la auditoría de un esquema existente: el punto de partida es didáctico.

## 0. Punto de partida (no normalizado)

Una sola relación hipotética que responde a RF-13 ("Mis citas") y RF-04 (perfil):

```
cita_plana(
  cita_id, paciente_email, paciente_nombres, paciente_documento,
  eps_name, plan_name, regime_name,
  profesional_codigo, profesional_nombre, especialidades = 'Cardiología,Medicina General',
  especialidad_cita_name, especialidad_duracion,
  sede_nombre, sede_direccion,
  fecha, hora_inicio, slots = '08:00,08:30',
  estado = 'Aprobada', motivo_rechazo,
  historial = 'REQUESTED@2026-09-10;APPROVED@2026-09-11'
)
```

## 1. Primera forma normal (1FN)

**Problema:** atributos multivaluados y grupos repetidos.

| Violación | Anomalía | Solución en el modelo |
|---|---|---|
| `especialidades = '1,2,3'` (lista en columna, prohibido de forma explícita por la actividad) | No se puede indexar ni validar con FK; eliminar una especialidad obliga a reescribir texto | Tabla puente `professional_specialties(professional_id, specialty_id, is_primary, is_active)` |
| `slots = '08:00,08:30'` | No se puede impedir con una restricción que otro registro use "08:30" | Filas en `availability_slots` + ocupación en `slot_reservations` (una fila por slot) |
| `historial = '…;…'` | Imposible consultar "quién rechazó" o filtrar por fecha; edición destructiva | Tabla `appointment_status_history` (una fila por transición) |
| Usuario con varios roles como `roles='USER,ADMIN'` | Autorización por `LIKE`, errores de subcadena | `user_roles(user_id, role_code)` |

**Atomicidad considerada:** `sites.address` se deja como un único valor porque ninguna HU la consulta por partes (SUPUESTO S-25). `start_at` (DATETIME) sustituye a `fecha` + `hora_inicio`: es un único instante y evita combinaciones inválidas. Nombres y apellidos se guardan en dos columnas porque así los pide RF-01.

Resultado: toda columna es atómica y cada tabla tiene clave primaria.

## 2. Segunda forma normal (2FN)

**Regla:** en tablas con clave compuesta, cada atributo no primo depende de la clave **completa**.

Tablas con PK compuesta del modelo y verificación:

| Tabla | PK | Atributos no primos | ¿Dependen de la clave completa? |
|---|---|---|---|
| `user_roles` | `(user_id, role_code)` | `assigned_at` | Sí: es la fecha de *esa* asignación. |
| `professional_specialties` | `(professional_id, specialty_id)` | `is_primary`, `is_active` | Sí: "primaria" es una propiedad de la especialidad *para ese profesional*. |
| `professional_sites` | `(professional_id, site_code)` | `is_active` | Sí. |

**Ejemplos de dependencias parciales evitadas:**

- Si `professional_specialties` incluyera `specialty_name` o `duration_minutes`, dependerían solo de `specialty_id` (parte de la clave). Anomalía: renombrar "Cardiología" obligaría a actualizar N filas; si falla una, la misma especialidad tendría dos nombres. → Viven en `specialties`.
- Si `professional_sites` incluyera `site_address`, dependería solo de `site_code`. → Vive en `sites`.
- Si `user_roles` incluyera `user_email`, dependería solo de `user_id`. → Vive en `users`.

Las relaciones N:M (profesional–especialidad, profesional–sede, usuario–rol) se resuelven con tablas puente, como pide la actividad.

## 3. Tercera forma normal (3FN)

**Regla:** ningún atributo no primo depende transitivamente de la clave a través de otro atributo no primo.

### 3.1 EPS, plan y régimen (no repetir `eps_name`, `plan_name`, `regime_name`)

- Hecho: `plan → EPS` (HU-007: el plan pertenece a exactamente una EPS).
- Si `users` guardara `eps_name, plan_name, regime_name`: `user_id → plan_name → eps_name` es transitiva. **Anomalías:** actualización (renombrar una EPS = tocar miles de usuarios), inserción (no se puede crear un plan sin un usuario), inconsistencia (plan "Oro" asociado a dos EPS distintas en filas diferentes).
- Modelo: `eps(id, name)`, `eps_plans(id, eps_id, name)`, `regimes(code, name)`, `user_affiliations(id, user_id, plan_id, regime_code)`.
- Además, `user_affiliations` **no** guarda `eps_id`: `id → plan_id → eps_id` sería transitiva. Como consecuencia estructural, la regla "el plan debe pertenecer a la EPS seleccionada" (HU-004 CA-02) no puede violarse en la BD; la API recibe `eps_id` + `plan_id` y valida que coincidan antes de guardar solo `plan_id`.

### 3.2 Especialidad (no repetir `specialty_name` en profesional o cita)

- `appointments` guarda `specialty_id`; el nombre se obtiene con JOIN a `specialties`. Así se evita `appointment_id → specialty_id → specialty_name`.
- **Matiz:** `duration_minutes` **sí** está en `appointments`, y no es una violación. La DF `specialty_id → duration_minutes` es válida en `specialties` en *un instante*, pero no se cumple a lo largo del tiempo para las citas: si ADMIN cambia la duración de 30 a 60, las citas ya reservadas siguen ocupando 1 slot. La duración de la cita es un hecho propio (snapshot) → `appointment_id → duration_minutes` directo.

### 3.3 Estados como catálogo (sin textos divergentes)

- `appointments.status_code` y `appointment_status_history.status_code` son FK a `appointment_statuses(code)`; `reschedule_requests.status_code` es FK a `reschedule_statuses(code)`.
- Evita "Aprobada", "APROBADA", "approved" como valores distintos; `name` e `is_terminal` dependen de `code` y viven solo en el catálogo.
- `source` (SYSTEM/USER/ADMIN) **no** se modela como tabla: no tiene atributos dependientes (no hay DF `source → X`), así que una tabla no aportaría normalización. Se restringe con `CHECK`. Decisión declarada para no normalizar por reflejo.

### 3.4 Sede de la cita y del slot

- `availability_slots` **no** guarda `site_code`: `slot_id → block_id → site_code` sería transitiva con un atributo no primo.
- `availability_slots` **sí** guarda `professional_id` aunque `block_id → professional_id`: se acepta porque `professional_id` es **primo** (clave candidata `(professional_id, start_at)`). La tabla cumple 3FN (no BCNF), y a cambio la BD impide bloques solapados. La FK compuesta impide que la copia diverja.
- `appointments.site_code` no es transitiva: tras cancelar se borran las reservas, así que la sede de la cita no puede derivarse de otra fila; es un hecho de la cita.

### 3.5 Motivo de rechazo y estado actual

- El motivo de rechazo se guarda en `appointment_status_history.reason` (el registro de la transición a `REJECTED`), no en `appointments`. Así se evita tener el mismo dato en dos lugares.
- `appointments.status_code` (estado actual) es derivable del último registro de historial. **Redundancia declarada**: se conserva para filtrar la bandeja y la agenda con un índice y para bloquear la fila en transiciones concurrentes. La consistencia se garantiza escribiendo ambas en la misma transacción (lo cubren pruebas de HU-021).

### 3.6 Datos derivados excluidos o controlados

| Dato | Tratamiento | Motivo |
|---|---|---|
| `appointments.end_at` | Columna generada STORED | Derivada de `start_at + duration_minutes`; el motor impide la divergencia y permite indexar/filtrar |
| Fin de slot | No se almacena | Siempre `start_at + 30 min` |
| Token vigente/expirado | No se almacena | Depende del reloj; se calcula |
| Estado anterior en historial | No se almacena | Se deriva del registro previo |
| Tipo de cita general/especializada | No se almacena en la cita | Se deriva de `specialties.is_general` |

## 4. Resumen por tabla

| Tabla | 1FN | 2FN | 3FN | Nota |
|---|---|---|---|---|
| roles, document_types, regimes, sites, appointment_statuses, reschedule_statuses | ✔ | ✔ (PK simple) | ✔ | Catálogos fijos |
| users, refresh_tokens, password_reset_tokens | ✔ | ✔ | ✔ | |
| user_roles, professional_specialties, professional_sites | ✔ | ✔ | ✔ | Puentes N:M |
| eps, eps_plans, specialties, user_affiliations | ✔ | ✔ | ✔ | EPS derivada del plan |
| professionals | ✔ | ✔ | ✔ | Subtipo 1:0..1 |
| availability_blocks | ✔ | ✔ | ✔ | |
| availability_slots | ✔ | ✔ | ✔ (no BCNF) | `professional_id` primo, redundancia controlada |
| appointments | ✔ | ✔ | ✔ con redundancias declaradas | `end_at` generada; `status_code` actual |
| reschedule_requests | ✔ | ✔ | ✔ | Snapshots de franja |
| slot_reservations | ✔ | ✔ | ✔ | Titular exclusivo (XOR) |
| appointment_status_history | ✔ | ✔ | ✔ con redundancia declarada | Enlace opcional a la solicitud con FK compuesta |
