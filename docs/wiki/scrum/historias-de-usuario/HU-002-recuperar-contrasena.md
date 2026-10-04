---
id: HU-002
tipo: historia-de-usuario
titulo: "Recuperar contraseña"
estado: Completada
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

- [x] **T-01 — Migración de tokens de recuperación**
  Dificultad: Bajo  
  Descripción: almacenamiento del token (hash), expiración y marca de uso.
- [x] **T-02 — Casos de uso de solicitud y cambio**
  Dificultad: Medio  
  Descripción: generación, validación, consumo del token y actualización de contraseña.
- [x] **T-03 — Entrega del token sin exposición insegura**
  Dificultad: Bajo  
  Descripción: no se registra ni retorna el valor claro; SMTP permanece fuera de alcance y el formulario admite el código recibido por un canal seguro.
- [x] **T-04 — Vistas frontend de recuperación y cambio**
  Dificultad: Medio  
  Descripción: formularios según diseño aprobado, integrados con la API.
- [x] **T-05 — Pruebas**
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

- [x] Todos los criterios de aceptación obligatorios están validados con evidencia.
- [x] Migración Flyway del token de recuperación presente.
- [x] Pruebas de backend de los casos CA-01 a CA-04 en verde.
- [x] Vistas de recuperación/cambio de contraseña funcionales en `citas-web`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `PasswordRecoveryApiIntegrationTest#ca01_laSolicitudTieneLaMismaRespuestaConEmailExistenteOInexistente`; prueba manual `204` el 2026-09-30 | La respuesta no revela la existencia de la cuenta. |
| CA-02 | Cumple | `PasswordRecoveryApiIntegrationTest#ca02_conTokenVigenteCambiaLaContrasenaYConsumeElToken` | Cambia el hash BCrypt, consume el token y permite el nuevo inicio de sesión. |
| CA-03 | Cumple | `PasswordRecoveryApiIntegrationTest#ca03_unTokenYaUsadoNoSePuedeReutilizar` | Un segundo uso recibe `INVALID_PASSWORD_RESET_TOKEN`. |
| CA-04 | Cumple | `PasswordRecoveryApiIntegrationTest#ca04_tokenExpiradoOInexistenteNoModificaLaCuenta` | Rechaza token vencido o inexistente y conserva la contraseña anterior. |
| DoD | Cumple | Flyway `V8__password_reset_tokens_hu002.sql`; backend 171/171; frontend lint, 99 pruebas, typecheck y build; `PasswordRecoveryPage`; `docs/contratos/autenticacion.md` | Token hash-only de 30 minutos, sin exponer su valor claro; SMTP no es obligatorio. |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Borrador`.

- 2026-09-30 (S4) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz) para implementar RF-03 con token de un solo uso de 30 minutos (D-032).

- 2026-09-30 (S4) — Evidencia CA/DoD verificada y cierre `Completada` aprobado explícitamente por el Product Owner.

## Notas y decisiones

- Resuelto (D-032): el token de recuperación dura 30 minutos, es de un solo uso y se guarda como hash.
