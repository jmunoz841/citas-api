# AGENTS.md — `citas-api`

Instrucciones para el agente principal del backend. Generado con `../prompts/agents/PROMPT_AGENT_CITAS_API.md` a partir del estado real del repositorio (2026-09-18). El orquestador cross-repo está en `../AGENTS.md`.

## Alcance

- Solo este repositorio. **No edites `citas-web`**; si un cambio afecta el contrato REST, avisa al orquestador.
- Fuente funcional: `../PRD.md`, `../RESTRICCIONES_TECNICAS.md` y las HU **Aprobadas** o **En desarrollo** de `docs/wiki/scrum/`. No implementes nada fuera de una HU.
- Lee antes de actuar: la HU en curso, `docs/contratos/`, `docs/wiki/llm-wiki/wiki/decisiones.md` y `docs/database/normalizacion-3fn/README.md`.

## Stack real

| Pieza | Versión / detalle |
|---|---|
| Java | 21 (JDK Temurin portable en `%USERPROFILE%\.jdks\temurin-21`; el Java global del equipo es 26 y no sirve) |
| Spring Boot | 3.5.16 (`pom.xml` manual: Initializr ya no ofrece 3.5.x) |
| Build | Maven Wrapper (`mvnw.cmd`, Maven 3.9.16). No hay Maven instalado |
| Persistencia | Spring Data JPA + Flyway + MySQL 8.4 (`mysql-connector-j`) |
| Seguridad | Spring Security stateless + jjwt 0.13.0 (HS256) + BCrypt |
| Pruebas | JUnit 5, AssertJ, MockMvc, Testcontainers (`mysql:8.4`), ArchUnit 1.5.0 |

## Comandos

Desde `citas-api/` en PowerShell:

```powershell
$env:JAVA_HOME="$env:USERPROFILE\.jdks\temurin-21"; $env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test              # 54+ pruebas; requiere Docker Desktop (Testcontainers)
docker compose up -d         # MySQL propio jmunoz-citas-mysql en localhost:3308
.\mvnw.cmd spring-boot:run   # API en http://localhost:8081 (aplica migraciones Flyway)
```

- Para lanzar la API en segundo plano usa `Start-Process -FilePath mvnw.cmd -WorkingDirectory <citas-api>`; `cmd /c` falla con la ruta con espacios.
- **No** uses el `docker-compose.yml` de la raíz ni toques contenedores/volúmenes `fcv-citas-*`: pertenecen a otro grupo que comparte este equipo. Puertos ocupados por ellos: 3307, 8080, 5173, 4200.

## Arquitectura hexagonal (`com.citas.api`)

```text
domain/                         Java puro: sin Spring, JPA, Jakarta ni HTTP
  model/user/                   User, Email, IdentityDocument, DocumentType, Role, PasswordPolicy
  model/auth/                   RefreshToken
  exception/                    DomainException (+ code estable) y subclases
application/                    depende solo de domain (se permite @Transactional)
  port/in/                      casos de uso: RegisterUser, Login, RefreshSession, Logout
  port/out/                     UserRepositoryPort, RefreshTokenRepositoryPort, PasswordHasherPort, TokenProviderPort
  service/                      AuthService (sin @Service: se registra en infrastructure/config)
infrastructure/
  adapters/in/web/<modulo>/     controladores + DTOs (records) por módulo
  adapters/in/web/error/        GlobalExceptionHandler, ApiProblems, SecurityProblemHandlers
  adapters/out/persistence/     entidades *JpaEntity, repositorios Spring Data (package-private), adaptadores de puertos
  adapters/out/security/        BCryptPasswordHasher, JwtTokenProvider, JwtAuthenticationFilter
  config/                       ApplicationConfig (beans de casos de uso, Clock), SecurityConfig, JwtProperties
```

Reglas (verificadas por `HexagonalArchitectureTest`):
- `domain` no depende de `application`, `infrastructure`, Spring, Jakarta ni Hibernate.
- `application` no depende de `infrastructure`, Spring Web, JPA ni Hibernate.
- Los controladores traducen HTTP ↔ comandos de `port.in`; la lógica y las reglas viven en dominio/aplicación.
- Un caso de uso nuevo: interfaz en `port.in`, implementación en `service`, bean en `ApplicationConfig`.
- Las entidades JPA no salen del paquete de persistencia; el adaptador mapea a/desde el modelo de dominio.

## Convenciones

- **Fechas:** `Clock` de Spring en `America/Bogota` (D-007). Usa `LocalDateTime.now(clock)` / `clock.instant()`; nunca `now()` sin reloj.
- **Validación:** DTOs con Jakarta Validation para forma; reglas de negocio en el dominio (lanzan `InvalidFieldException` con el campo).
- **Errores:** siempre ProblemDetail (RFC 9457) con `code` estable y, en validación, `errors[{field, message}]`. Nuevas excepciones de dominio → extiende `DomainException` y mapea el HTTP en `GlobalExceptionHandler`. Documenta cada `code` nuevo en el contrato.
- **Concurrencia:** las restricciones únicas de MySQL son la última defensa; tradúcelas a la excepción de dominio en el adaptador (ver `UserPersistenceAdapter`). Usa lock pesimista cuando una fila se lee para modificarla (ver `RefreshTokenJpaRepository`).
- **Nombres:** código en inglés; mensajes al usuario, documentación y nombres de pruebas en español.
- **Registros y `toString`:** los records que transportan contraseñas o tokens sobrescriben `toString` con `***`. No registres cuerpos de petición, contraseñas ni tokens.

## Base de datos y Flyway

- Migraciones en `src/main/resources/db/migration/V<n>__<descripcion>.sql`. **Nunca edites una migración ya aplicada**; crea una nueva.
- Todo cambio de esquema parte del diseño 3FN propio (`docs/database/normalizacion-3fn/schema.sql`) y se justifica en la HU. Si te apartas del diseño, actualízalo y registra la decisión.
- `spring.jpa.hibernate.ddl-auto=validate`: Flyway gobierna el esquema. Columnas `CHAR` requieren `columnDefinition`. `created_at`/`updated_at` los gestiona MySQL y no se mapean.
- Charset `utf8mb4`; email con collation `utf8mb4_0900_as_ci`; catálogos fijos con PK `code`.
- Flyway avisa que su soporte probado llega a MySQL 8.1; V1 funciona en 8.4.11. Si una migración falla por compatibilidad, repórtalo.

## Seguridad

- Secretos solo por variables de entorno (`.env` local importado por `application.yml`; plantilla en `.env.example`). **Nunca abras, imprimas ni copies `.env`.**
- `JwtProperties` impide arrancar con secretos de menos de 32 bytes o iguales entre access y refresh.
- Access (15 min) y refresh (7 días) son JWT con secreto y `typ` distintos. El refresh se guarda solo como SHA-256 hex y se rota en cada uso (D-003).
- Contraseñas: BCrypt; política en `PasswordPolicy` (D-004).
- Rutas públicas: `POST /api/auth/{register,login,refresh,logout}` y `GET /actuator/health`. El resto exige access token. Autorización por rol con `ROLE_<ROL>`; aplica también *ownership* cuando la HU lo pida.
- CORS: solo `FRONTEND_ORIGIN`.

## Pruebas

| Tipo | Dónde | Cómo |
|---|---|---|
| Dominio | `src/test/.../domain` | Unitarias puras |
| Aplicación | `src/test/.../application` | Servicio con puertos falsos en memoria (sin Spring) |
| Adaptadores | `src/test/.../infrastructure` | Unitarias del adaptador (p. ej. `JwtTokenProviderTest`) |
| Integración REST + BD | `*IntegrationTest` | `@SpringBootTest` + MockMvc + `@ActiveProfiles("test")` + Testcontainers `mysql:8.4` con `@ServiceConnection` |
| Arquitectura | `HexagonalArchitectureTest` | ArchUnit |

- Cada criterio de aceptación de la HU debe tener al menos una prueba que lo demuestre; nombra la prueba con su `CA-xx`.
- Datos de prueba sintéticos y únicos por prueba (el contenedor se comparte dentro de la clase).
- Perfil `test`: secretos ficticios en `src/test/resources/application-test.properties`. No requiere `.env`.
- No declares una tarea terminada si `mvnw test` falla o no se ejecutó.

## Modo de trabajo

1. Localiza la HU y su DoD en `docs/wiki/scrum/historias-de-usuario/`.
2. Identifica reglas, decisiones (`decisiones.md`) y contratos (`docs/contratos/`) afectados.
3. Propón un plan antes de editar.
4. Implementa el mínimo coherente, capa por capa (dominio → aplicación → adaptadores).
5. Ejecuta `mvnw test`.
6. Verifica arquitectura y DoD.
7. Resume la evidencia y deja explícito lo no verificado.

## Documentación

- **Contratos REST:** `docs/contratos/<modulo>.md`. Actualízalos en el mismo cambio que modifica la API.
- **Scrum** (`docs/wiki/scrum/`): lo gestiona la skill `scrum-spec-orchestrator`. Este agente solo marca tareas o añade notas de evidencia en la HU en curso; no cambia estados a `Aprobada` ni `Completada`.
- **LLM Wiki** (`docs/wiki/llm-wiki/`): es la memoria global del orquestador. No mantengas una wiki propia; léela y propón al orquestador los hechos o decisiones nuevos.

## Git

- Trabaja en `develop`; `main` solo cuando el usuario decida un merge.
- Mensajes Conventional Commits con prefijo de sesión, p. ej. `feat(s2): ...`, `test(s2): ...`.
- Autor: Juan Munoz <jmunoz841@unab.edu.co>. **Sin líneas de coautoría de IA.**
- No reescribas historial (sin rebase destructivo, squash ni force push). Verifica que ningún `.env` quede en staging.
