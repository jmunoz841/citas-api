---
id: HU-004
tipo: historia-de-usuario
titulo: "Registrar afiliación EPS"
estado: Aprobada
epica: "[[EP-002-perfil-y-afiliacion]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 2"
dependencias:
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
relacionadas:
  - "[[HU-003-consultar-y-actualizar-perfil]]"
  - "[[HU-007-gestionar-eps-y-planes]]"
---

# HU-004 — Registrar afiliación EPS

## Historia de usuario

**COMO** visitante que se registra  
**QUIERO** seleccionar opcionalmente mi plan de EPS  
**PARA** que mi afiliación quede registrada desde el primer momento

> Como visitante que se registra, quiero seleccionar opcionalmente mi plan de EPS para que mi afiliación quede registrada desde el primer momento.

## Contexto y descripción

Implementa la segunda parte de RF-04. La afiliación referencia catálogos (plan, régimen) en lugar de duplicar nombres (3FN); la EPS se deriva del plan, así que un "plan de otra EPS" es imposible por estructura.

**Recorte de alcance para S3 (2026-09-23):** la afiliación se captura como paso **opcional** dentro del registro de USER, no como pantalla de perfil. Los planes se leen del catálogo sembrado; su CRUD administrativo sigue en HU-007 (S4). La afiliación **no interviene** en búsqueda, disponibilidad, precio, aprobación ni reserva: HU-013 guardará la afiliación vigente si existe y `null` si no. Por eso desaparece la dependencia hacia HU-003 y HU-007.

## Alcance

- Campo opcional `insurancePlanId` en el registro de USER.
- Validación de que el plan exista y esté activo.
- Creación de la afiliación inicial mediante clave foránea cuando se envía un plan.
- Registro sin plan: flujo idéntico al actual, sin afiliación.
- Endpoint de lectura de planes activos para alimentar el selector del frontend.

## Fuera de alcance

- CRUD administrativo de EPS y planes ([[HU-007-gestionar-eps-y-planes]]).
- Pantalla de perfil y edición posterior de la afiliación ([[HU-003-consultar-y-actualizar-perfil]]).
- Selección de régimen por el usuario en S3: se toma el régimen del plan sembrado.
- Validación contra sistemas reales de EPS.

## Reglas de negocio

- La afiliación referencia plan y régimen por clave; no se duplican sus nombres en la tabla de usuario.
- La EPS **no se almacena** en la afiliación: se deriva de `plan_id`.
- Solo se pueden seleccionar planes activos.
- Un plan inexistente o inactivo produce un error controlado y **no** crea el usuario.
- La afiliación es opcional: su ausencia nunca bloquea el registro ni ninguna regla de reserva.
- No se permiten afiliaciones duplicadas (misma combinación usuario/plan/régimen).

## Dependencias y relaciones

- Épica: [[EP-002-perfil-y-afiliacion]]
- Dependencias: [[HU-001-registro-e-inicio-de-sesion-jwt]]
- Relacionadas: [[HU-003-consultar-y-actualizar-perfil]], [[HU-007-gestionar-eps-y-planes]], [[HU-013-reservar-cita-general]]

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** toca el caso de uso de registro ya cerrado en HU-001, añade dos tablas de catálogo y una de afiliación, y exige que el fallo de validación no deje un usuario creado a medias.

## Tareas de desarrollo

- [ ] **T-01 — Migración de EPS, planes y afiliación**  
  Dificultad: Medio  
  Descripción: `eps`, `eps_plans` y `user_affiliations` según el diseño 3FN, con seed sintético de EPS y planes activos.
- [ ] **T-02 — Endpoint de planes activos**  
  Dificultad: Bajo  
  Descripción: lectura pública de planes activos con su EPS, para el selector del registro.
- [ ] **T-03 — Registro con afiliación opcional**  
  Dificultad: Medio  
  Descripción: `insurancePlanId` opcional en el comando de registro; validación de existencia y estado activo; creación de la afiliación en la misma transacción.
- [ ] **T-04 — Selector en el registro**  
  Dificultad: Medio  
  Descripción: campo opcional en el formulario de `citas-web`, alimentado por la API, con la opción de no elegir plan.
- [ ] **T-05 — Pruebas**  
  Dificultad: Medio  
  Descripción: registro sin plan, registro con plan activo, plan inexistente, plan inactivo; y pruebas de frontend del selector.

## Criterios de aceptación

### CA-01 — Registro sin plan

**Dado** un visitante con datos válidos que no selecciona plan  
**Cuando** envía el registro  
**Entonces** la cuenta se crea con rol `USER` y sin afiliación, con el mismo resultado que antes de esta HU

### CA-02 — Registro con plan activo

**Dado** un visitante que selecciona un plan activo  
**Cuando** envía el registro  
**Entonces** la cuenta se crea y queda asociada a una afiliación que referencia ese plan por clave foránea

### CA-03 — Plan inexistente o inactivo

**Dado** un identificador de plan que no existe o que está inactivo  
**Cuando** se envía el registro  
**Entonces** se responde con un error de validación identificable y **no** se crea ni el usuario ni la afiliación

### CA-04 — La EPS no se duplica

**Dado** un usuario afiliado  
**Cuando** se inspecciona su registro en base de datos  
**Entonces** ni la tabla de usuarios ni la de afiliaciones almacenan el nombre de la EPS o del plan: solo claves foráneas

### CA-05 — Catálogo de planes

**Dado** el catálogo sembrado  
**Cuando** el frontend solicita los planes  
**Entonces** recibe únicamente los planes activos, cada uno con su EPS, sin requerir autenticación

## Definition of Done

- [ ] Criterios CA-01 a CA-05 validados con evidencia.
- [ ] Migración Flyway de EPS, planes y afiliación presente y aplicada.
- [ ] Pruebas de backend en verde (`mvnw test`).
- [ ] Pruebas de frontend del selector en verde (`npm test`).
- [ ] Contrato REST del registro actualizado en `docs/contratos/`.
- [ ] Selector integrado en el registro de `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| CA-04 | Pendiente | — | — |
| CA-05 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.
- 2026-09-23 (S3) — Alcance recortado a "afiliación opcional durante el registro" y movida a Sprint 2; se eliminan las dependencias hacia HU-003 y HU-007.
- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.

## Notas y decisiones

- Resuelto: el diseño 3FN admite varias afiliaciones por usuario (`uk_user_affiliations_combo`), pero en S3 el registro solo crea la primera. La gestión de afiliaciones múltiples o vigentes se define en HU-003.
- La EPS se deriva de `plan_id`; el diseño 3FN evita por estructura que un plan quede asociado a otra EPS.
- El régimen se toma del plan sembrado. Permitir que el usuario elija régimen queda para HU-003.
