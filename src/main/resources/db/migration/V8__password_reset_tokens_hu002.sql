-- HU-002: tokens de recuperación de contraseña. Solo se guarda SHA-256, nunca el valor claro.
CREATE TABLE password_reset_tokens (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at DATETIME(6) NOT NULL,
  used_at    DATETIME(6) NULL,
  revoked_at DATETIME(6) NULL,
  CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
  CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
  KEY idx_password_reset_tokens_user_expires (user_id, expires_at),
  CONSTRAINT chk_prt_expiry CHECK (expires_at > created_at),
  CONSTRAINT chk_prt_used CHECK (used_at IS NULL OR used_at >= created_at),
  CONSTRAINT chk_prt_single_end CHECK (used_at IS NULL OR revoked_at IS NULL),
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
