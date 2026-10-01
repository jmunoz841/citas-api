---
id: HU-003
tipo: historia-de-usuario
titulo: "Consultar y actualizar perfil"
estado: Completada
epica: "[[EP-002-perfil-y-afiliacion]]"
esfuerzo: "Bajo"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
relacionadas:
  - "[[HU-004-registrar-afiliacion]]"
---

# HU-003 — Consultar y actualizar perfil

## Historia de usuario

**COMO** USER autenticado  
**QUIERO** consultar y actualizar los datos permitidos de mi perfil  
**PARA** mantener mi información de contacto al día

> Como USER autenticado, quiero consultar y actualizar los datos permitidos de mi perfil para mantener mi información de contacto al día.

## Contexto y descripción

Implementa la primera parte de RF-04. Incluye la vista home/dashboard USER como punto de entrada.

## Alcance

- Consulta del propio perfil.
- Actualización de datos permitidos (p. ej. nombres, apellidos, teléfono).
- Home/dashboard USER.

## Fuera de alcance

- Cambio de email o documento (supuesto: no editables).
- Afiliación ([[HU-004-registrar-afiliacion]]).

## Reglas de negocio

- Un usuario solo puede consultar y modificar su propio perfil (ownership).
- Validación server-side de los campos editables.

## Dependencias y relaciones

- Épica: [[EP-002-perfil-y-afiliacion]]
- Dependencias: [[HU-001-registro-e-inicio-de-sesion-jwt]]
- Relacionadas: [[HU-004-registrar-afiliacion]]

## Esfuerzo

**Nivel:** Bajo

**Justificación de dificultad:** operación de lectura/actualización sobre una entidad existente con control de ownership.

## Tareas de desarrollo

- [x] **T-01 — Casos de uso consultar/actualizar perfil**
  Dificultad: Bajo  
  Descripción: lectura del usuario autenticado y actualización de campos permitidos.
- [x] **T-02 — Endpoints protegidos**
  Dificultad: Bajo  
  Descripción: identificación del usuario desde el token, sin recibir su id como parámetro manipulable.
- [x] **T-03 — Vistas home USER y perfil**
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [x] **T-04 — Pruebas**
  Dificultad: Bajo  
  Descripción: actualización válida, inválida y acceso sin autenticación.

## Criterios de aceptación

### CA-01 — Consulta del perfil propio

**Dado** un USER autenticado  
**Cuando** consulta su perfil  
**Entonces** obtiene sus datos sin información sensible (hash de contraseña)

### CA-02 — Actualización válida

**Dado** un USER autenticado  
**Cuando** actualiza campos permitidos con valores válidos  
**Entonces** los cambios quedan persistidos

### CA-03 — Campos no editables

**Dado** un USER autenticado  
**Cuando** intenta modificar email, documento o roles  
**Entonces** esos campos no cambian

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Pruebas de backend en verde.
- [ ] Vistas de home y perfil integradas en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `AuthController.profile`, `OwnProfileService.view`, `ProfileResponse`; `ProfilePage` carga `GET /api/v1/auth/profile` | El id procede de `AuthenticatedUser` y la respuesta no expone hash de contraseña. |
| CA-02 | Cumple | `AuthController.updateProfile`, `OwnProfileService.update`, `User.withProfile`; `AuthServiceTest#hu003_elUserConsultaYActualizaSoloLosCamposPermitidosDeSuPerfil` | Persiste nombres, apellidos y teléfono. Prueba manual del Product Owner el 2026-09-30: satisfactoria. |
| CA-03 | Cumple | `UpdateProfileRequest` solo contiene `firstNames`, `lastNames` y `phone`; `User.withProfile` conserva documento, correo y roles; `ProfilePage` deshabilita correo y documento | El test específico comprueba que correo y documento no cambian. Los roles no forman parte del contrato de actualización. |
| DoD — CA obligatorios | Cumple | Filas CA-01 a CA-03 de esta matriz | Todos los criterios tienen evidencia concreta. |
| DoD — Pruebas backend | Cumple | `target/surefire-reports/TEST-*.xml`: 172 pruebas, 0 errores y 0 fallos; incluye `AuthServiceTest` (16 pruebas) | Resultado disponible tras la validación de S4. |
| DoD — Vistas integradas | Cumple | `src/App.tsx`, `src/shared/layout/AppShell.tsx`, `src/features/auth/pages/ProfilePage.tsx`, `PatientHomePage.tsx` | Ruta USER `/perfil`, entrada de navegación y dashboard USER existentes. |
| DoD — Trazabilidad | Cumple | Esta HU y `EP-002-perfil-y-afiliacion.md` | Estado y evidencia actualizados para Obsidian. |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-30 (S4) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para implementar perfil propio con email, documento y roles no editables.

- 2026-09-30 (S4) — HU pasa a `En validación`. El Product Owner realizó la prueba manual del perfil y confirmó que funcionó. La matriz recoge además evidencia de código, contrato y pruebas.

- 2026-09-30 (S4) — HU `Completada` con confirmación explícita del Product Owner. Todos los criterios de aceptación y elementos aplicables de la DoD figuran como `Cumple` en la matriz.

## Notas y decisiones

- Supuesto: email y documento no son editables por el USER.
