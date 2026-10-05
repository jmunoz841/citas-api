-- =====================================================================
-- V10 — Recordatorios enviados por n8n (HU-023, D-041)
--
-- Una fila por cita y horario recordado. La clave (appointment_id, start_at) hace idempotente el
-- registro: WF-001 puede reintentar sin duplicar, y si la cita se reprograma su nuevo horario no
-- tiene fila y vuelve a ser recordable. Solo inserción; no guarda el contenido del correo.
-- =====================================================================

CREATE TABLE appointment_reminders (
  appointment_id  BIGINT UNSIGNED NOT NULL,
  start_at        DATETIME        NOT NULL,
  sent_at         DATETIME(6)     NOT NULL,
  CONSTRAINT pk_appointment_reminders PRIMARY KEY (appointment_id, start_at),
  CONSTRAINT fk_ar_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
