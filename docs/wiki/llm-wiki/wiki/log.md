# Log — LLM Wiki `citas`

Registro append-only. Formato definido en `schema/SCHEMA.md`.

## 2026-09-16 — SPEC — Especificación Scrum inicial
- Fuentes: `raw/PRD.md`, `raw/RESTRICCIONES_TECNICAS.md`, `raw/REQUISITOS_NORMALIZACION_3FN.md`
- Resultado: 8 épicas y 25 HU en `docs/wiki/scrum/` generadas con `scrum-spec-orchestrator`.

## 2026-09-16 — INGEST — Documentos base y decisiones de HU-001
- Fuentes: `raw/PRD.md`, `raw/RESTRICCIONES_TECNICAS.md`, `raw/REQUISITOS_NORMALIZACION_3FN.md`, `raw/2026-09-16-decisiones-hu-001.md`
- Páginas afectadas: [[index]], [[dominio]], [[arquitectura]], [[decisiones]], [[ejecucion]] (creadas)

## 2026-09-16 — INGEST — Decisiones de workspace y puerto MySQL
- Fuentes: `raw/2026-09-16-decisiones-workspace.md`
- Páginas afectadas: [[decisiones]] (D-005, D-006; cerradas Q-001 y Q-002), [[arquitectura]], [[index]]

## 2026-09-16 — LEARN — Inicialización del backend (paso 6)
- HECHO: `mvnw test` con JDK 21 → 2 tests ArchUnit, BUILD SUCCESS.
- HECHO: Spring Initializr no ofrece Boot 3.5.x; `pom.xml` manual con 3.5.16.
- Páginas afectadas: [[arquitectura]], [[ejecucion]]

## 2026-09-16 — INGEST — Diseño 3FN propio y decisiones de normalización
- Fuentes: `docs/database/normalizacion-3fn/` (agente de normalización, sin acceso a la referencia), `raw/2026-09-16-decisiones-normalizacion.md`
- Páginas afectadas: [[decisiones]] (D-007, D-008), [[index]]; `application.yml` pasa a America/Bogota

## 2026-09-16 — INGEST — Entorno Docker aislado del otro grupo
- Fuentes: `raw/2026-09-16-decisiones-entorno-aislado.md`
- HECHO: `jmunoz-citas-mysql` healthy; `SELECT VERSION(), @@time_zone` → 8.4.11, America/Bogota, utf8mb4, BD `citas_fcv_training`.
- Páginas afectadas: [[decisiones]] (D-009 reemplaza D-006), [[arquitectura]], [[index]]

## 2026-09-18 — INGEST — Repo privado de la raíz del workspace
- Fuentes: `raw/2026-09-18-decisiones-repo-raiz.md`
- Páginas afectadas: [[decisiones]] (D-011 reemplaza D-005), [[index]]

## 2026-09-18 — INGEST — Comparación con la referencia y migración V1
- Fuentes: `docs/database/normalizacion-3fn/comparacion-referencia.md`, `raw/2026-09-18-decisiones-comparacion-referencia.md`
- HECHO: `schema.sql` propio cargado sin errores en MySQL 8.4.11 (23 tablas, 33 FK, 31 CHECK, 2 triggers) en base desechable; 9 pruebas de restricciones de HU-001 con el resultado esperado.
- HECHO: `V1__identidad_hu001.sql` aplicada con Flyway; aviso de Flyway sobre soporte probado hasta MySQL 8.1.
- Páginas afectadas: [[decisiones]] (D-012, D-013; Q-003 cerrada), [[ejecucion]], [[index]]

## 2026-09-18 — LEARN — Vertical slice de autenticación (paso 8)
- HECHO: API arranca con Flyway `validate` sobre V1; humo manual 20/20 (CA-01…CA-11) contra `jmunoz-citas-mysql`; contraseña guardada como BCrypt `$2a$10$`; refresh tokens como SHA-256 hex; rotación enlazada por `replaced_by_token_id`; 0 contraseñas/tokens en el log.
- Páginas afectadas: [[arquitectura]], [[ejecucion]], [[index]]

## 2026-09-18 — LEARN — Pruebas automatizadas de HU-001 (paso 9)
- HECHO: `mvn test` → 54 pruebas, 0 fallos, BUILD SUCCESS; integración contra `mysql:8.4` de Testcontainers (el contenedor se elimina al terminar; la BD local no cambia).
- DECISIÓN (usuario, 2026-09-18): pruebas de integración con Testcontainers en lugar de la MySQL local.
- Páginas afectadas: [[arquitectura]], [[ejecucion]]

## 2026-09-18 — LEARN — Cierre de S2
- HECHO: HU-001 validada y `Completada` (11 CA + 7 DoD en `Cumple`); `mvnw test` 54/54; commits de cierre en ambos repos.
- HECHO: GOAL_01 ejecutado por el usuario con `/goal`; 6 condiciones verificadas; nueva prueba de refresh expirado; `mvnw test` 55/55 (commit `a5f583e`).
- Páginas afectadas: [[ejecucion]], [[evidencia-s2]], [[index]]

## 2026-09-18 — INGEST — Un único .env por carpeta
- DECISIÓN (instructor, vía usuario): solo `.env`, sin `.env.example`; local y no versionado (D-017).
- HECHO: `.env` de raíz creado con los valores de `citas-api/.env`; `.env.example` eliminados de los tres repos; README, AGENTS, HU-001 (T-07/DoD), `preflight.ps1` y comentarios actualizados.
- Páginas afectadas: [[decisiones]], [[ejecucion]]

## 2026-09-18 — INGEST — Panel de sedes plegable
- DECISIÓN (usuario): riel delgado de 72px, cambio directo en código (D-016).
- HECHO: typecheck/build en verde; capturas desplegado y plegado correctas; la preferencia persiste entre pantallas (`localStorage`).
- Páginas afectadas: [[decisiones]]

## 2026-09-18 — LEARN — AGENTS del frontend (paso 14)
- HECHO: `citas-web/AGENTS.md` y `CLAUDE.md` creados; `AGENTS.md.template` retirado. PREGUNTA ABIERTA: herramienta de pruebas automatizadas del frontend (hoy solo typecheck/build).
- Páginas afectadas: [[ejecucion]]

## 2026-09-18 — LEARN — Importación del frontend (pasos 12 y 13)
- HECHO: export de AI Studio con buena estructura y accesibilidad; se retiraron dependencias no pedidas y se corrigieron tipos numéricos, rol `USER` en el mock, sesión cerrada por errores de red, resumen de errores que etiquetaba mal los 400 y no se limpiaba, estilos de error y tamaños fuera de `DESIGN.md`.
- HECHO: E2E navegador→API con origen 5174; `mvn test` 54/54 tras cambiar CORS.
- HECHO: Edge headless tiene ancho mínimo ~500px; para móvil se captura dentro de un iframe de 390px.
- Páginas afectadas: [[decisiones]] (D-015), [[ejecucion]], [[arquitectura]]

## 2026-09-18 — INGEST — Diseño aprobado de login y registro (paso 11)
- Fuentes: `citas-web/docs/diseno/APROBACION.md`, `DESIGN.md`, `stitch-v1/`, `stitch-v2/`
- DECISIÓN (usuario): v2 aprobada; Ley 1581 fuera de S2. PREGUNTA ABIERTA: consentimiento de datos como HU futura.
- Páginas afectadas: [[decisiones]] (D-014), [[ejecucion]]

## 2026-09-18 — LEARN — AGENTS del backend (paso 10)
- HECHO: `citas-api/AGENTS.md` generado desde el estado real del repo; `citas-api/CLAUDE.md` lo importa; `AGENTS.md.template` retirado.
- Páginas afectadas: [[ejecucion]]

## 2026-09-23 — INGEST — Apertura de S3 y decisiones
- Fuentes: `raw/2026-09-23-decisiones-s3.md`, brief de S3 del instructor
- DECISIÓN (usuario): diez HU pasan a `Aprobada`; HU-004 recortada a afiliación **opcional**; API versionada bajo `/api/v1` (D-018); ADMIN inicial por semilla de migración (D-021); backend completo antes de la pasada de frontend.
- Páginas afectadas: [[decisiones]] (D-018 a D-024), [[ejecucion]]

## 2026-09-23 — LEARN — Núcleo administrable de S3 (HU-004, 005, 006, 008, 009, 010)
- HECHO: migraciones `V2`–`V5`; `mvnw test` 108/108; `npm test` 22/22; Flyway `v1`–`v5` con `success=1` contra MySQL real.
- HECHO: el `mockAuthApi` del frontend tenía un *fallback* silencioso que servía una cuenta de demo con contraseña en el código si faltaba `VITE_API_URL`; se eliminó y ahora `apiBaseUrl()` falla en arranque.
- HECHO: los slots se materializan como filas al crear el bloque (D-013). Es lo que permitirá que la doble reserva la impida la clave primaria de la base, no la aplicación.
- HECHO: `uk_slots_professional_start` impide bloques solapados aunque sean de sedes distintas, porque al estar todo alineado a `:00`/`:30` dos bloques que se cruzan comparten al menos un inicio de slot.
- HECHO: un bloque ajeno responde `404` y no `403`; un `403` confirmaría que ese bloque existe.
- PREGUNTA ABIERTA: ninguna. CA diferidos con razón escrita: [[HU-009-activar-desactivar-profesional]] CA-02 (necesita HU-012) y [[HU-010-gestionar-bloques-de-disponibilidad]] CA-05 (necesita `slot_reservations` de HU-013).
- Páginas afectadas: [[ejecucion]], [[arquitectura]]

## 2026-09-23 — LEARN — Credenciales de GitHub en el equipo compartido
- HECHO: el push a `jmunoz841/*` fallaba con `403` porque el Administrador de credenciales de Windows guarda el token de otra cuenta (`christtobar-land`). La autoría de los commits siempre fue correcta; solo fallaba la credencial de red.
- DECISIÓN: se fija `credential.https://github.com.username = jmunoz841` en el `.git/config` **local** de los tres repos, no en el global, para no invalidar la sesión del otro estudiante del equipo compartido.
- PREGUNTA ABIERTA: falta autenticarse una vez y publicar S2 y S3.
- Páginas afectadas: [[ejecucion]]

## 2026-09-25 — LEARN — Búsqueda y reserva de citas (HU-012, HU-013, HU-014)
- Fuentes: `raw/2026-09-25-decisiones-reserva.md`
- HECHO: migración `V6`; `GET /api/v1/availability` y `POST /api/v1/appointments`; `mvnw clean test` 136/136. Flyway v5 y v6 aplicadas en `jmunoz-citas-mysql` (la base local seguía en v4, aunque el log del 2026-09-23 decía v1–v5).
- HECHO: el LOOP del instructor pasa por HTTP con dos hilos: un 201 y un 409. La aplicación no comprueba antes si el slot está libre; lo decide la PK `slot_id` de `slot_reservations` (error 1062).
- HECHO: guardar un profesional borraba y reinsertaba sus asignaciones; con bloques, la FK RESTRICT devolvía 500 al desactivarlo. Ahora se sincroniza por diferencia.
- HECHO: en este equipo el editor guarda los archivos con hora de modificación unas 5 h adelantada y la compilación incremental de Maven puede ejecutar clases viejas; usar `mvnw clean test`. El desfase de reloj provoca también fallos intermitentes de TLS con MySQL de Testcontainers.
- DECISIÓN (usuario): D-025, historial sin triggers; LOOP probado por HTTP.
- PREGUNTA ABIERTA: endpoint único de reserva; retirar especialidad o sede con agenda (500 por FK); citas de un profesional desactivado.
- Páginas afectadas: [[decisiones]], [[ejecucion]], [[arquitectura]]

## 2026-09-25 — LEARN — Decisión del ADMIN sobre citas especializadas (HU-015)
- HECHO: pruebas escritas antes del código (Red: 7 de 9 fallan con 404 → Green: 13/13); `mvnw clean test` 149/149.
- HECHO: aprobar y rechazar cambian el estado con un `UPDATE` condicionado a `REQUESTED`; dos decisiones simultáneas → un 200 y un 409 `INVALID_STATUS_TRANSITION`. Rechazar borra las filas de `slot_reservations` y los slots vuelven a la búsqueda.
- PREGUNTA ABIERTA: expiración de solicitudes `REQUESTED` (el PRD no la define).
- Páginas afectadas: [[ejecucion]]

## 2026-09-25 — LEARN — Catálogo público de especialidades
- HECHO: el modal de reserva no tenía de dónde listar especialidades (`/api/v1/admin/specialties` es solo ADMIN). Se añadió `GET /api/v1/catalogs/specialties` (activas, con `type` GENERAL/SPECIALIZED); `mvnw clean test` 150/150.
- HECHO: se entregaron tres prompts de Stitch (ADMIN, PROFESSIONAL, USER) que extienden `citas-web/docs/diseno/DESIGN.md`; el diseño sigue pendiente de generar y aprobar.
- Páginas afectadas: [[ejecucion]]

## 2026-09-25 — INGEST — Diseño aprobado de las áreas autenticadas de S3
- Fuentes: `raw/2026-09-25-aprobacion-diseno-s3.md`, `citas-web/docs/diseno/APROBACION.md`
- HECHO: la v3 de Stitch repitió dos fallos ya corregidos en la v1 (superficies azuladas y contenido inventado, incluido un soporte telefónico 24/7).
- DECISIÓN (usuario): D-026, v4 aprobada con cinco correcciones obligatorias en la implementación.
- Páginas afectadas: [[decisiones]], [[ejecucion]]

## 2026-09-25 — LEARN — Pasada de frontend de S3
- HECHO: la API no ofrecía los nombres de la sesión ni un perfil propio del profesional; se añadieron de forma aditiva (`/auth/session` con nombres, `/professional/me`). `mvnw clean test` 151/151.
- HECHO: `citas-web` tiene rutas por rol, cliente con refresh ante 401 y las seis pantallas aprobadas en Stitch v4 (D-026). 95 pruebas, lint y build en verde; verificado contra la API real.
- HECHO: en la primera carga en frío, la fuente variable de Material Symbols tarda varios segundos y los íconos se ven como texto ("inbox", "check"); después queda en caché. Afecta también al login desde S2.
- HECHO: los datos sintéticos de prueba se cargan con un script local no versionado; PowerShell 5.1 lee un `.ps1` sin BOM como ANSI y corrompe los acentos (se corrigieron con SQL).
- Páginas afectadas: [[ejecucion]]

## 2026-09-25 — INGEST — Preguntas abiertas de S3 resueltas
- Fuentes: `raw/2026-09-25-decisiones-abiertas-s3.md`
- DECISIÓN (usuario): D-027 endpoint único de reserva; D-028 `409 ASSIGNMENT_IN_USE` al quitar una asignación en uso; D-029 desactivar conserva las citas; D-030 sin expiración de solicitudes en S3.
- HECHO: D-028 implementada con la prueba primero (Red: 500 → Green: 409); `mvnw clean test` 152/152.
- Páginas afectadas: [[decisiones]], [[ejecucion]]

## 2026-09-25 — LEARN — Evidencia de cierre de S3
- HECHO: las 10 HU de S3 tienen sus 43 CA y su DoD en `Cumple`; ninguna en `No cumple` ni `No verificable`. Siguen `Aprobada` hasta que el Product Owner confirme el cierre.
- HECHO: las épicas listaban todas las HU como `Borrador`; se sincronizaron con el estado real de cada HU.
- HECHO: demo del hook en ambos repos: secreto ficticio bloqueado, prueba en rojo bloqueada y corregida permitida (`citas-web` `6e8d9aa`, `citas-api` `a321231`). El hook de `citas-api` usa `mvnw -q` y no imprime resumen en verde.
- Páginas afectadas: [[evidencia-s3]], [[ejecucion]], [[index]]

## 2026-09-25 — INGEST — Cierre de S3
- DECISIÓN (usuario): "aprobado". Las 10 HU de S3 (HU-004, 005, 006, 008, 009, 010, 012, 013, 014, 015) pasan a `Completada`; merge `develop` → `main` y etiqueta `s3` en `citas-api` y `citas-web`.
- PREGUNTA ABIERTA: ninguna de producto. Falta el push (credencial de GitHub).
- Páginas afectadas: [[evidencia-s3]], [[ejecucion]]
