---
id: EP-001
tipo: epica
titulo: "Identidad y acceso"
estado: Pendiente de aprobación
historias:
  - "[[HU-001-registro-e-inicio-de-sesion-jwt]]"
  - "[[HU-002-recuperar-contrasena]]"
dependencias: []
---

# EP-001 — Identidad y acceso

## Objetivo

Permitir que las personas creen su cuenta, inicien sesión con JWT access/refresh y recuperen su contraseña de forma segura (RF-01, RF-02, RF-03).

## Valor esperado

Base de identidad y autorización por rol sobre la que se construyen todas las demás capacidades.

## Actores

- Visitante
- USER
- PROFESSIONAL y ADMIN (login)

## Alcance

- Registro de USER con unicidad de email y documento.
- Login, refresh y logout/revocación con JWT separados.
- Recuperación de contraseña con token temporal de un solo uso.

## Fuera de alcance

- Verificación de email, MFA, OAuth social.
- SMTP obligatorio.

## Reglas de negocio

- Contraseñas con hash adaptativo (BCrypt).
- Access y refresh token separados; roles en el contexto de autorización.
- Secretos solo por variables de entorno; nunca registrar contraseñas/tokens.

## Dependencias

- Ninguna (épica fundacional).

## Historias de usuario

- [[HU-001-registro-e-inicio-de-sesion-jwt]] — Sprint 1 — Completada
- [[HU-002-recuperar-contrasena]] — Sprint 3 — Borrador

## Criterio de completitud de la épica

- [ ] Todas las HU obligatorias de esta épica están `Completada`.
- [ ] No quedan dependencias bloqueantes dentro del alcance de la épica.

## Riesgos e incógnitas

- Resuelto: rotación del refresh token y almacenamiento como hash.
- Resuelto: contraseña mínima de 8 caracteres con letra y número.
