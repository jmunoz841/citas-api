# Decisiones aprobadas — Workspace y entorno (S2)

- **Fecha:** 2026-09-16
- **Aprobado por:** Juan Muñoz

## Decisiones

1. **Raíz del workspace.** La carpeta raíz es un clon de la plantilla del trainer (`juancarlosfc5/FCV_Proyecto_Citas_v1`). No se hacen commits en ella. En el equipo local, `citas-api/` y `citas-web/` se excluyen mediante `.git/info/exclude`, y los 17 archivos que la plantilla ya rastreaba se marcan con `git update-index --skip-worktree`. Reversible con `git update-index --no-skip-worktree`.
2. **Puerto MySQL.** `docker-compose.yml` publica MySQL en el puerto 3307 del host. `citas-api/.env.example` usa `DB_HOST=localhost` y `DB_PORT=3307` para ejecutar Spring Boot en el host. Dentro de la red Docker (`citas-api-dev`) se usa `mysql:3306`.
