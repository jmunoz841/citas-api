---
tipo: dominio
actualizado: 2026-09-16
fuentes:
  - raw/PRD.md
---

# Dominio

Sistema académico de agendamiento de citas. Todos los datos de pacientes, profesionales, EPS, planes, horarios y citas son sintéticos.

## Actores

| Actor | Origen de la cuenta | Capacidades principales |
|---|---|---|
| USER | Autoregistro | Perfil/afiliación, buscar disponibilidad, reservar, cancelar, reprogramar, ver sus citas |
| PROFESSIONAL | Creado por ADMIN | Bloques de disponibilidad, agenda propia, cerrar atención (`COMPLETED`/`NO_SHOW`) |
| ADMIN | Seed | Profesionales, catálogos configurables, aprobar/rechazar citas especializadas y reprogramaciones |

## Sedes (catálogo fijo)

- **HIC** — Hospital Internacional de Colombia.
- **ICV** — Fundación Cardiovascular de Colombia / Instituto Cardiovascular.

## Catálogos

- **Fijos (seed, solo lectura):** roles, estados de cita, estados de reprogramación, regímenes, sedes.
- **Configurables (CRUD ADMIN):** EPS, planes de EPS, especialidades (duración 30 o 60 min). Sin borrado físico si están referenciados.

## Citas

- Slots de 30 min; especialidad de 60 min = 2 slots consecutivos.
- **General** (Medicina General) → `APPROVED` automáticamente.
- **Especializada** → `REQUESTED` con slots retenidos → ADMIN aprueba (`APPROVED`) o rechaza con motivo (`REJECTED`, libera slots).
- **Cancelación** por USER de cita futura no terminal → `CANCELLED`, libera slots.
- **Reprogramación** de cita `APPROVED` futura → solicitud `PENDING` con nueva franja retenida; la cita original se conserva hasta la decisión del ADMIN.
- **Cierre** por PROFESSIONAL → `COMPLETED` o `NO_SHOW`.
- Todo cambio de estado se audita: cita, estado nuevo, actor, fuente (`SYSTEM`/`USER`/`ADMIN`), fecha/hora, motivo opcional.

## Reglas de negocio (RN-01 a RN-12)

Sin doble reserva; generales auto-aprobadas; especializadas requieren ADMIN; rechazo con motivo; slots consecutivos para 60 min; nada en el pasado; agenda solo en sedes asignadas; especialidad activa y asociada; cancelar/rechazar libera; reprogramación no destruye la cita original; transiciones explícitas; auditoría no editable. Detalle en `raw/PRD.md` §5.

## Fuera de alcance

Historia clínica, facturación, pagos, diagnósticos, datos reales de FCV, integración con sistemas clínicos, SMS/WhatsApp, SMTP obligatorio.

## Relacionadas

[[arquitectura]] · [[decisiones]]
