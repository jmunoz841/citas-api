-- =====================================================================
-- V9 — Reprogramación de citas (HU-018, HU-019; bandeja HU-022)
-- Origen: docs/database/normalizacion-3fn/schema.sql (sección 6), parte que V6 dejó fuera.
--
-- Claves del diseño:
--   * reschedule_requests guarda SNAPSHOTS de la franja original y de la solicitada: al
--     aprobar, appointments se sobrescribe y la solicitud conserva de dónde venía.
--   * uk_rr_one_pending_per_appointment (índice funcional): como máximo una solicitud
--     PENDING por cita (D-033, HU-018 CA-04), también ante dos peticiones simultáneas.
--   * slot_reservations pasa a tener titular exclusivo: la cita (franja vigente) XOR la
--     solicitud PENDING (franja retenida). La PK slot_id sigue impidiendo que dos titulares
--     ocupen el mismo slot. Aprobar = borrar las filas de la cita y pasar a la cita las de la
--     solicitud, en una transacción (HU-019 CA-01).
--   * MySQL prohíbe acciones referenciales en FK cuyas columnas participan en un CHECK; por
--     eso fk_sr_appointment se recrea sin ON DELETE/ON UPDATE (equivale a RESTRICT).
-- =====================================================================

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
  CONSTRAINT uk_rr_id_appointment UNIQUE (id, appointment_id),
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

-- Titular exclusivo de cada slot ocupado.
ALTER TABLE slot_reservations DROP FOREIGN KEY fk_sr_appointment;

ALTER TABLE slot_reservations
  MODIFY appointment_id BIGINT UNSIGNED NULL,
  ADD COLUMN reschedule_request_id BIGINT UNSIGNED NULL AFTER appointment_id,
  ADD KEY idx_sr_reschedule_request (reschedule_request_id);

ALTER TABLE slot_reservations
  ADD CONSTRAINT fk_sr_appointment FOREIGN KEY (appointment_id, professional_id)
    REFERENCES appointments (id, professional_id),
  ADD CONSTRAINT fk_sr_reschedule_request FOREIGN KEY (reschedule_request_id)
    REFERENCES reschedule_requests (id);

ALTER TABLE slot_reservations
  ADD CONSTRAINT chk_sr_single_holder CHECK ((appointment_id IS NULL) XOR (reschedule_request_id IS NULL));

-- El registro de historial que mueve la cita queda enlazado con la solicitud aprobada; la FK
-- compuesta impide que apunte a una solicitud de otra cita.
ALTER TABLE appointment_status_history
  ADD COLUMN reschedule_request_id BIGINT UNSIGNED NULL AFTER reason,
  ADD KEY idx_ash_reschedule_appointment (reschedule_request_id, appointment_id),
  ADD CONSTRAINT fk_ash_reschedule_request FOREIGN KEY (reschedule_request_id, appointment_id)
    REFERENCES reschedule_requests (id, appointment_id) ON DELETE RESTRICT ON UPDATE RESTRICT;
