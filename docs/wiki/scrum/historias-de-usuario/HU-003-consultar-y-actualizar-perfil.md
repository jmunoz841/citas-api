---
id: HU-003
tipo: historia-de-usuario
titulo: "Consultar y actualizar perfil"
estado: Borrador
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

- [ ] **T-01 — Casos de uso consultar/actualizar perfil**  
  Dificultad: Bajo  
  Descripción: lectura del usuario autenticado y actualización de campos permitidos.
- [ ] **T-02 — Endpoints protegidos**  
  Dificultad: Bajo  
  Descripción: identificación del usuario desde el token, sin recibir su id como parámetro manipulable.
- [ ] **T-03 — Vistas home USER y perfil**  
  Dificultad: Medio  
  Descripción: según diseño aprobado.
- [ ] **T-04 — Pruebas**  
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
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

## Notas y decisiones

- Supuesto: email y documento no son editables por el USER.
