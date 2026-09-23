-- =====================================================================
-- V3 — EPS, planes y afiliación de usuario (HU-004)
-- Origen: docs/database/normalizacion-3fn/schema.sql (subconjunto de HU-004)
-- La EPS NO se guarda en la afiliación: se deriva de plan_id, así que un plan
-- de otra EPS es imposible por estructura.
-- El régimen es un hecho del afiliado, no del plan (D-019 y decisión del PO
-- del 2026-09-23): por eso vive en user_affiliations y no en eps_plans.
-- Datos 100 % sintéticos: ninguna EPS real de Colombia.
-- =====================================================================

CREATE TABLE eps (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(120)    NOT NULL,
  is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_eps PRIMARY KEY (id),
  CONSTRAINT uk_eps_name UNIQUE (name),
  CONSTRAINT chk_eps_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- El nombre del plan es único dentro de su EPS, no globalmente.
CREATE TABLE eps_plans (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  eps_id      BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(120)    NOT NULL,
  is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_eps_plans PRIMARY KEY (id),
  CONSTRAINT uk_eps_plans_eps_name UNIQUE (eps_id, name),
  CONSTRAINT chk_eps_plans_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) > 0),
  CONSTRAINT fk_eps_plans_eps FOREIGN KEY (eps_id)
    REFERENCES eps (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_affiliations (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  plan_id      BIGINT UNSIGNED NOT NULL,
  regime_code  VARCHAR(20)     NOT NULL,
  created_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_user_affiliations PRIMARY KEY (id),
  CONSTRAINT uk_user_affiliations_combo UNIQUE (user_id, plan_id, regime_code),
  KEY idx_user_affiliations_plan (plan_id),
  KEY idx_user_affiliations_regime (regime_code),
  CONSTRAINT fk_user_affiliations_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_user_affiliations_plan FOREIGN KEY (plan_id)
    REFERENCES eps_plans (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_user_affiliations_regime FOREIGN KEY (regime_code)
    REFERENCES regimes (code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed sintético. El CRUD administrativo llega con HU-007 (S4); hasta entonces
-- estos son los únicos valores seleccionables. Un plan inactivo a propósito, para
-- poder demostrar la regla "solo planes activos".
INSERT INTO eps (name, is_active) VALUES
  ('EPS Salud Sintética',   TRUE),
  ('EPS Vida Laboratorio',  TRUE),
  ('EPS Prueba Inactiva',   FALSE);

INSERT INTO eps_plans (eps_id, name, is_active) VALUES
  ((SELECT id FROM eps WHERE name = 'EPS Salud Sintética'),  'Plan Básico',       TRUE),
  ((SELECT id FROM eps WHERE name = 'EPS Salud Sintética'),  'Plan Complementario', TRUE),
  ((SELECT id FROM eps WHERE name = 'EPS Vida Laboratorio'), 'Plan Esencial',     TRUE),
  ((SELECT id FROM eps WHERE name = 'EPS Vida Laboratorio'), 'Plan Familiar',     TRUE),
  ((SELECT id FROM eps WHERE name = 'EPS Vida Laboratorio'), 'Plan Descontinuado', FALSE),
  ((SELECT id FROM eps WHERE name = 'EPS Prueba Inactiva'),  'Plan Suspendido',   TRUE);
