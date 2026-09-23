# Normalización 3FN — Modelo relacional propio

**Proyecto:** Sistema de Agendamiento de Citas (laboratorio FCV, datos sintéticos)
**Motor:** MySQL 8.4 · InnoDB · utf8mb4
**Modo:** diseño propio a partir de requisitos (no es la auditoría de un `db.sql` existente)
**Fuentes:** `PRD.md`, `RESTRICCIONES_TECNICAS.md`, `database/REQUISITOS_NORMALIZACION_3FN.md`, HU-001…HU-025 (`docs/wiki/scrum/`), `docs/wiki/llm-wiki/wiki/decisiones.md`
**Estado:** validado. El 2026-09-18 `schema.sql` se cargó sin errores en MySQL 8.4.11 (base desechable) y 9 pruebas de restricciones de HU-001 dieron el resultado esperado. Comparado con la referencia en [comparacion-referencia.md](comparacion-referencia.md) (decisiones D-012 y D-013). El subconjunto de HU-001 está en la migración `src/main/resources/db/migration/V1__identidad_hu001.sql`.
**Independencia:** el diseño se hizo sin consultar el modelo de referencia del trainer. La comparación se hará después, por separado.

---

## Diagnóstico ejecutivo

- **23 tablas** en 6 grupos: 6 catálogos fijos, 4 de identidad, 4 de catálogos configurables y afiliación, 3 de profesionales, 2 de agenda y 4 del ciclo de vida de citas.
- **Todas cumplen 3FN.** Hay tres redundancias controladas y **declaradas**, cada una protegida por la BD:
  1. `availability_slots.professional_id` (atributo primo; FK compuesta al bloque): permite impedir en la BD que los bloques se solapen.
  2. `appointments.status_code` (estado actual, derivable del historial): se mantiene para filtrar con índice y bloquear la fila en las transiciones.
  3. `appointment_status_history.appointment_id` junto al enlace opcional `reschedule_request_id` (FK compuesta).
  Además, `appointments.end_at` es una **columna generada**, así que no puede tener anomalías.
- **Doble reserva:** se impide **con una restricción de BD** (`slot_reservations` con `PRIMARY KEY (slot_id)`), no solo con lógica de la app. Resiste la concurrencia: el segundo INSERT falla con `ER_DUP_ENTRY`.
- **60 min** = 2 filas de ocupación sobre slots consecutivos del mismo bloque, en una sola transacción. La duración se guarda como snapshot en la cita.
- **Reprogramación:** la franja original (titular = cita) y la provisional (titular = solicitud `PENDING`) coexisten sin compartir slots. La aprobación intercambia los titulares de forma atómica.
- **3FN aplicada a EPS/plan/régimen:** la afiliación guarda `plan_id` y `regime_code`; la EPS se deriva del plan. Así "plan de otra EPS" es imposible por estructura.
- **Qué queda en la aplicación** (MySQL no lo expresa con restricciones declarativas): "no en el pasado", catálogo o profesional activo al reservar, cotas mínimas (≥1 rol, ≥1 especialidad, ≥1 sede, exactamente 1 primaria), número y contigüidad de los slots de una cita y transiciones de estado válidas.

## Índice de archivos

| Archivo | Contenido |
|---|---|
| [README.md](README.md) | Este resumen: diagnóstico, supuestos, preguntas abiertas y subconjunto para HU-001 |
| [erd.md](erd.md) | Diagrama ER (Mermaid) y tabla de cardinalidades explicadas |
| [dependencias-funcionales.md](dependencias-funcionales.md) | DF por tabla (explícitas, inferidas, supuestas y "no-DF" que justifican decisiones) |
| [normalizacion-1fn-3fn.md](normalizacion-1fn-3fn.md) | Recorrido 1FN → 2FN → 3FN con anomalías evitadas |
| [decisiones-de-diseno.md](decisiones-de-diseno.md) | Respuesta a las 9 decisiones requeridas y validaciones estáticas del DDL |
| [schema.sql](schema.sql) | DDL MySQL 8.4 completo + seeds de catálogos fijos |

## Tablas

| # | Tabla | Grupo | Propósito |
|---|---|---|---|
| 1 | `roles` | Catálogo fijo | USER, PROFESSIONAL, ADMIN |
| 2 | `document_types` | Catálogo fijo | Tipos de documento (S-01) |
| 3 | `regimes` | Catálogo fijo | Regímenes de afiliación |
| 4 | `sites` | Catálogo fijo | Sedes HIC e ICV con dirección |
| 5 | `appointment_statuses` | Catálogo fijo | Estados de cita + `is_terminal` |
| 6 | `reschedule_statuses` | Catálogo fijo | Estados de reprogramación |
| 7 | `users` | Identidad | Cuenta: datos personales, email/documento únicos, hash BCrypt |
| 8 | `user_roles` | Identidad | N:M usuario–rol |
| 9 | `refresh_tokens` | Identidad | Hash del refresh token, expiración, revocación, cadena de rotación |
| 10 | `password_reset_tokens` | Identidad | Hash del token de recuperación, expiración, uso único |
| 11 | `eps` | Configurable | EPS con activación |
| 12 | `eps_plans` | Configurable | Planes de cada EPS |
| 13 | `specialties` | Configurable | Especialidad, duración 30/60, marca general, activación |
| 14 | `user_affiliations` | Afiliación | Usuario + plan (→ EPS) + régimen |
| 15 | `professionals` | Profesionales | Subtipo de usuario: código, matrícula, activo |
| 16 | `professional_specialties` | Profesionales | N:M con marca primaria |
| 17 | `professional_sites` | Profesionales | N:M con sedes habilitadas |
| 18 | `availability_blocks` | Agenda | Bloque por profesional/sede/fecha-hora |
| 19 | `availability_slots` | Agenda | Slots de 30 min materializados |
| 20 | `appointments` | Citas | Cita: paciente, profesional, especialidad, sede, franja, estado actual |
| 21 | `reschedule_requests` | Citas | Solicitud de reprogramación con snapshots y decisión |
| 22 | `slot_reservations` | Citas | Ocupación exclusiva de slots (cita XOR solicitud) |
| 23 | `appointment_status_history` | Citas | Auditoría inmutable de estados |

---

## Supuestos

Decisiones tomadas donde el PRD o las HU no definen el comportamiento. **No son hechos**; cada uno puede revertirse con el impacto indicado.

| ID | Supuesto | Justificación | Impacto si cambia |
|---|---|---|---|
| S-01 | Tipos de documento como catálogo fijo con PK `code`: CC, CE, TI, RC, PA, PPT | HU-001 lo deja abierto; un catálogo evita textos divergentes y permite mostrar el nombre | Cambiar valores = migración de seed |
| S-02 | Regímenes sembrados: `CONTRIBUTIVO`, `SUBSIDIADO` | Ejemplo de HU-005 | Añadir `ESPECIAL`/`EXCEPCION` = INSERT |
| S-03 | Todas las DATETIME en hora civil America/Bogota (UTC-5, sin horario de verano); la conexión JDBC y la sesión MySQL fijan esa zona | La agenda se razona en hora local y los CHECK de alineación y "mismo día" dependen de ella | Pasar a UTC exige revisar `chk_blocks_same_day` |
| S-04 | La app normaliza el email (trim + minúsculas); la BD garantiza unicidad sin distinguir mayúsculas con la collation `utf8mb4_0900_as_ci` | HECHO de HU-001 + defensa en BD sin columna generada | — |
| S-05 | La app normaliza `document_number` (sin puntos ni espacios, en mayúsculas) antes de guardar | Evita que "1.234" y "1234" sean distintos | Sin normalización, la UK no detecta esos duplicados |
| S-06 | `users.is_active` controla el login; `professionals.is_active` controla si se puede reservar con él. Son independientes | HU-009 pregunta si un profesional inactivo puede iniciar sesión | Ver Q-07 |
| S-07 | Refresh y reset tokens: se guarda SHA-256 en hex (64 caracteres), no BCrypt | El token es aleatorio de alta entropía y se busca por igualdad; BCrypt tiene sal y no permite buscar por índice | — |
| S-08 | Un usuario puede tener **varias** afiliaciones, sin repetir (usuario, plan, régimen); el plan es obligatorio; el régimen no lo determina el plan | HU-004 prohíbe duplicados (implica multiplicidad) y deja abierto "una o varias" | Si es una sola: `UNIQUE(user_id)`. Si el plan determina el régimen: mover `regime_code` a `eps_plans` |
| S-09 | `specialties.is_general` marca "Medicina General" (máximo una); sembrada con 30 min | RF-11 y HU-006 suponen 30 min; derivar el tipo del *nombre* es frágil porque el nombre es editable | Quitar el índice funcional si hay varias generales |
| S-10 | `professional_specialties` y `professional_sites` tienen `is_active`; con historial se desactivan, no se borran | Las FK RESTRICT desde bloques y citas impiden borrar asignaciones usadas | — |
| S-11 | Los slots se materializan como filas al crear el bloque | Permiten una FK desde la reserva y una UNIQUE contra solapamientos; volumen bajo (~6 000 filas por profesional al año) | Calcularlos al vuelo obligaría a garantizar la no-doble-reserva sin FK al slot |
| S-12 | Un bloque no cruza la medianoche (`end_at` ≤ 00:00 del día siguiente) | RF-08 habla de bloques "por día" | Quitar `chk_blocks_same_day` |
| S-13 | Solo `CANCELLED` y `REJECTED` liberan slots; `COMPLETED` y `NO_SHOW` conservan la ocupación | HECHO RN-09 menciona solo cancelar o rechazar; conservarla protege los bloques pasados con citas | — |
| S-14 | Máximo una reprogramación `PENDING` por cita (restricción de BD) | Supuesto de HU-018 (CA-04) | Quitar el índice funcional |
| S-15 | Existe el estado de reprogramación `CANCELLED`: al cancelar una cita con una solicitud `PENDING`, la solicitud pasa a `CANCELLED` y se liberan ambas franjas | Supuesto de HU-017 | Quitar la semilla y el CHECK asociado |
| S-16 | El cierre de atención por PROFESSIONAL (RF-17) se registra con `source = 'USER'` y el actor = profesional | El PRD solo define SYSTEM/USER/ADMIN | Ver Q-01 |
| S-17 | Con fuente `USER` o `ADMIN`, `actor_user_id` es obligatorio; con `SYSTEM` es opcional | RF-19: "actor cuando existe" | Quitar `chk_ash_actor_required` |
| S-18 | Los nombres de catálogo (especialidad, sede, EPS, profesional) no se copian en citas; un renombrado se ve en el histórico | No hay historia clínica ni facturación; 3FN | Si se requiere congelarlos, añadir columnas snapshot declaradas |
| S-19 | `appointments.duration_minutes` es un snapshot de la especialidad en el momento de reservar | RF-09 + cambio de duración posible vía CRUD | — |
| S-20 | Una reprogramación puede cambiar de sede (conserva profesional y especialidad) | RF-15 solo exige conservar profesional y especialidad | Si se debe conservar la sede: CHECK `requested_site_code = original_site_code` |
| S-21 | Usuarios, citas y catálogos no se borran físicamente (FK RESTRICT); los tokens sí (CASCADE) | RF-06 y conservación del historial | — |
| S-22 | Una cita de 60 min usa dos slots **del mismo bloque** | Supuesto de HU-012 | Permitir bloques contiguos cambia solo la validación de la app |
| S-23 | El motivo de rechazo de la cita vive en `appointment_status_history.reason` (con CHECK) y no en `appointments` | Evita duplicarlo; RF-19 ya incluye el motivo | — |
| S-24 | Longitudes: nombres 100, email 254, teléfono 20, documento 30, motivos 500, código/matrícula 30 | Valores razonables; no están en el PRD | Ajustar tipos |
| S-25 | `sites.address` es un valor atómico (no se descompone en ciudad o departamento) | Ninguna HU filtra por partes de la dirección | Descomponer si se filtra por ciudad |
| S-26 | Una nueva solicitud de recuperación revoca (`revoked_at`) los tokens de reset vigentes del usuario | Reduce la superficie de ataque; HU-002 no lo define | — |

## Preguntas abiertas

Necesitan una decisión del usuario o Product Owner. Mientras no la haya, rige el supuesto indicado.

| ID | Pregunta | Supuesto vigente | Afecta a |
|---|---|---|---|
| Q-01 | ¿Se añade la fuente `PROFESSIONAL` al historial para el cierre de atención (RF-17), o `USER` + actor es suficiente? | S-16 | `chk_ash_source`, HU-020 |
| Q-02 | ¿Un USER puede tener varias afiliaciones simultáneas o solo una vigente? | S-08 | `user_affiliations`, HU-004 |
| Q-03 | Valores exactos de régimen: ¿solo contributivo y subsidiado, o también especial/excepción? | S-02 | Seed de `regimes` |
| ~~Q-04~~ | **Resuelta 2026-09-16 (D-008):** la lista CC, CE, TI, RC, PA, PPT es correcta | S-01 confirmado | Seed de `document_types`, HU-001 |
| Q-05 | ¿Caducan automáticamente las citas `REQUESTED` no resueltas y las reprogramaciones `PENDING` cuya fecha pasa? ¿Con qué estado (`EXPIRED`) y con fuente `SYSTEM`? | No hay expiración | Catálogos de estado, job programado, HU-014/HU-019 |
| Q-06 | Al cancelar una cita con reprogramación `PENDING`, ¿la solicitud pasa a `CANCELLED`? | S-15 | `reschedule_statuses`, HU-017 |
| Q-07 | ¿Un profesional inactivo puede iniciar sesión para ver su agenda histórica? | S-06 (sí, si `users.is_active`) | HU-009 |
| ~~Q-08~~ | **Resuelta 2026-09-16 (D-007):** America/Bogota confirmada como zona horaria de almacenamiento | S-03 confirmado | Todas las DATETIME, configuración JDBC |
| Q-09 | ¿Cómo se crea el primer ADMIN (sin contraseña en migraciones)? **Diferida (2026-09-16) a la sesión en que se trabaje HU-008.** | No se siembra ningún usuario | HU-008 |
| Q-10 | ¿Se mantienen los triggers de inmutabilidad del historial (pueden requerir `log_bin_trust_function_creators=1` con binlog) o se usa solo `GRANT SELECT, INSERT`? | Triggers incluidos como opcionales | `schema.sql` §7, entorno Docker |
| Q-11 | ¿La cita debe registrar con qué afiliación (EPS/plan) se atendió? | No se relaciona (S-18) | `appointments` |
| Q-12 | ¿Debe impedirse que un mismo USER tenga dos citas solapadas con profesionales distintos? | No se impide | Nueva restricción o validación en la app |
| Q-13 | ¿Puede un PROFESSIONAL o ADMIN reservar citas como paciente? | El modelo lo permite (roles múltiples) | Autorización |

---

## Subconjunto para la migración inicial de HU-001

HU-001 (única HU aprobada para S2, D-002) necesita registro, login, refresh con rotación y logout. **No se crea la migración aquí**; solo se identifica qué parte del modelo le corresponde, para que la futura `V1__…` sea coherente con este diseño.

### Tablas (en este orden de creación)

| Orden | Tabla | Motivo en HU-001 |
|---|---|---|
| 1 | `roles` | Seed fijo USER, PROFESSIONAL, ADMIN (T-01); el registro asigna `USER` |
| 2 | `document_types` | Registro con tipo de documento (S-01; HU-001 lo permite como "catálogo mínimo de autenticación") |
| 3 | `users` | Registro, email y documento únicos, hash BCrypt, `is_active` (CA-06 "registrado y activo") |
| 4 | `user_roles` | Roles en el contexto de autorización |
| 5 | `refresh_tokens` | Hash, expiración, revocación en logout y rotación (`replaced_by_token_id`) (D-003, CA-09…CA-11) |

### Seeds
- `roles`: USER, PROFESSIONAL, ADMIN.
- `document_types`: CC, CE, TI, RC, PA, PPT (confirmado, D-008).
- **Ningún usuario** (ni ADMIN) en la migración: ver Q-09.

### Fuera de la migración inicial (entran con su HU)
- `password_reset_tokens` → HU-002.
- `regimes`, `sites`, `appointment_statuses`, `reschedule_statuses` → HU-005 (HU-005 indica que los roles ya se sembraron en HU-001).
- `eps`, `eps_plans`, `user_affiliations` → HU-007 / HU-004.
- `specialties` (+ semilla "Medicina General") → HU-006.
- `professionals`, `professional_specialties`, `professional_sites` → HU-008.
- `availability_blocks`, `availability_slots` → HU-010.
- `appointments`, `slot_reservations`, `appointment_status_history` → HU-013; `reschedule_requests` → HU-018. Nota: `slot_reservations` y `appointment_status_history` tienen FK a `reschedule_requests`. Si se crean en HU-013, esas columnas y FK se añaden con `ALTER TABLE` en la migración de HU-018, o `reschedule_requests` se adelanta a HU-013.

### Puntos de atención para esa migración
- Copiar tal cual la collation `utf8mb4_0900_as_ci` de `users.email` y la UNIQUE compuesta del documento (CA-03, CA-04).
- `refresh_tokens.token_hash` como `CHAR(64) ascii_bin`; `replaced_by_token_id` con `ON DELETE SET NULL` y sin CHECK sobre esa columna.
- Validar primero en un MySQL 8.4 desechable. Este diseño no se ha ejecutado contra ninguna base de datos.
