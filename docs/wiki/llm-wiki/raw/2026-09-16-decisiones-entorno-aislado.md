# Decisiones aprobadas — Entorno Docker aislado (S2)

- **Fecha:** 2026-09-16
- **Aprobado por:** Juan Muñoz
- **Contexto:** en el mismo equipo otro grupo trabaja con la misma plantilla (`Documents\Proyectos Christian\FCV_Proyecto_Citas_v1-main`). Ambos `docker-compose.yml` de plantilla usan el proyecto `fcv-citas-training` y los mismos nombres de contenedor, por lo que se pisan entre sí. El 2026-09-16 un `docker compose up -d mysql` desde la raíz de este workspace recreó el contenedor `fcv-citas-mysql` del otro grupo; su volumen `fcv-citas-training_mysql_data` no se modificó ni se consultó.

## Decisiones

1. Se eliminó solo el contenedor recreado (`docker rm -f fcv-citas-mysql`, sin `-v`). Los volúmenes del otro grupo se conservan; lo recrean con `docker compose up -d mysql` desde su carpeta.
2. `citas-api/docker-compose.yml` propio: proyecto `jmunoz-citas`, contenedor `jmunoz-citas-mysql`, volumen `jmunoz-citas_mysql_data`, puerto host **3308**, zona `America/Bogota`, utf8mb4.
3. `DB_PORT=3308` en `citas-api/.env.example`, `citas-api/.env` y valor por defecto de `application.yml` (reemplaza la decisión anterior de 3307).
4. Se eliminó el `.env` de la raíz del workspace para que el `docker-compose.yml` de la plantilla no vuelva a levantarse con el proyecto compartido. No usar el compose raíz.
5. `citas-api` escucha en el puerto **8081** (`API_PORT`), porque el 8080 lo ocupa `fcv-citas-api-dev` del otro grupo.
