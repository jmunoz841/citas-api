---
id: HU-002
tipo: historia-de-usuario
titulo: "Recuperar contraseña"
estado: Borrador
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: "Medio"
sprint_sugerido: "Sprint 3"
dependencias:
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
relacionadas: []
---

# HU-002 — Recuperar contraseña

## Historia de usuario

**COMO** usuario registrado que olvidó su contraseña  
**QUIERO** solicitar la recuperación por email y definir una nueva contraseña  
**PARA** recuperar el acceso a mi cuenta sin intervención administrativa

> Como usuario registrado que olvidó su contraseña, quiero solicitar la recuperación por email y definir una nueva contraseña para recuperar el acceso a mi cuenta sin intervención administrativa.

## Contexto y descripción

Implementa RF-03. El envío real de correo es opcional; en desarrollo el token puede exponerse de forma controlada (log o respuesta habilitada solo en perfil de desarrollo).

## Alcance

- Solicitud de recuperación por email.
- Token temporal y de un solo uso.
- Cambio de contraseña con token válido.
- Pantalla de recuperación/cambio de contraseña.

## Fuera de alcance

- SMTP obligatorio.
- Cambio de contraseña autenticado desde perfil.

## Reglas de negocio

- La solicitud no revela si el email existe.
- El token expira y solo puede usarse una vez.
- Cambiar la contraseña consume/invalida el token.
- La nueva contraseña se almacena con hash BCrypt.
- La exposición del token fuera de correo solo se permite en entorno de desarrollo.

## Dependencias y relaciones

- Épica: [[EP-001-identidad-y-acceso]]
- Dependencias: [[HU-001-registro-e-inicio-de-sesion-jwt]]
- Relacionadas: ninguna

## Esfuerzo

**Nivel:** Medio

**Justificación de dificultad:** requiere persistencia y ciclo de vida de un token de un solo uso y cuidado para no filtrar información de cuentas.

## Tareas de desarrollo

- [ ] **T-01 — Migración de tokens de recuperación**  
  Dificultad: Bajo  
  Descripción: almacenamiento del token (hash), expiración y marca de uso.
- [ ] **T-02 — Casos de uso de solicitud y cambio**  
  Dificultad: Medio  
  Descripción: generación, validación, consumo del token y actualización de contraseña.
- [ ] **T-03 — Adaptador de entrega del token**  
  Dificultad: Bajo  
  Descripción: exposición controlada en desarrollo; puerto preparado para correo.
- [ ] **T-04 — Vistas frontend de recuperación y cambio**  
  Dificultad: Medio  
  Descripción: formularios según diseño aprobado, integrados con la API.
- [ ] **T-05 — Pruebas**  
  Dificultad: Medio  
  Descripción: token válido, expirado, reutilizado e inexistente.

## Criterios de aceptación

### CA-01 — Solicitud sin revelar existencia

**Dado** cualquier email  
**Cuando** se solicita recuperación  
**Entonces** la respuesta es la misma exista o no la cuenta

### CA-02 — Cambio con token válido

**Dado** un token vigente y no usado  
**Cuando** se envía una nueva contraseña válida  
**Entonces** la contraseña cambia y el usuario puede iniciar sesión con ella

### CA-03 — Token de un solo uso

**Dado** un token ya utilizado  
**Cuando** se intenta usar nuevamente  
**Entonces** la operación es rechazada

### CA-04 — Token expirado o inválido

**Dado** un token expirado o inexistente  
**Cuando** se intenta cambiar la contraseña  
**Entonces** la operación es rechazada sin modificar la cuenta

## Definition of Done

- [ ] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [ ] Migración Flyway del token de recuperación presente.
- [ ] Pruebas de backend de los casos CA-01 a CA-04 en verde.
- [ ] Vistas de recuperación/cambio de contraseña funcionales en `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| CA-04 | Pendiente | — | — |
| DoD | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

## Notas y decisiones

- Resuelto (D-032): el token de recuperación dura 30 minutos, es de un solo uso y se guarda como hash.
