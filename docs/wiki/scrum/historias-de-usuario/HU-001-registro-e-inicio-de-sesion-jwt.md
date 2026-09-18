---
id: HU-001
tipo: historia-de-usuario
titulo: "Registro e inicio de sesión con sesión JWT"
estado: Aprobada
epica: "[[EP-001-identidad-y-acceso]]"
esfuerzo: "Alto"
sprint_sugerido: "Sprint 1"
dependencias: []
relacionadas:
  - "[[HU-002-recuperar-contrasena]]"
  - "[[HU-003-consultar-y-actualizar-perfil]]"
---

# HU-001 — Registro e inicio de sesión con sesión JWT

## Historia de usuario

**COMO** visitante  
**QUIERO** registrarme como usuario e iniciar sesión  
**PARA** acceder al sistema con una sesión JWT segura

> Como visitante, quiero registrarme e iniciar sesión para acceder al sistema con una sesión JWT segura.

## Contexto y descripción

Primer incremento funcional del producto (RF-01 y RF-02 del PRD). Establece la base de identidad sobre la que dependen todas las demás historias: usuarios, roles, hash de contraseñas y sesión con access/refresh token separados.

Se mantiene como una única HU compuesta por decisión explícita del plan S2: registro y login se entregan juntos porque el login sin registro no es demostrable y el registro sin sesión no aporta acceso. Se acota a backend; las vistas de login/registro se diseñan en Stitch dentro de la misma sesión, pero su integración REST no es obligatoria para cerrar esta HU.

## Alcance

- Registro público de una cuenta con rol `USER`.
- Datos mínimos: nombres, apellidos, tipo de documento, número de documento, email, teléfono y contraseña.
- Unicidad de email y de documento (tipo + número).
- Almacenamiento de la contraseña con hash adaptativo BCrypt.
- Login por email + contraseña.
- Emisión de access token de corta duración y refresh token diferenciados.
- Refresh de sesión a partir de un refresh token válido.
- Logout con revocación del refresh token.
- Roles del usuario incluidos en el contexto de autorización.
- Validación server-side y respuestas de error consistentes.
- Migraciones Flyway para usuarios, roles, relación usuario-rol, refresh tokens y catálogos mínimos de autenticación (roles y tipos de documento, si se modelan como catálogo).
- Pruebas automatizadas de backend.

## Fuera de alcance

- Recuperación de contraseña ([[HU-002-recuperar-contrasena]]).
- Consulta/actualización de perfil y afiliación ([[HU-003-consultar-y-actualizar-perfil]], [[HU-004-registrar-afiliacion]]).
- Creación de `PROFESSIONAL` o `ADMIN` (los crea ADMIN o seed).
- Verificación de email por correo.
- Integración REST completa del frontend (opcional en S2).

## Reglas de negocio

- El registro público solo crea usuarios con rol `USER`; el cliente no puede elegir el rol.
- Email único (comparación insensible a mayúsculas/minúsculas).
- Documento único por tipo + número.
- La contraseña nunca se almacena ni se registra en logs en texto plano.
- El access token y el refresh token son distintos y no intercambiables: un access token no sirve para refrescar y un refresh token no autoriza recursos.
- Un refresh token revocado o expirado no permite obtener nuevos tokens.
- Rotación: cada refresh exitoso emite un nuevo refresh token y revoca el anterior.
- Los refresh tokens se almacenan en BD como hash, nunca en texto plano.
- Política de contraseña: mínimo 8 caracteres, con al menos una letra y al menos un número.
- El login fallido no revela si el email existe.
- Secretos JWT y credenciales de BD solo por variables de entorno.
- No se registran tokens ni contraseñas en logs.

## Dependencias y relaciones

- Épica: [[EP-001-identidad-y-acceso]]
- Dependencias: ninguna (HU fundacional).
- Relacionadas: [[HU-002-recuperar-contrasena]], [[HU-003-consultar-y-actualizar-perfil]]

## Esfuerzo

**Nivel:** Alto

**Justificación de dificultad:** cruza todas las capas del backend (dominio, aplicación, adaptadores web/persistencia/seguridad), requiere la inicialización de Spring Security con JWT, el esquema inicial con Flyway y el ciclo de vida del refresh token (emisión, rotación/validación, revocación).

## Tareas de desarrollo

- [x] **T-01 — Esquema inicial de identidad** (2026-09-18: `V1__identidad_hu001.sql` aplicada)  
  Dificultad: Medio  
  Descripción: migración Flyway con usuarios, roles, relación usuario-rol, refresh tokens y seed de roles fijos (`USER`, `PROFESSIONAL`, `ADMIN`); restricciones únicas de email y documento. Debe ser coherente con el diseño 3FN propio.
- [ ] **T-02 — Modelo de dominio de usuario**  
  Dificultad: Medio  
  Descripción: entidad/valores de dominio (usuario, email, documento, roles) y puertos de repositorio, sin dependencias de Spring.
- [ ] **T-03 — Caso de uso de registro**  
  Dificultad: Medio  
  Descripción: validación de datos, comprobación de unicidad, hash BCrypt y asignación del rol `USER`.
- [ ] **T-04 — Caso de uso de login y emisión de tokens**  
  Dificultad: Alto  
  Descripción: autenticación por email/contraseña, generación de access token con roles y refresh token persistido de forma segura.
- [ ] **T-05 — Refresh y logout**  
  Dificultad: Alto  
  Descripción: validar refresh token (existencia, expiración, revocación, tipo), emitir nuevos tokens y revocar en logout.
- [ ] **T-06 — Adaptadores REST y seguridad**  
  Dificultad: Medio  
  Descripción: endpoints públicos de autenticación, filtro JWT, CORS explícito, rutas protegidas por defecto y manejo global de errores con formato uniforme.
- [ ] **T-07 — Configuración por entorno**  
  Dificultad: Bajo  
  Descripción: conexión MySQL, secretos y duraciones JWT por variables de entorno; `.env.example` actualizado sin secretos reales.
- [ ] **T-08 — Pruebas de backend**  
  Dificultad: Medio  
  Descripción: registro exitoso, email duplicado, documento duplicado, datos inválidos, login correcto, credenciales inválidas, refresh válido, refresh inválido/revocado y logout.
- [ ] **T-09 — Contrato de autenticación documentado**  
  Dificultad: Bajo  
  Descripción: documentar requests, responses y códigos de error de autenticación para consumo de `citas-web`.

## Criterios de aceptación

### CA-01 — Registro exitoso

**Dado** un visitante con datos válidos y un email y documento no registrados  
**Cuando** envía la solicitud de registro  
**Entonces** se crea la cuenta con rol `USER`, la respuesta indica éxito sin exponer la contraseña ni su hash

### CA-02 — Contraseña almacenada con hash

**Dado** un usuario registrado  
**Cuando** se inspecciona su registro en base de datos  
**Entonces** la contraseña está almacenada como hash BCrypt y no en texto plano

### CA-03 — Email duplicado

**Dado** un email ya registrado (en cualquier combinación de mayúsculas/minúsculas)  
**Cuando** un visitante intenta registrarse con ese email  
**Entonces** el registro es rechazado con un error de conflicto identificable y no se crea ningún usuario

### CA-04 — Documento duplicado

**Dado** un tipo y número de documento ya registrados  
**Cuando** un visitante intenta registrarse con ese documento  
**Entonces** el registro es rechazado con un error de conflicto identificable y no se crea ningún usuario

### CA-05 — Validación de datos de registro

**Dado** un registro con campos obligatorios ausentes, email con formato inválido o contraseña con menos de 8 caracteres, sin letras o sin números  
**Cuando** se envía la solicitud  
**Entonces** se responde con error de validación que indica los campos inválidos, sin crear usuario

### CA-06 — Login correcto

**Dado** un usuario registrado y activo  
**Cuando** inicia sesión con email y contraseña correctos  
**Entonces** recibe un access token y un refresh token distintos, y el access token incluye sus roles

### CA-07 — Credenciales inválidas

**Dado** un email inexistente o una contraseña incorrecta  
**Cuando** se intenta iniciar sesión  
**Entonces** se responde con error de no autenticado, con el mismo mensaje en ambos casos y sin emitir tokens

### CA-08 — Acceso protegido con access token

**Dado** un recurso protegido  
**Cuando** se invoca sin token, con token inválido/expirado o con un refresh token en lugar del access token  
**Entonces** el acceso es rechazado; y con un access token válido es permitido

### CA-09 — Refresh de sesión

**Dado** un refresh token válido y no revocado  
**Cuando** se solicita refrescar la sesión  
**Entonces** se emiten un nuevo access token y un nuevo refresh token, y el refresh token usado queda revocado (un segundo uso es rechazado)

### CA-10 — Refresh inválido

**Dado** un refresh token inexistente, expirado, revocado o un access token usado como refresh  
**Cuando** se solicita refrescar la sesión  
**Entonces** se responde con error de no autenticado y no se emiten tokens

### CA-11 — Logout y revocación

**Dado** un usuario con sesión activa  
**Cuando** hace logout con su refresh token  
**Entonces** ese refresh token queda revocado y un intento posterior de refresh con él es rechazado

## Definition of Done

- [ ] Todos los criterios de aceptación CA-01 a CA-11 están validados con evidencia.
- [ ] Proyecto Spring Boot 3.5.x / Java 21 / Maven con paquetes hexagonales (`domain`, `application`, `infrastructure/adapters`) observable en `citas-api`.
- [ ] Migración Flyway de identidad presente y aplicada correctamente contra MySQL 8.4.
- [ ] Pruebas de registro, login correcto, credenciales inválidas, email/documento duplicados y refresh inválido existen y `mvn test` termina en verde.
- [ ] Ningún secreto real versionado; `.env.example` documenta las variables necesarias.
- [ ] No se registran contraseñas ni tokens en logs.
- [ ] Contrato REST de autenticación documentado para `citas-web`.
- [ ] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 | Pendiente | — | — |
| CA-04 | Pendiente | — | — |
| CA-05 | Pendiente | — | — |
| CA-06 | Pendiente | — | — |
| CA-07 | Pendiente | — | — |
| CA-08 | Pendiente | — | — |
| CA-09 | Pendiente | — | — |
| CA-10 | Pendiente | — | — |
| CA-11 | Pendiente | — | — |
| DoD-01 Arquitectura hexagonal | Pendiente | — | — |
| DoD-02 Flyway | Pendiente | — | — |
| DoD-03 `mvn test` | Pendiente | — | — |
| DoD-04 Secretos | Pendiente | — | — |
| DoD-05 Logs | Pendiente | — | — |
| DoD-06 Contrato REST | Pendiente | — | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Pendiente de aprobación`.
- 2026-09-16 (S2) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz).

## Notas y decisiones

- Decisión aprobada (2026-09-16): rotación del refresh token en cada refresh y almacenamiento del refresh token como hash en BD.
- Decisión aprobada (2026-09-16): política de contraseña fija — mínimo 8 caracteres, al menos una letra y al menos un número.
- Supuesto: tipos de documento como catálogo fijo o enumeración; se resolverá en la normalización 3FN propia.
