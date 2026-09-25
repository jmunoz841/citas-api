-- =====================================================================
-- V6 — Citas, ocupación de slots e historial de estados (HU-012, HU-013, HU-014)
-- Origen: docs/database/normalizacion-3fn/schema.sql (subconjunto de la sección 6)
--
-- Claves del diseño:
--   * slot_reservations tiene PRIMARY KEY (slot_id): una fila por slot ocupado.
--     Dos citas no pueden ocupar el mismo slot porque la segunda inserción falla con
--     el error 1062 de MySQL. La doble reserva la impide la base, no la aplicación,
--     y es segura ante concurrencia (HU-013 CA-03).
--   * fk_sr_slot es compuesta (slot_id, professional_id): el slot ocupado pertenece al
--     mismo profesional de la cita. fk_sr_appointment cierra el triángulo.
--   * fk_appt_professional_specialty y fk_appt_professional_site garantizan en la base
--     que el profesional atiende esa especialidad y está asignado a esa sede (RN-07, RN-08).
--   * duration_minutes es un SNAPSHOT de la especialidad; end_at es columna generada.
--   * La FK RESTRICT de slot_reservations hacia availability_slots es la última
--     defensa de HU-010 CA-05: no se borran los slots de un bloque con citas.
--
-- Subconjunto respecto al diseño 3FN:
--   * Sin reschedule_requests (reprogramación, fuera de S3). Por eso la cita es el
--     único titular posible y slot_reservations.appointment_id es NOT NULL. La
--     migración de reprogramación lo relajará y añadirá el CHECK de titular exclusivo.
--   * Sin los triggers de inmutabilidad del historial (riesgo de
--     log_bin_trust_function_creators, Q-10): la aplicación solo inserta en
--     appointment_status_history y nunca actualiza ni borra.
--
-- "No en el pasado" (RN-06) vive en la aplicación: MySQL no admite NOW() en un CHECK.
-- =====================================================================

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
  version           INT UNSIGNED      NOT NULL DEFAULT 0,
  created_at        DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at        DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_appointments PRIMARY KEY (id),
  CONSTRAINT uk_appointments_id_professional UNIQUE (id, professional_id),
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

-- Ocupación de slots: UNA fila por slot reservado (APPROVED) o retenido (REQUESTED).
-- Liberar un slot = DELETE de su fila (HU-015 al rechazar).
CREATE TABLE slot_reservations (
  slot_id          BIGINT UNSIGNED NOT NULL,
  professional_id  BIGINT UNSIGNED NOT NULL,
  appointment_id   BIGINT UNSIGNED NOT NULL,
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_slot_reservations PRIMARY KEY (slot_id),
  KEY idx_sr_slot_professional (slot_id, professional_id),
  KEY idx_sr_appointment_professional (appointment_id, professional_id),
  CONSTRAINT fk_sr_slot FOREIGN KEY (slot_id, professional_id)
    REFERENCES availability_slots (id, professional_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_sr_appointment FOREIGN KEY (appointment_id, professional_id)
    REFERENCES appointments (id, professional_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Historial de estados de cita (RF-19, RN-12). Solo inserción.
CREATE TABLE appointment_status_history (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  appointment_id   BIGINT UNSIGNED NOT NULL,
  status_code      VARCHAR(20)     NOT NULL,
  source           VARCHAR(10)     NOT NULL,
  actor_user_id    BIGINT UNSIGNED NULL,
  reason           VARCHAR(500)    NULL,
  changed_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_appointment_status_history PRIMARY KEY (id),
  KEY idx_ash_appointment_changed (appointment_id, changed_at),
  KEY idx_ash_status_changed (status_code, changed_at),
  KEY idx_ash_actor (actor_user_id),
  CONSTRAINT chk_ash_source CHECK (source IN ('SYSTEM', 'USER', 'ADMIN')),
  CONSTRAINT chk_ash_actor_required CHECK (source = 'SYSTEM' OR actor_user_id IS NOT NULL),
  CONSTRAINT chk_ash_rejection_reason CHECK (status_code <> 'REJECTED' OR (reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0)),
  CONSTRAINT fk_ash_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_ash_status FOREIGN KEY (status_code)
    REFERENCES appointment_statuses (code) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_ash_actor FOREIGN KEY (actor_user_id)
    REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
