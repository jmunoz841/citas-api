-- =====================================================================
-- V1 — Identidad y sesión JWT (HU-001)
-- Origen: docs/database/normalizacion-3fn/schema.sql (subconjunto de HU-001)
-- Decisiones: D-003 (refresh token rotado y guardado como hash),
--             D-007 (America/Bogota), D-008 (tipos de documento),
--             D-012 (password_hash VARCHAR(255)).
-- No crea usuarios: el primer ADMIN se define con HU-008.
-- =====================================================================

-- Catálogo fijo de roles.
CREATE TABLE roles (
  code  VARCHAR(20) NOT NULL,
  name  VARCHAR(60) NOT NULL,
  CONSTRAINT pk_roles PRIMARY KEY (code),
  CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Catálogo fijo de tipos de documento de identidad.
CREATE TABLE document_types (
  code  VARCHAR(10) NOT NULL,
  name  VARCHAR(60) NOT NULL,
  CONSTRAINT pk_document_types PRIMARY KEY (code),
  CONSTRAINT uk_document_types_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Cuentas. Email único sin distinguir mayúsculas (collation as_ci);
-- documento único por tipo + número; solo se guarda el hash de la contraseña.
CREATE TABLE users (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  first_names         VARCHAR(100)    NOT NULL,
  last_names          VARCHAR(100)    NOT NULL,
  document_type_code  VARCHAR(10)     NOT NULL,
  document_number     VARCHAR(30)     NOT NULL,
  email               VARCHAR(254)    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci NOT NULL,
  phone               VARCHAR(20)     NOT NULL,
  password_hash       VARCHAR(255)    NOT NULL,
  is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uk_users_email UNIQUE (email),
  CONSTRAINT uk_users_document UNIQUE (document_type_code, document_number),
  CONSTRAINT chk_users_first_names_not_blank CHECK (CHAR_LENGTH(TRIM(first_names)) > 0),
  CONSTRAINT chk_users_last_names_not_blank  CHECK (CHAR_LENGTH(TRIM(last_names)) > 0),
  CONSTRAINT chk_users_password_hash_len     CHECK (CHAR_LENGTH(password_hash) >= 60),
  CONSTRAINT fk_users_document_type FOREIGN KEY (document_type_code)
    REFERENCES document_types (code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Relación N:M usuario-rol.
CREATE TABLE user_roles (
  user_id      BIGINT UNSIGNED NOT NULL,
  role_code    VARCHAR(20)     NOT NULL,
  assigned_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_code),
  KEY idx_user_roles_role (role_code),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code)
    REFERENCES roles (code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Refresh tokens: solo el hash SHA-256 en hex minúsculas. Rotación: el token
-- usado recibe revoked_at y apunta a su reemplazo (replaced_by_token_id).
CREATE TABLE refresh_tokens (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id               BIGINT UNSIGNED NOT NULL,
  token_hash            CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  issued_at             DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at            DATETIME(6)     NOT NULL,
  revoked_at            DATETIME(6)     NULL,
  replaced_by_token_id  BIGINT UNSIGNED NULL,
  CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
  CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
  CONSTRAINT uk_refresh_tokens_replaced_by UNIQUE (replaced_by_token_id),
  KEY idx_refresh_tokens_user_expires (user_id, expires_at),
  CONSTRAINT chk_refresh_tokens_expiry  CHECK (expires_at > issued_at),
  CONSTRAINT chk_refresh_tokens_revoked CHECK (revoked_at IS NULL OR revoked_at >= issued_at),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_token_id)
    REFERENCES refresh_tokens (id) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seeds de catálogos fijos.
INSERT INTO roles (code, name) VALUES
  ('USER',         'Usuario'),
  ('PROFESSIONAL', 'Profesional'),
  ('ADMIN',        'Administrador');

INSERT INTO document_types (code, name) VALUES
  ('CC',  'Cédula de ciudadanía'),
  ('CE',  'Cédula de extranjería'),
  ('TI',  'Tarjeta de identidad'),
  ('RC',  'Registro civil'),
  ('PA',  'Pasaporte'),
  ('PPT', 'Permiso por protección temporal');
