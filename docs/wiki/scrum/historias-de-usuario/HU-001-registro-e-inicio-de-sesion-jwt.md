---
id: HU-001
tipo: historia-de-usuario
titulo: "Registro e inicio de sesión con sesión JWT"
estado: Completada
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
- [x] **T-02 — Modelo de dominio de usuario**  
  Dificultad: Medio  
  Descripción: entidad/valores de dominio (usuario, email, documento, roles) y puertos de repositorio, sin dependencias de Spring.
- [x] **T-03 — Caso de uso de registro**  
  Dificultad: Medio  
  Descripción: validación de datos, comprobación de unicidad, hash BCrypt y asignación del rol `USER`.
- [x] **T-04 — Caso de uso de login y emisión de tokens**  
  Dificultad: Alto  
  Descripción: autenticación por email/contraseña, generación de access token con roles y refresh token persistido de forma segura.
- [x] **T-05 — Refresh y logout**  
  Dificultad: Alto  
  Descripción: validar refresh token (existencia, expiración, revocación, tipo), emitir nuevos tokens y revocar en logout.
- [x] **T-06 — Adaptadores REST y seguridad**  
  Dificultad: Medio  
  Descripción: endpoints públicos de autenticación, filtro JWT, CORS explícito, rutas protegidas por defecto y manejo global de errores con formato uniforme.
- [x] **T-07 — Configuración por entorno**  
  Dificultad: Bajo  
  Descripción: conexión MySQL, secretos y duraciones JWT por variables de entorno en un `.env` local no versionado; variables documentadas en `README.md` (D-017).
- [x] **T-08 — Pruebas de backend** (2026-09-18: 54 pruebas, `mvn test` en verde)  
  Dificultad: Medio  
  Descripción: registro exitoso, email duplicado, documento duplicado, datos inválidos, login correcto, credenciales inválidas, refresh válido, refresh inválido/revocado y logout.
- [x] **T-09 — Contrato de autenticación documentado**  
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

- [x] Todos los criterios de aceptación CA-01 a CA-11 están validados con evidencia.
- [x] Proyecto Spring Boot 3.5.x / Java 21 / Maven con paquetes hexagonales (`domain`, `application`, `infrastructure/adapters`) observable en `citas-api`.
- [x] Migración Flyway de identidad presente y aplicada correctamente contra MySQL 8.4.
- [x] Pruebas de registro, login correcto, credenciales inválidas, email/documento duplicados y refresh inválido existen y `mvn test` termina en verde.
- [x] Ningún secreto real versionado; `README.md` documenta las variables necesarias del `.env` local.
- [x] No se registran contraseñas ni tokens en logs.
- [x] Contrato REST de autenticación documentado para `citas-web`.
- [x] La trazabilidad de esta HU y su épica está actualizada en `docs/wiki/scrum/`.

## Evidencia de validación

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
Pruebas en `src/test/java/com/citas/api/`; `AuthApiIntegrationTest` = HTTP → casos de uso → JPA → MySQL 8.4 (Testcontainers) con Flyway V1.

| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 Registro exitoso | Cumple | `AuthApiIntegrationTest.ca01_registroExitosoCreaUserSinExponerLaContrasena`, `registroNoAceptaRolDesdeElCliente`; `AuthServiceTest.registraUserConHashYSinGuardarLaContrasena` | Rol siempre `USER`; la respuesta no incluye contraseña ni hash |
| CA-02 Hash BCrypt | Cumple | `ca02_laContrasenaSeGuardaComoHashBCrypt`; `BCryptPasswordHasher`; CHECK `chk_users_password_hash_len` (V1) | Hash `$2a$10$`, 60 caracteres |
| CA-03 Email duplicado | Cumple | `ca03_emailDuplicadoConOtrasMayusculasDevuelve409`; `uk_users_email` con collation `utf8mb4_0900_as_ci`; `UserPersistenceAdapter` traduce la violación concurrente | 409 `EMAIL_ALREADY_REGISTERED` |
| CA-04 Documento duplicado | Cumple | `ca04_documentoDuplicadoAunqueTengaPuntosDevuelve409`; `uk_users_document`; `IdentityDocument` normaliza | 409 `DOCUMENT_ALREADY_REGISTERED` |
| CA-05 Validación | Cumple | `ca05_camposInvalidosDevuelven400ConErroresPorCampo`, `ca05_contrasenaQueNoCumpleLaPoliticaDevuelve400`, `ca05_tipoDeDocumentoInexistenteDevuelve400`; `PasswordPolicyTest`, `UserValueObjectsTest` | 400 `VALIDATION_ERROR` con `errors[]` por campo |
| CA-06 Login correcto | Cumple | `ca06_loginCorrectoEmiteAccessYRefreshDistintos`; `JwtTokenProviderTest.accessTokenLlevaUsuarioYRoles` | Access con roles; `expiresIn` 900 s, refresh 7 días |
| CA-07 Credenciales inválidas | Cumple | `ca07_credencialesInvalidasDevuelvenElMismo401`; `AuthServiceTest.loginFallaIgualConEmailInexistenteOContrasenaIncorrecta` | Mismo `detail` para ambos casos; sin tokens |
| CA-08 Acceso protegido | Cumple | `ca08_recursoProtegidoSoloConAccessTokenValido`; `JwtTokenProviderTest.accessYRefreshNoSonIntercambiables`, `tokenExpiradoSeRechaza`, `tokenAlteradoOBasuraSeRechaza` | Sin token, token inválido o refresh como access → 401 |
| CA-09 Refresh con rotación | Cumple | `ca09_refreshRotaElTokenYRevocaElAnterior`; `AuthServiceTest.refreshRotaElTokenYElAnteriorNoSePuedeReusar` | El usado queda revocado y enlazado (`replaced_by_token_id`); reuso → 401 |
| CA-10 Refresh inválido | Cumple | `ca10_refreshInvalidoDevuelve401SinEmitirTokens`; `AuthServiceTest.refreshRechazaAccessTokenYTokensDesconocidos`, `ca10_refreshExpiradoEnBaseDeDatosSeRechazaSinEmitirTokens`; `RefreshTokenTest.esUsableAntesDeExpirarYSinRevocar` | Access como refresh, basura, inexistente, revocado y expirado → 401 `INVALID_REFRESH_TOKEN` |
| CA-11 Logout | Cumple | `ca11_logoutRevocaElRefreshToken`; `AuthServiceTest.logoutRevocaYEsIdempotente` | 204 idempotente; refresh posterior → 401 |
| DoD-01 Arquitectura hexagonal | Cumple | `HexagonalArchitectureTest` (ArchUnit, 2 reglas); paquetes `domain`, `application`, `infrastructure/adapters` | Dominio sin Spring/JPA |
| DoD-02 Flyway | Cumple | `src/main/resources/db/migration/V1__identidad_hu001.sql`; `flyway_schema_history` v1 `success=1` en `jmunoz-citas-mysql` y en cada ejecución de Testcontainers | MySQL 8.4.11 |
| DoD-03 `mvn test` | Cumple | `mvnw test` 2026-09-18: 54 pruebas, 0 fallos, BUILD SUCCESS | Requiere Docker Desktop |
| DoD-04 Secretos | Cumple | `git ls-files` sin `.env`; `.gitignore`; variables en `README.md` (D-017); `JwtProperties` rechaza secretos débiles | Pruebas con secretos ficticios de `application-test.properties` |
| DoD-05 Logs | Cumple | Humo manual 2026-09-18: 0 coincidencias de contraseñas o JWT en el log de la API; `toString` enmascarado en comandos, DTOs y tokens | Revisión estática + ejecución |
| DoD-06 Contrato REST | Cumple | `docs/contratos/autenticacion.md`; consumido por `citas-web` (E2E 2026-09-18 con CORS 5174) | — |
| DoD-07 Trazabilidad | Cumple | Esta HU, [[EP-001-identidad-y-acceso]], `docs/wiki/scrum/README.md`, wiki `ejecucion.md` | — |

## Historial de validación

- 2026-09-16 (S2) — HU creada en estado `Pendiente de aprobación`.
- 2026-09-16 (S2) — HU `Aprobada` explícitamente por el Product Owner (Juan Muñoz).
- 2026-09-18 (S2) — HU `En desarrollo` (inicio del vertical slice de autenticación, confirmado por el usuario).
- 2026-09-18 (S2) — HU `En validación`: matriz de evidencia registrada (11 CA + 7 DoD en `Cumple`; `mvnw test` 54/54).
- 2026-09-18 (S2) — HU `Completada`.
- 2026-09-18 (S2) — DoD y T-07 ajustadas: el instructor indicó un único `.env` por carpeta (sin `.env.example`); las variables pasan a documentarse en `README.md` (D-017).

## Notas y decisiones

- Decisión aprobada (2026-09-16): rotación del refresh token en cada refresh y almacenamiento del refresh token como hash en BD.
- Decisión aprobada (2026-09-16): política de contraseña fija — mínimo 8 caracteres, al menos una letra y al menos un número.
- Resuelto (D-008): tipos de documento como catálogo fijo `document_types` (CC, CE, TI, RC, PA, PPT).
- Diseño implementado: el logout revoca el refresh token; el access token (15 min) sigue siendo válido hasta expirar y el cliente debe descartarlo. Contrato en `docs/contratos/autenticacion.md`.
- 2026-09-18 — Prueba manual de humo contra MySQL real: 20 casos de CA-01 a CA-11 con el resultado esperado.
- 2026-09-18 — Pruebas automatizadas (T-08): `AuthApiIntegrationTest` cubre CA-01…CA-11 contra MySQL 8.4 desechable (Testcontainers) con Flyway V1; además pruebas unitarias de dominio, `AuthService` con puertos falsos y `JwtTokenProvider`. Total 54 pruebas, 0 fallos. La validación formal de CA/DoD se hará al cerrar la HU (GOAL_01).
- 2026-09-18 — Reverificación (GOAL): añadida prueba de refresh expirado en BD con JWT aún parseable (CA-10). `mvnw test`: 55 pruebas, 0 fallos, BUILD SUCCESS.
