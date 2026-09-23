-- =====================================================================
-- Modelo relacional PROPIO (diseño 3FN) — Sistema de Agendamiento de Citas
-- Motor objetivo : MySQL 8.4 LTS · InnoDB · utf8mb4 / utf8mb4_0900_ai_ci
-- Naturaleza     : documento de DISEÑO. NO es una migración Flyway.
--                  No contiene datos personales ni contraseñas.
-- Documentación  : README.md, erd.md, dependencias-funcionales.md,
--                  normalizacion-1fn-3fn.md, decisiones-de-diseno.md
--
-- Convenciones
--  * Catálogos FIJOS (seed, solo lectura): PK natural `code` inmutable.
--    Permite CHECKs sobre códigos (p. ej. REJECTED exige motivo).
--  * Catálogos CONFIGURABLES y entidades: PK sustituta BIGINT UNSIGNED.
--  * Fechas/horas de agenda y auditoría: DATETIME en hora civil
--    America/Bogota (UTC-5, sin horario de verano)  -> SUPUESTO S-03.
--  * FKs cuyas columnas participan en un CHECK se declaran SIN cláusula
--    ON DELETE / ON UPDATE: MySQL prohíbe acciones referenciales
--    (CASCADE / SET NULL) sobre columnas usadas en CHECK. El comportamiento
--    por defecto en InnoDB es NO ACTION (equivalente a RESTRICT).
--  * MySQL 8.4 (restrict_fk_on_non_standard_key=ON): toda FK referencia
--    una PRIMARY KEY o UNIQUE con exactamente esas columnas.
--  * Un CHECK no puede referenciar columnas AUTO_INCREMENT ni funciones
--    no deterministas (NOW()): "no en el pasado" es regla de aplicación.
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 1. CATÁLOGOS FIJOS (seed, solo lectura)
-- =====================================================================

-- Roles de autorización (N:M con usuarios).
CREATE TABLE roles (
  code  VARCHAR(20) NOT NULL,
  name  VARCHAR(60) NOT NULL,
  CONSTRAINT pk_roles PRIMARY KEY (code),
  CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Tipos de documento de identidad (SUPUESTO S-01: catálogo fijo).
CREATE TABLE document_types (
  code  VARCHAR(10) NOT NULL,
  name  VARCHAR(60) NOT NULL,
  CONSTRAINT pk_document_types PRIMARY KEY (code),
  CONSTRAINT uk_document_types_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Regímenes de afiliación (valores: SUPUESTO S-02).
CREATE TABLE regimes (
  code  VARCHAR(20) NOT NULL,
  name  VARCHAR(60) NOT NULL,
  CONSTRAINT pk_regimes PRIMARY KEY (code),
  CONSTRAINT uk_regimes_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Sedes fijas del laboratorio (HIC, ICV). Dirección tratada como valor
-- atómico (no se consulta por partes) -> SUPUESTO S-25.
CREATE TABLE sites (
  code     VARCHAR(10)  NOT NULL,
  name     VARCHAR(120) NOT NULL,
  address  VARCHAR(255) NOT NULL,
  CONSTRAINT pk_sites PRIMARY KEY (code),
  CONSTRAINT uk_sites_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Estados de cita. is_terminal: la cita ya no admite transiciones.
CREATE TABLE appointment_statuses (
  code         VARCHAR(20) NOT NULL,
  name         VARCHAR(60) NOT NULL,
  is_terminal  BOOLEAN     NOT NULL,
  CONSTRAINT pk_appointment_statuses PRIMARY KEY (code),
  CONSTRAINT uk_appointment_statuses_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Estados de solicitud de reprogramación.
CREATE TABLE reschedule_statuses (
  code         VARCHAR(20) NOT NULL,
  name         VARCHAR(60) NOT NULL,
  is_terminal  BOOLEAN     NOT NULL,
  CONSTRAINT pk_reschedule_statuses PRIMARY KEY (code),
  CONSTRAINT uk_reschedule_statuses_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================
-- 2. IDENTIDAD Y ACCESO
-- =====================================================================

-- Usuario (paciente, profesional o administrador; los roles van aparte).
-- email: collation acento-sensible / mayúsculas-insensible -> la UNIQUE
-- rechaza "Ana@X.com" si existe "ana@x.com" (HECHO HU-001 CA-03).
CREATE TABLE users (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  first_names         VARCHAR(100)    NOT NULL,
  last_names          VARCHAR(100)    NOT NULL,
  document_type_code  VARCHAR(10)     NOT NULL,
  document_number     VARCHAR(30)     NOT NULL,
  email               VARCHAR(254)    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci NOT NULL,
  phone               VARCHAR(20)     NOT NULL,
  password_hash       VARCHAR(255)    NOT NULL,  -- BCrypt (60) o Argon2 con prefijo {id} (D-012)
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

-- Refresh tokens: solo el hash (SHA-256 hex, SUPUESTO S-07). Rotación:
-- el token usado recibe revoked_at y apunta a su reemplazo.
-- replaced_by_token_id usa ON DELETE SET NULL, por eso NO participa en CHECK.
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

-- Tokens de recuperación de contraseña: temporales y de un solo uso (hash).
CREATE TABLE password_reset_tokens (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  token_hash  CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at  DATETIME(6)     NOT NULL,
  used_at     DATETIME(6)     NULL,  -- consumido al cambiar la contraseña
  revoked_at  DATETIME(6)     NULL,  -- invalidado por una solicitud posterior (SUPUESTO S-26)
  CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
  CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
  KEY idx_password_reset_tokens_user_expires (user_id, expires_at),
  CONSTRAINT chk_prt_expiry   CHECK (expires_at > created_at),
  CONSTRAINT chk_prt_used     CHECK (used_at IS NULL OR used_at >= created_at),
  CONSTRAINT chk_prt_single_end CHECK (used_at IS NULL OR revoked_at IS NULL),
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================
-- 3. CATÁLOGOS CONFIGURABLES Y AFILIACIÓN
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

-- Un plan pertenece a exactamente una EPS; nombre único dentro de su EPS.
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

-- Afiliación del usuario. La EPS NO se almacena: se deriva de plan_id
-- (plan_id -> eps_id). Así "el plan pertenece a la EPS" es imposible de violar.
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

-- Especialidades con duración 30/60. is_general marca "Medicina General"
-- (auto-aprobación). Índice funcional: como máximo UNA especialidad general.
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

-- =====================================================================
-- 4. PROFESIONALES (subtipo 1:0..1 de users) Y ASIGNACIONES N:M
-- =====================================================================

CREATE TABLE professionals (
  user_id            BIGINT UNSIGNED NOT NULL,
  professional_code  VARCHAR(30)     NOT NULL,
  license_number     VARCHAR(30)     NOT NULL,  -- matrícula ficticia
  is_active          BOOLEAN         NOT NULL DEFAULT TRUE,
  created_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_professionals PRIMARY KEY (user_id),
  CONSTRAINT uk_professionals_code UNIQUE (professional_code),
  CONSTRAINT uk_professionals_license UNIQUE (license_number),
  CONSTRAINT fk_professionals_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- N:M profesional-especialidad. Máximo una primaria por profesional
-- (índice funcional: los NULL no colisionan). "Al menos una" = aplicación.
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

-- N:M profesional-sede (habilitación para publicar agenda, RN-07).
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

-- =====================================================================
-- 5. AGENDA: BLOQUES Y SLOTS MATERIALIZADOS
-- =====================================================================

-- Bloque de disponibilidad. La FK compuesta a professional_sites garantiza
-- que el profesional está asignado a la sede (RN-07).
CREATE TABLE availability_blocks (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  professional_id  BIGINT UNSIGNED NOT NULL,
  site_code        VARCHAR(10)     NOT NULL,
  start_at         DATETIME        NOT NULL,
  end_at           DATETIME        NOT NULL,
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_availability_blocks PRIMARY KEY (id),
  CONSTRAINT uk_blocks_id_professional UNIQUE (id, professional_id),          -- destino de FK compuesta
  CONSTRAINT uk_blocks_professional_start UNIQUE (professional_id, start_at),
  KEY idx_blocks_professional_site (professional_id, site_code),
  KEY idx_blocks_site_start (site_code, start_at),
  CONSTRAINT chk_blocks_range     CHECK (end_at > start_at),
  CONSTRAINT chk_blocks_same_day  CHECK (end_at <= TIMESTAMP(DATE(start_at)) + INTERVAL 1 DAY),
  CONSTRAINT chk_blocks_start_aligned CHECK (MINUTE(start_at) IN (0, 30) AND SECOND(start_at) = 0),
  CONSTRAINT chk_blocks_end_aligned   CHECK (MINUTE(end_at) IN (0, 30) AND SECOND(end_at) = 0),
  CONSTRAINT fk_blocks_professional_site FOREIGN KEY (professional_id, site_code)
    REFERENCES professional_sites (professional_id, site_code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Slot de 30 min. professional_id es redundancia CONTROLADA (FK compuesta al
-- bloque) que permite UNIQUE(professional_id, start_at): impide a nivel de BD
-- bloques solapados del mismo profesional, incluso en sedes distintas.
-- La sede NO se repite aquí (se obtiene del bloque).
CREATE TABLE availability_slots (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  block_id         BIGINT UNSIGNED NOT NULL,
  professional_id  BIGINT UNSIGNED NOT NULL,
  start_at         DATETIME        NOT NULL,
  CONSTRAINT pk_availability_slots PRIMARY KEY (id),
  CONSTRAINT uk_slots_id_professional UNIQUE (id, professional_id),           -- destino de FK compuesta
  CONSTRAINT uk_slots_professional_start UNIQUE (professional_id, start_at),
  KEY idx_slots_block_professional (block_id, professional_id),
  CONSTRAINT chk_slots_start_aligned CHECK (MINUTE(start_at) IN (0, 30) AND SECOND(start_at) = 0),
  CONSTRAINT fk_slots_block FOREIGN KEY (block_id, professional_id)
    REFERENCES availability_blocks (id, professional_id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================
-- 6. CITAS, REPROGRAMACIONES, RESERVAS DE SLOTS E HISTORIAL
-- =====================================================================

-- Cita. start_at/site_code son hechos propios de la cita (sobreviven a la
-- liberación de slots). duration_minutes es SNAPSHOT de la especialidad.
-- end_at es columna generada (sin anomalía de actualización posible).
-- status_code = estado ACTUAL (redundancia declarada frente al historial).
CREATE TABLE appointments (
  id                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
  patient_user_id   BIGINT UNSIGNED   NOT NULL,
  professional_id   BIGINT UNSIGNED   NOT NULL,
  specialty_id      BIGINT UNSIGNED   NOT NULL,
  site_code         VARCHAR(10)       NOT NULL,
  start_at          DATETIME          NOT NULL,
  duration_minutes  SMALLINT UNSIGNED NOT NULL,
  end_at            DATETIME GENERATED ALWAYS AS (start_at + INTERVAL duration_minutes MINUTE) STORED NOT NULL,
  status_code       VARCHAR(20)       NOT NULL,
  version           INT UNSIGNED      NOT NULL DEFAULT 0,  -- bloqueo optimista (JPA @Version)
  created_at        DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_appointments PRIMARY KEY (id),
  CONSTRAINT uk_appointments_id_professional UNIQUE (id, professional_id),     -- destino de FK compuesta
  KEY idx_appt_professional_start (professional_id, start_at),
  KEY idx_appt_patient_start (patient_user_id, start_at),
  KEY idx_appt_status_start (status_code, start_at),
  KEY idx_appt_site_start (site_code, start_at),
  KEY idx_appt_specialty_start (specialty_id, start_at),
  KEY idx_appt_professional_specialty (professional_id, specialty_id),
  KEY idx_appt_professional_site (professional_id, site_code),
  CONSTRAINT chk_appt_duration CHECK (duration_minutes IN (30, 60)),
  CONSTRAINT chk_appt_start_aligned CHECK (MINUTE(start_at) IN (0, 30) AND SECOND(start_at) = 0),
  CONSTRAINT fk_appt_patient FOREIGN KEY (patient_user_id)
    REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_appt_professional_specialty FOREIGN KEY (professional_id, specialty_id)
    REFERENCES professional_specialties (professional_id, specialty_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_appt_professional_site FOREIGN KEY (professional_id, site_code)
    REFERENCES professional_sites (professional_id, site_code) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_appt_status FOREIGN KEY (status_code)
    REFERENCES appointment_statuses (code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Solicitud de reprogramación. original_* y requested_* son SNAPSHOTS
-- (la franja original se sobrescribe en appointments al aprobar).
-- Índice funcional: como máximo una solicitud PENDING por cita.
-- status_code, decided_by_user_id participan en CHECK -> FK sin acciones.
CREATE TABLE reschedule_requests (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  appointment_id       BIGINT UNSIGNED NOT NULL,
  original_start_at    DATETIME        NOT NULL,
  original_site_code   VARCHAR(10)     NOT NULL,
  requested_start_at   DATETIME        NOT NULL,
  requested_site_code  VARCHAR(10)     NOT NULL,
  status_code          VARCHAR(20)     NOT NULL,
  decided_by_user_id   BIGINT UNSIGNED NULL,
  decided_at           DATETIME(6)     NULL,
  decision_reason      VARCHAR(500)    NULL,
  version              INT UNSIGNED    NOT NULL DEFAULT 0,
  created_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_reschedule_requests PRIMARY KEY (id),
  CONSTRAINT uk_rr_id_appointment UNIQUE (id, appointment_id),                -- destino de FK compuesta
  UNIQUE INDEX uk_rr_one_pending_per_appointment ((CASE WHEN status_code = 'PENDING' THEN appointment_id ELSE NULL END)),
  KEY idx_rr_appointment_created (appointment_id, created_at),
  KEY idx_rr_status_requested_start (status_code, requested_start_at),
  KEY idx_rr_decided_by (decided_by_user_id),
  KEY idx_rr_original_site (original_site_code),
  KEY idx_rr_requested_site (requested_site_code),
  CONSTRAINT chk_rr_start_aligned CHECK (MINUTE(requested_start_at) IN (0, 30) AND SECOND(requested_start_at) = 0),
  CONSTRAINT chk_rr_changes_time CHECK (requested_start_at <> original_start_at),
  CONSTRAINT chk_rr_pending_undecided CHECK (status_code <> 'PENDING' OR (decided_at IS NULL AND decided_by_user_id IS NULL)),
  CONSTRAINT chk_rr_admin_decision CHECK (status_code NOT IN ('APPROVED', 'REJECTED') OR (decided_at IS NOT NULL AND decided_by_user_id IS NOT NULL)),
  CONSTRAINT chk_rr_cancelled_closed CHECK (status_code <> 'CANCELLED' OR decided_at IS NOT NULL),
  CONSTRAINT chk_rr_rejection_reason CHECK (status_code <> 'REJECTED' OR (decision_reason IS NOT NULL AND CHAR_LENGTH(TRIM(decision_reason)) > 0)),
  CONSTRAINT fk_rr_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id),
  CONSTRAINT fk_rr_status FOREIGN KEY (status_code)
    REFERENCES reschedule_statuses (code),
  CONSTRAINT fk_rr_decided_by FOREIGN KEY (decided_by_user_id)
    REFERENCES users (id),
  CONSTRAINT fk_rr_original_site FOREIGN KEY (original_site_code)
    REFERENCES sites (code) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_rr_requested_site FOREIGN KEY (requested_site_code)
    REFERENCES sites (code) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Ocupación de slots: UNA fila por slot ocupado (reservado o retenido).
-- PK = slot_id  => imposible que dos citas/solicitudes ocupen el mismo slot
-- (garantía de BD, segura ante concurrencia: la 2.ª inserción falla con 1062).
-- Titular exclusivo: la cita (franja vigente) XOR la reprogramación PENDING
-- (franja retenida provisional). Liberar = DELETE de la fila.
-- appointment_id / reschedule_request_id participan en CHECK -> FK sin acciones.
CREATE TABLE slot_reservations (
  slot_id                BIGINT UNSIGNED NOT NULL,
  professional_id        BIGINT UNSIGNED NOT NULL,
  appointment_id         BIGINT UNSIGNED NULL,
  reschedule_request_id  BIGINT UNSIGNED NULL,
  created_at             DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_slot_reservations PRIMARY KEY (slot_id),
  KEY idx_sr_slot_professional (slot_id, professional_id),
  KEY idx_sr_appointment_professional (appointment_id, professional_id),
  KEY idx_sr_reschedule_request (reschedule_request_id),
  CONSTRAINT chk_sr_single_holder CHECK ((appointment_id IS NULL) XOR (reschedule_request_id IS NULL)),
  CONSTRAINT fk_sr_slot FOREIGN KEY (slot_id, professional_id)
    REFERENCES availability_slots (id, professional_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_sr_appointment FOREIGN KEY (appointment_id, professional_id)
    REFERENCES appointments (id, professional_id),
  CONSTRAINT fk_sr_reschedule_request FOREIGN KEY (reschedule_request_id)
    REFERENCES reschedule_requests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Historial inmutable de estados de cita (RF-19, RN-12).
-- reschedule_request_id enlaza el registro generado al aprobar una
-- reprogramación; la FK compuesta impide que apunte a otra cita.
CREATE TABLE appointment_status_history (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  appointment_id         BIGINT UNSIGNED NOT NULL,
  status_code            VARCHAR(20)     NOT NULL,
  source                 VARCHAR(10)     NOT NULL,
  actor_user_id          BIGINT UNSIGNED NULL,
  reason                 VARCHAR(500)    NULL,
  reschedule_request_id  BIGINT UNSIGNED NULL,
  changed_at             DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_appointment_status_history PRIMARY KEY (id),
  KEY idx_ash_appointment_changed (appointment_id, changed_at),
  KEY idx_ash_status_changed (status_code, changed_at),
  KEY idx_ash_actor (actor_user_id),
  KEY idx_ash_reschedule_appointment (reschedule_request_id, appointment_id),
  CONSTRAINT chk_ash_source CHECK (source IN ('SYSTEM', 'USER', 'ADMIN')),
  CONSTRAINT chk_ash_actor_required CHECK (source = 'SYSTEM' OR actor_user_id IS NOT NULL),
  CONSTRAINT chk_ash_rejection_reason CHECK (status_code <> 'REJECTED' OR (reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0)),
  CONSTRAINT fk_ash_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_ash_status FOREIGN KEY (status_code)
    REFERENCES appointment_statuses (code),
  CONSTRAINT fk_ash_actor FOREIGN KEY (actor_user_id)
    REFERENCES users (id),
  CONSTRAINT fk_ash_reschedule_request FOREIGN KEY (reschedule_request_id, appointment_id)
    REFERENCES reschedule_requests (id, appointment_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================
-- 7. INMUTABILIDAD DEL HISTORIAL (defensa en profundidad, opcional)
--    Cuerpo de una sola sentencia: no requiere DELIMITER.
--    Riesgo: con binlog activo, un usuario sin SUPER puede necesitar
--    log_bin_trust_function_creators=1 para crear triggers (ver README, Q-10).
--    Alternativa/complemento: GRANT solo SELECT, INSERT sobre esta tabla.
-- =====================================================================

CREATE TRIGGER trg_ash_block_update
  BEFORE UPDATE ON appointment_status_history
  FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'appointment_status_history es inmutable: UPDATE no permitido';

CREATE TRIGGER trg_ash_block_delete
  BEFORE DELETE ON appointment_status_history
  FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'appointment_status_history es inmutable: DELETE no permitido';

-- =====================================================================
-- 8. SEEDS DE CATÁLOGOS FIJOS (sin datos personales ni contraseñas)
-- =====================================================================

INSERT INTO roles (code, name) VALUES
  ('USER',         'Usuario'),
  ('PROFESSIONAL', 'Profesional'),
  ('ADMIN',        'Administrador');

-- SUPUESTO S-01: tipos de documento colombianos habituales.
INSERT INTO document_types (code, name) VALUES
  ('CC',  'Cédula de ciudadanía'),
  ('CE',  'Cédula de extranjería'),
  ('TI',  'Tarjeta de identidad'),
  ('RC',  'Registro civil'),
  ('PA',  'Pasaporte'),
  ('PPT', 'Permiso por protección temporal');

-- SUPUESTO S-02: valores de régimen (confirmar si aplica régimen especial).
INSERT INTO regimes (code, name) VALUES
  ('CONTRIBUTIVO', 'Contributivo'),
  ('SUBSIDIADO',   'Subsidiado');

-- HECHO (PRD §3): sedes fijas con sus direcciones.
INSERT INTO sites (code, name, address) VALUES
  ('HIC', 'Hospital Internacional de Colombia',
          'Km 7 Autopista Bucaramanga–Piedecuesta, Valle de Menzulí, Santander'),
  ('ICV', 'Fundación Cardiovascular de Colombia / Instituto Cardiovascular',
          'Calle 155A No. 23-58, Urbanización El Bosque, Floridablanca, Santander');

-- HECHO (PRD RF-11..RF-17): estados de cita.
INSERT INTO appointment_statuses (code, name, is_terminal) VALUES
  ('REQUESTED', 'Solicitada',   FALSE),
  ('APPROVED',  'Aprobada',     FALSE),
  ('REJECTED',  'Rechazada',    TRUE),
  ('CANCELLED', 'Cancelada',    TRUE),
  ('COMPLETED', 'Atendida',     TRUE),
  ('NO_SHOW',   'Inasistencia', TRUE);

-- HECHO: PENDING/APPROVED/REJECTED. SUPUESTO S-15: CANCELLED.
INSERT INTO reschedule_statuses (code, name, is_terminal) VALUES
  ('PENDING',   'Pendiente', FALSE),
  ('APPROVED',  'Aprobada',  TRUE),
  ('REJECTED',  'Rechazada', TRUE),
  ('CANCELLED', 'Cancelada', TRUE);

-- Semilla mínima de catálogo CONFIGURABLE requerida por RF-11 / HU-006.
-- SUPUESTO S-09: duración 30 min y marca is_general.
INSERT INTO specialties (name, duration_minutes, is_general, is_active) VALUES
  ('Medicina General', 30, TRUE, TRUE);
