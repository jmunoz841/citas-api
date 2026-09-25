---
tipo: ejecucion
actualizado: 2026-09-25
fuentes:
  - EVIDENCIAS_Y_TRAZABILIDAD.md (plantilla de registro)
  - GUIA_SESIONES_S2_S6.md (verificación obligatoria de S3)
---

# Evidencia S3

Evidencia mínima de S3 según `EVIDENCIAS_Y_TRAZABILIDAD.md`: commits, pruebas, hook en FAIL y PASS y secreto ficticio bloqueado. La matriz detallada por CA y DoD vive en cada HU; aquí está el resumen y la demo del hook.

## citas-api

```text
Sesión: S3
Repo: github.com/jmunoz841/citas-api
Branch: develop
Commit hash: a321231 (último de S3; historial completo desde 5718bd5)
HU abordadas: HU-004, HU-005, HU-006, HU-008, HU-009, HU-010, HU-012, HU-013, HU-014, HU-015 (todas Completada)
Criterios completados: 43 CA y la DoD completa de las 10 HU en "Cumple" (matriz en cada HU)
Pruebas ejecutadas: mvnw clean test 2026-09-25 → 153 pruebas, 0 fallos (dominio, aplicación, integración con Testcontainers MySQL 8.4 + Flyway V1–V6, ArchUnit)
Qué quedó pendiente: push (credencial de GitHub)
Evidencia adicional: migraciones V2–V6, contratos en docs/contratos/, LOOP de doble reserva (PK de slot_reservations), decisiones D-018 a D-030, hook pre-commit (D-024)
```

## citas-web

```text
Sesión: S3
Repo: github.com/jmunoz841/citas-web
Branch: develop
Commit hash: 6e8d9aa (último de S3; historial completo desde 7eecf11)
HU abordadas: vistas de HU-004, HU-005, HU-006, HU-008, HU-009, HU-010, HU-012, HU-013, HU-014, HU-015
Criterios completados: diseño Stitch v4 aprobado (D-026) e implementado con las 5 correcciones obligatorias; frontend sin mocks contra citas-api
Pruebas ejecutadas: npm run lint, npm test (96 pruebas), npm run typecheck y npm run build en verde; capturas autenticadas contra la API real (1440, 1280 y 390 px)
Qué quedó pendiente: push (credencial de GitHub)
Evidencia adicional: docs/diseno/stitch-v3, stitch-v4, APROBACION.md, DESIGN.md § Áreas autenticadas
```

## Matriz resumida por HU

| HU | Título | CA | Resultado CA | DoD | Pruebas principales |
|---|---|---|---|---|---|
| [[HU-004-registrar-afiliacion]] | Registrar afiliación EPS | 6 | Cumple | Cumple | `RegisterAffiliationApiIntegrationTest`, `RegisterPage.affiliation.test.tsx` |
| [[HU-005-consultar-catalogos-fijos]] | Consultar catálogos fijos | 3 | Cumple | Cumple | `CatalogApiIntegrationTest`, `catalogsApi.test.ts` |
| [[HU-006-gestionar-especialidades]] | Gestionar especialidades | 4 | Cumple | Cumple | `AdminOfferApiIntegrationTest`, `SpecialtiesPage.test.tsx` |
| [[HU-008-crear-profesional]] | Crear profesional | 5 | Cumple | Cumple | `AdminOfferApiIntegrationTest`, `ProfessionalsPage.test.tsx` |
| [[HU-009-activar-desactivar-profesional]] | Activar y desactivar profesional | 3 | Cumple | Cumple | `AdminOfferApiIntegrationTest`, `BookingApiIntegrationTest.hu009_ca02_…`, `ProfessionalsPage.test.tsx` |
| [[HU-010-gestionar-bloques-de-disponibilidad]] | Gestionar bloques de disponibilidad | 6 | Cumple | Cumple | `AvailabilityApiIntegrationTest`, `BookingApiIntegrationTest.hu010_ca05_…`, `AgendaPage.test.tsx` |
| [[HU-012-consultar-disponibilidad]] | Consultar disponibilidad | 4 | Cumple | Cumple | `SlotPlannerTest`, `BookingApiIntegrationTest.hu012_…`, `PatientHomePage.test.tsx` |
| [[HU-013-reservar-cita-general]] | Reservar cita general | 4 | Cumple | Cumple | `BookingApiIntegrationTest.hu013_…` (incluye el LOOP concurrente), `PatientHomePage.test.tsx` |
| [[HU-014-solicitar-cita-especializada]] | Solicitar cita especializada | 3 | Cumple | Cumple | `BookingApiIntegrationTest.hu014_…`, `PatientHomePage.test.tsx` |
| [[HU-015-resolver-cita-especializada]] | Aprobar o rechazar cita especializada | 5 | Cumple | Cumple | `AdminAppointmentApiIntegrationTest`, `AppointmentTest`, `RequestsPage.test.tsx` |

Ningún elemento quedó en `No cumple` ni `No verificable`. El Product Owner confirmó el cierre el 2026-09-25: las 10 HU están `Completada`, y `develop` se integró en `main` con la etiqueta `s3` en ambos repos.

## Verificación obligatoria de S3 (`GUIA_SESIONES_S2_S6.md`)

| # | Exigencia | Evidencia |
|---|---|---|
| 1 | El agente escribe pruebas antes o durante la implementación | HU-015, catálogo de especialidades, sesión con nombres, `/professional/me` y D-028 se hicieron con la prueba primero (Red → Green registrado en cada HU y en [[log]]) |
| 2 | Una prueba que falla intencionalmente | Demo del hook (abajo), en ambos repos |
| 3 | Red → Green | Ídem, más los Red → Green de desarrollo del punto 1 |
| 4 | Pruebas de reglas de slots 30/60 | `SlotPlannerTest` (8), `BookingApiIntegrationTest.hu012_ca01/ca02`, `hu014_ca02` |
| 5 | Prueba de doble reserva | `hu013_ca03_dobleReservaConcurrenteSoloUnaCreaLaCita` (dos peticiones HTTP simultáneas → 201 y 409) y `hu013_ca03_laClavePrimariaDeLaBaseImpideOcuparDosVecesElMismoSlot` |
| 6 | Pruebas de autorización básicas | 401/403 por rol en `BookingApiIntegrationTest`, `AdminAppointmentApiIntegrationTest`, `AvailabilityApiIntegrationTest`, `AdminOfferApiIntegrationTest` |
| 7 | Frontend con build, typecheck y pruebas | `citas-web`: lint, 96 pruebas, typecheck y build |
| 8 | Hook local que ejecuta verificaciones | `.githooks/pre-commit` en ambos repos (D-024) |
| 9 | Secreto ficticio bloqueado | Demo del hook (abajo) |
| 10 | Corregir y mostrar commit permitido | `citas-web` `6e8d9aa`, `citas-api` `a321231` |

## Demo del hook (2026-09-25)

Salida real de `git commit`, recortada. La clave usada fue una clave AWS ficticia (prefijo de 4 letras seguido de 16 caracteres): no se reproduce aquí porque el propio hook bloquearía este documento.

### citas-web

1. **Secreto ficticio → bloqueado** (`src/demo-secreto.ts`, exit 1):

   ```text
   [pre-commit] Buscando secretos en los archivos preparados...
   SECRETOS: posibles credenciales en los archivos preparados para commit.
     src/demo-secreto.ts:1 clave privada o credencial de conexion.
   Commit bloqueado.
   ```

2. **Prueba en rojo → bloqueado** (exit 1). `initials('andrés vargas rojas')` se esperó `'AVR'` a propósito:

   ```text
   [pre-commit] npm test...
    FAIL  src/features/booking/utils/booking.test.ts > … > las iniciales del avatar usan nombre y primer apellido en mayúsculas
   AssertionError: expected 'AV' to be 'AVR'
         Tests  1 failed | 95 passed (96)
   [pre-commit] Pruebas en rojo: commit bloqueado.
   ```

3. **Corregida → permitido** (exit 0), commit `6e8d9aa`:

   ```text
   [pre-commit] Sin secretos.
         Tests  96 passed (96)
   [pre-commit] Lint, pruebas y build en verde.
   [develop 6e8d9aa] test(s3): cover avatar initials
   ```

### citas-api

1. **Secreto ficticio → bloqueado** (`src/main/resources/demo-secreto.properties`, exit 1): mismo mensaje del detector.
2. **Prueba en rojo → bloqueado** (exit 1). Un bloque de un solo slot se esperó con un inicio reservable para 60 minutos:

   ```text
   [pre-commit] Ejecutando mvnw test...
   [ERROR] SlotPlannerTest.unBloqueDeUnSoloSlotNoAdmiteUnaCitaDeSesenta:78 Expected size: 1 but was: 0
   [ERROR] Tests run: 153, Failures: 1, Errors: 0, Skipped: 0
   [pre-commit] Pruebas en rojo: commit bloqueado.
   ```

3. **Corregida → permitido** (exit 0), commit `a321231`. El hook corre `mvnw -q test`, que en verde no imprime resumen; se confirmó aparte con `mvnw clean test -Dtest=SlotPlannerTest` → 8/8.

Los archivos de la demo del secreto nunca llegaron a un commit y se borraron del árbol de trabajo.

## Relacionadas

[[ejecucion]] · [[decisiones]] · [[evidencia-s2]]
