# Decisiones aprobadas — Repositorio de la raíz del workspace

- **Fecha:** 2026-09-18
- **Aprobado por:** Juan Muñoz
- **Contexto:** poder trabajar desde otro PC con todo el workspace (insumos del trainer, prompts, skills, agentes y `AGENTS.md` orquestador).

## Decisiones

1. La raíz se sube a un repo nuevo **privado** `github.com/jmunoz841/FCV_Proyecto_Citas_v1`. Privado porque `RESTRICCIONES_TECNICAS.md` limita a dos los repos públicos por estudiante y porque contiene la solución de BD del trainer.
2. Se incluye `database/reference/db.sql` (la plantilla lo excluía por `.gitignore`), solo por ser privado.
3. `citas-api/` y `citas-web/` se añaden al `.gitignore` de la raíz y dejan de rastrearse allí; se clonan por separado dentro de la carpeta raíz.
4. El remoto original del trainer (`juancarlosfc5/FCV_Proyecto_Citas_v1`) se conserva como `upstream`; el historial no se reescribe.
5. Reemplaza la decisión anterior de no versionar la raíz (D-005).
