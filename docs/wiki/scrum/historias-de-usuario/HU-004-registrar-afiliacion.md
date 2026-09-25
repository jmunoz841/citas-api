---
id: HU-004
tipo: historia-de-usuario
titulo: "Registrar afiliación EPS"
estado: Completada
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

- Campos opcionales `insurancePlanId` y `regimeCode` en el registro de USER, que van **en pareja**: enviar uno sin el otro es un error de validación.
- Validación de que el plan exista y esté activo, y de que el régimen exista.
- Creación de la afiliación inicial mediante claves foráneas cuando se envían ambos.
- Registro sin afiliación: flujo idéntico al actual.
- Endpoint de lectura de planes activos para alimentar el selector del frontend.

## Fuera de alcance

- CRUD administrativo de EPS y planes ([[HU-007-gestionar-eps-y-planes]]).
- Pantalla de perfil y edición posterior de la afiliación ([[HU-003-consultar-y-actualizar-perfil]]).
- Afiliaciones múltiples por usuario.
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

- [x] **T-01 — Migración de EPS, planes y afiliación**  
  Dificultad: Medio  
  Descripción: `eps`, `eps_plans` y `user_affiliations` según el diseño 3FN, con seed sintético de EPS y planes activos.
- [x] **T-02 — Endpoint de planes activos**  
  Dificultad: Bajo  
  Descripción: lectura pública de planes activos con su EPS, para el selector del registro.
- [x] **T-03 — Registro con afiliación opcional**  
  Dificultad: Medio  
  Descripción: `insurancePlanId` opcional en el comando de registro; validación de existencia y estado activo; creación de la afiliación en la misma transacción.
- [x] **T-04 — Selector en el registro**  
  Dificultad: Medio  
  Descripción: campo opcional en el formulario de `citas-web`, alimentado por la API, con la opción de no elegir plan.
- [x] **T-05 — Pruebas**  
  Dificultad: Medio  
  Descripción: registro sin plan, registro con plan activo, plan inexistente, plan inactivo; y pruebas de frontend del selector.

## Criterios de aceptación

### CA-01 — Registro sin plan

**Dado** un visitante con datos válidos que no selecciona plan  
**Cuando** envía el registro  
**Entonces** la cuenta se crea con rol `USER` y sin afiliación, con el mismo resultado que antes de esta HU

### CA-02 — Registro con plan activo

**Dado** un visitante que selecciona un plan activo y su régimen  
**Cuando** envía el registro  
**Entonces** la cuenta se crea y queda asociada a una afiliación que referencia ese plan y ese régimen por clave foránea

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

### CA-06 — Plan y régimen van en pareja

**Dado** un registro que envía solo el plan o solo el régimen  
**Cuando** se procesa la solicitud  
**Entonces** se responde con un error de validación que señala el campo que falta, sin crear usuario ni afiliación

## Definition of Done

- [x] Criterios CA-01 a CA-06 validados con evidencia.
- [x] Migración Flyway de EPS, planes y afiliación presente y aplicada.
- [x] Pruebas de backend en verde (`mvnw test`): 78/78.
- [x] Pruebas de frontend del selector en verde (`npm test`): 22/22.
- [x] Contrato REST del registro actualizado en `docs/contratos/`.
- [x] Selector integrado en el registro de `citas-web`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Registro sin plan | Cumple | `RegisterAffiliationApiIntegrationTest.ca01_registroSinAfiliacionCreaLaCuentaYNingunaAfiliacion`; `AuthServiceTest.ca01_registroSinAfiliacionNoCreaNingunaFila`; `RegisterPage.affiliation.test.tsx` "permite registrarse sin elegir plan" | El cuerpo enviado no incluye los campos de afiliación |
| CA-02 Registro con plan activo | Cumple | `ca02_registroConPlanYRegimenCreaLaAfiliacionPorClaveForanea` (lee `user_affiliations` en BD); `AuthServiceTest.ca02_...`; prueba de frontend "envia plan y regimen" | La fila referencia plan y régimen por FK |
| CA-03 Plan inexistente o inactivo | Cumple | `ca03_unPlanInexistenteDevuelve400YNoCreaUsuario`, `ca03_unPlanInactivoNoEsSeleccionable`, `ca03_unPlanDeUnaEpsInactivaTampocoEsSeleccionable`, `ca03_unRegimenInexistenteDevuelve400` | 400 `VALIDATION_ERROR` con el campo; el usuario no se crea |
| CA-04 La EPS no se duplica | Cumple | `ca04_laAfiliacionNoGuardaNombresDeEpsNiDePlan` | `user_affiliations` solo tiene `user_id`, `plan_id`, `regime_code` y marcas de tiempo |
| CA-05 Catálogo de planes | Cumple | `ca05_elCatalogoSoloOfreceLosPlanesSeleccionables`; humo manual 2026-09-23: 4 de 6 planes sembrados | Excluye el plan inactivo y el de EPS inactiva; sin autenticación |
| CA-06 Plan y régimen en pareja | Cumple | `ca06_soloElPlanOSoloElRegimenDevuelve400SenalandoElCampoQueFalta`; `AuthServiceTest.ca06_...`; prueba de frontend "exige el regimen si se eligio plan" | El cliente refleja la regla y no llega a llamar a la API |
| DoD Migración | Cumple | `V3__afiliacion_hu004.sql`; `flyway_schema_history` v3 `success=1` en `jmunoz-citas-mysql` | Seed sintético con un plan inactivo y una EPS inactiva a propósito |
| DoD Pruebas backend | Cumple | `mvnw test` 2026-09-23: 78 pruebas, 0 fallos | 9 de integración + 5 unitarias nuevas |
| DoD Pruebas frontend | Cumple | `npm test` 2026-09-23: 22 pruebas, 0 fallos | 7 nuevas del formulario de registro |
| DoD Contrato | Cumple | `docs/contratos/autenticacion.md` (registro) y `docs/contratos/catalogos.md` (planes) | — |
| DoD Selector en `citas-web` | Cumple | Sección "Afiliación (opcional)" en `RegisterPage.tsx` | Régimen deshabilitado hasta elegir plan; si el catálogo falla, la sección no se ofrece |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.
- 2026-09-23 (S3) — Alcance recortado a "afiliación opcional durante el registro" y movida a Sprint 2; se eliminan las dependencias hacia HU-003 y HU-007.
- 2026-09-23 (S3) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para el alcance de S3.
- 2026-09-23 (S3) — El PO decide que el registro pida `insurancePlanId` y `regimeCode` en pareja; se añade CA-06 y se corrige la nota errónea sobre el régimen.
- 2026-09-23 (S3) — HU implementada y verificada de punta a punta: migración V3, endpoint de planes, registro con afiliación opcional, selector en `citas-web`. `mvnw test` 78/78 y `npm test` 22/22.

- 2026-09-25 (S3) — HU `Completada` con confirmación explícita del Product Owner (Juan Muñoz): todos los CA y la DoD en `Cumple` con evidencia. Resumen en [[evidencia-s3]].

## Notas y decisiones

- Resuelto: el diseño 3FN admite varias afiliaciones por usuario (`uk_user_affiliations_combo`), pero en S3 el registro solo crea la primera. La gestión de afiliaciones múltiples o vigentes se define en HU-003.
- La EPS se deriva de `plan_id`; el diseño 3FN evita por estructura que un plan quede asociado a otra EPS.
- **Régimen (decisión del PO, 2026-09-23):** `user_affiliations.regime_code` es obligatorio y el plan **no** lo determina —en Colombia el régimen depende de la situación del afiliado, no del plan, y una misma EPS opera en ambos—. Por eso el registro pide `insurancePlanId` y `regimeCode` juntos. Se descartó fijar `CONTRIBUTIVO` por defecto (guardaría un dato que el usuario nunca confirmó) y mover el régimen a `eps_plans` (modelaría mal la realidad y se apartaría del diseño 3FN aprobado).
- Una nota anterior de esta HU afirmaba que el régimen se tomaba del plan; era incorrecta y queda corregida aquí.
