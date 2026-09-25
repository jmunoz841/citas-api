-- =====================================================================
-- V4 — Oferta administrable: especialidades y profesionales (HU-006, HU-008, HU-009)
-- Origen: docs/database/normalizacion-3fn/schema.sql (subconjunto de HU-006 y HU-008)
--
-- Dos índices funcionales hacen cumplir en la BD reglas que normalmente se dejan
-- a la aplicación:
--   uk_specialties_single_general      -> como máximo UNA especialidad general
--   uk_prof_specialties_one_primary    -> como máximo UNA primaria por profesional
-- Los NULL no colisionan en un índice único, así que el CASE deja fuera las filas
-- que no son generales o no son primarias.
--
-- "Al menos una especialidad" y "al menos una sede" no se pueden expresar con una
-- restricción declarativa: son invariantes de aplicación (ver ProfessionalService).
-- =====================================================================

CREATE TABLE specialties (
  id                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
  name              VARCHAR(120)      NOT NULL,
  duration_minutes  SMALLINT UNSIGNED NOT NULL,
  is_general        BOOLEAN           NOT NULL DEFAULT FALSE,
  is_active         BOOLEAN           NOT NULL DEFAULT TRUE,
  created_at        DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_specialties PRIMARY KEY (id),
  CONSTRAINT uk_specialties_name UNIQUE (name),
  UNIQUE INDEX uk_specialties_single_general ((CASE WHEN is_general = 1 THEN 1 ELSE NULL END)),
  CONSTRAINT chk_specialties_duration CHECK (duration_minutes IN (30, 60)),
  CONSTRAINT chk_specialties_is_general_bool CHECK (is_general IN (0, 1)),
  CONSTRAINT chk_specialties_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Subtipo 1:0..1 de users: un profesional ES un usuario, con datos propios.
CREATE TABLE professionals (
  user_id            BIGINT UNSIGNED NOT NULL,
  professional_code  VARCHAR(30)     NOT NULL,
  license_number     VARCHAR(30)     NOT NULL,
  is_active          BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_professionals PRIMARY KEY (user_id),
  CONSTRAINT uk_professionals_code UNIQUE (professional_code),
  CONSTRAINT uk_professionals_license UNIQUE (license_number),
  CONSTRAINT fk_professionals_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE professional_specialties (
  professional_id  BIGINT UNSIGNED NOT NULL,
  specialty_id     BIGINT UNSIGNED NOT NULL,
  is_primary       BOOLEAN         NOT NULL DEFAULT FALSE,
  is_active        BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_professional_specialties PRIMARY KEY (professional_id, specialty_id),
  UNIQUE INDEX uk_prof_specialties_one_primary (professional_id, (CASE WHEN is_primary = 1 THEN 1 ELSE NULL END)),
  KEY idx_prof_specialties_specialty (specialty_id, professional_id),
  CONSTRAINT chk_prof_specialties_is_primary_bool CHECK (is_primary IN (0, 1)),
  CONSTRAINT fk_prof_specialties_professional FOREIGN KEY (professional_id)
    REFERENCES professionals (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_prof_specialties_specialty FOREIGN KEY (specialty_id)
    REFERENCES specialties (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE professional_sites (
  professional_id  BIGINT UNSIGNED NOT NULL,
  site_code        VARCHAR(10)     NOT NULL,
  is_active        BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_professional_sites PRIMARY KEY (professional_id, site_code),
  KEY idx_prof_sites_site (site_code, professional_id),
  CONSTRAINT fk_prof_sites_professional FOREIGN KEY (professional_id)
    REFERENCES professionals (user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_prof_sites_site FOREIGN KEY (site_code)
    REFERENCES sites (code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Especialidad general del PRD: la única con is_general = TRUE (supuesto S-09).
INSERT INTO specialties (name, duration_minutes, is_general, is_active) VALUES
  ('Medicina General', 30, TRUE, TRUE);

-- ---------------------------------------------------------------------
-- ADMIN inicial (D-021). Sin él nadie podría crear el primer profesional.
-- Credenciales de laboratorio, documentadas en README.md: deben cambiarse antes
-- de cualquier despliegue. La contraseña se guarda solo como hash BCrypt.
-- ---------------------------------------------------------------------
INSERT INTO users (first_names, last_names, document_type_code, document_number, email, phone,
                   password_hash, is_active)
VALUES ('Admin', 'Laboratorio', 'CC', '100000001', 'admin@citas.local', '3000000001',
        '$2a$10$BOgVdK.GBLAZzpXHyJ/.1OBrNLP7wgwsLkUJjffKqJ2cLeJNkwn8i', TRUE);

INSERT INTO user_roles (user_id, role_code)
SELECT id, 'ADMIN' FROM users WHERE email = 'admin@citas.local';
