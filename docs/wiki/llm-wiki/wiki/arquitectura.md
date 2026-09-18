---
tipo: arquitectura
actualizado: 2026-09-16
fuentes:
  - raw/RESTRICCIONES_TECNICAS.md
  - raw/PRD.md
---

# Arquitectura

> Estado: **backend inicializado (esqueleto), frontend pendiente**. Actualizar con evidencia del repositorio.

## Repositorios

| Repo | Remoto | Ramas | Responsabilidad |
|---|---|---|---|
| `citas-api` | github.com/jmunoz841/citas-api | `main` (estable), `develop` (trabajo) | Lógica de negocio, persistencia, seguridad, API REST, wiki global, JSON n8n |
| `citas-web` | github.com/jmunoz841/citas-web | `main`, `develop` | UI; consume `citas-api` por REST |

## Backend (`citas-api`)

- Java 21 LTS, Spring Boot **3.5.16**, Maven Wrapper (`mvnw`, Maven 3.9.16). `pom.xml` escrito a mano: Spring Initializr ya no ofrece 3.5.x.
- Paquete raíz `com.citas.api`. Arquitectura hexagonal:
  - `domain`: modelo y reglas, sin dependencias de Spring/JPA/Jakarta.
  - `application`: `port.in`, `port.out`, `service`; depende solo de `domain`.
  - `infrastructure/adapters`: `in.web` (REST), `out.persistence` (Spring Data JPA), `out.security` (JWT, BCrypt); `infrastructure/config`.
  - Reglas de dependencia verificadas por `HexagonalArchitectureTest` (ArchUnit 1.5.0).
- Autenticación (HU-001), por capa:
  - `domain.model.user` (`User`, `Email`, `IdentityDocument`, `DocumentType`, `Role`, `PasswordPolicy`), `domain.model.auth.RefreshToken`, `domain.exception`.
  - `application.port.in` (registro, login, refresh, logout), `application.port.out` (usuarios, refresh tokens, hash, tokens), `application.service.AuthService` (con `@Transactional`; cableado en `infrastructure.config.ApplicationConfig`, sin anotaciones de componente).
  - `infrastructure.adapters.out.persistence` (entidades JPA, lock pesimista al leer un refresh token), `infrastructure.adapters.out.security` (BCrypt, `JwtTokenProvider` HS256 con secretos y `typ` distintos, filtro JWT), `infrastructure.adapters.in.web` (`AuthController`, errores ProblemDetail), `infrastructure.config` (`SecurityConfig` stateless + CORS, `JwtProperties` exige secretos ≥ 32 bytes y distintos).
  - Contrato: `docs/contratos/autenticacion.md`.
- Pruebas: unitarias de dominio y aplicación (puertos falsos, sin Spring) + integración REST/persistencia con Testcontainers (`mysql:8.4` desechable, `@ServiceConnection`, perfil `test` con secretos ficticios en `src/test/resources/application-test.properties`). **`mvn test` requiere Docker Desktop**; no usa ni modifica `jmunoz-citas-mysql`.
- Dependencias: web, validation, data-jpa, mysql-connector-j, flyway-core + flyway-mysql, security, jjwt 0.13.0, actuator; test: spring-boot-starter-test, spring-security-test, archunit.
- Configuración en `application.yml` solo por variables de entorno; en local importa `citas-api/.env` (`spring.config.import: optional:file:.env[.properties]`). `ddl-auto: validate`: el esquema lo gobierna Flyway.
- Toolchain local: JDK Temurin 21 portable en `%USERPROFILE%\.jdks\temurin-21` (el Java global del equipo es 26). Ejecutar con `JAVA_HOME` apuntando a esa ruta.
- MySQL 8.4 + Flyway (migraciones propias del estudiante).
- JWT access (corta duración) y refresh separados; ver [[decisiones]] para rotación y almacenamiento.
- Actuator health recomendado.

## Frontend (`citas-web`)

- Node.js 24 LTS + TypeScript.
- React **o** Angular, según export de Stitch/Google AI Studio. **Supuesto actual:** React + Vite + TypeScript + Tailwind (pendiente de confirmar).
- URL del backend por environment (`VITE_API_URL` o equivalente Angular).
- Sin Express ni BFF.

## Seguridad transversal

BCrypt; secretos solo por variables de entorno; autorización por rol y ownership; CORS explícito (`FRONTEND_ORIGIN`); validación server-side; sin logging de contraseñas/tokens.

## Entorno local

- Desde `citas-api/`: `docker compose up -d` levanta `jmunoz-citas-mysql` (MySQL 8.4.11, zona America/Bogota, utf8mb4) en `localhost:3308` ([[decisiones]] D-009).
- **No usar** el `docker-compose.yml` de la raíz: su proyecto `fcv-citas-training` y nombres de contenedor colisionan con otro grupo que usa la plantilla en este equipo.
- Puertos ocupados por el otro grupo: 3307 (MySQL), 8080 (api-dev), 5173 y 4200 (web-dev).
- Variables backend (`citas-api/.env`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`, `JWT_ACCESS_MINUTES`, `JWT_REFRESH_DAYS`, `FRONTEND_ORIGIN`.

## Documentación

- `docs/wiki/scrum/`: épicas y HU (skill `scrum-spec-orchestrator`).
- `docs/wiki/llm-wiki/`: esta wiki.
- `automations/n8n/`: JSON de workflows (S5–S6).

## Preguntas abiertas

- Framework frontend definitivo.

## Relacionadas

[[dominio]] · [[decisiones]] · [[ejecucion]]
