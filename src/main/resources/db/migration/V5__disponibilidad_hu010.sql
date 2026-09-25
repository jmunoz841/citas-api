-- =====================================================================
-- V5 — Bloques de disponibilidad y slots (HU-010)
-- Origen: docs/database/normalizacion-3fn/schema.sql (subconjunto de HU-010)
--
-- Claves del diseño:
--   * Los slots se materializan al crear el bloque: una fila por cada 30 minutos.
--     Así la ocupación puede ser una fila con PK por slot (V6, HU-013) y la doble
--     reserva la impide la base, no la aplicación.
--   * uk_slots_professional_start impide que dos bloques del mismo profesional se
--     solapen, INCLUSO en sedes distintas: al estar todo alineado a :00/:30, dos
--     bloques solapados comparten al menos un inicio de slot (CA-03).
--   * fk_blocks_professional_site es compuesta: garantiza en la base que el
--     profesional está asignado a esa sede (CA-04, RN-07).
--   * availability_slots.professional_id es una redundancia controlada: es el
--     precio de poder tener esas dos restricciones compuestas. La FK compuesta
--     hacia el bloque impide que diverja.
--
-- "No en el pasado" (RN-06) no se puede expresar con un CHECK, porque MySQL no
-- admite funciones no deterministas como NOW(): vive en AvailabilityService.
-- =====================================================================

CREATE TABLE availability_blocks (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  professional_id  BIGINT UNSIGNED NOT NULL,
  site_code        VARCHAR(10)     NOT NULL,
  start_at         DATETIME        NOT NULL,
  end_at           DATETIME        NOT NULL,
  created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_availability_blocks PRIMARY KEY (id),
  CONSTRAINT uk_blocks_id_professional UNIQUE (id, professional_id),
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

CREATE TABLE availability_slots (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  block_id         BIGINT UNSIGNED NOT NULL,
  professional_id  BIGINT UNSIGNED NOT NULL,
  start_at         DATETIME        NOT NULL,
  CONSTRAINT pk_availability_slots PRIMARY KEY (id),
  CONSTRAINT uk_slots_id_professional UNIQUE (id, professional_id),
  CONSTRAINT uk_slots_professional_start UNIQUE (professional_id, start_at),
  KEY idx_slots_block_professional (block_id, professional_id),
  CONSTRAINT chk_slots_start_aligned CHECK (MINUTE(start_at) IN (0, 30) AND SECOND(start_at) = 0),
  CONSTRAINT fk_slots_block FOREIGN KEY (block_id, professional_id)
    REFERENCES availability_blocks (id, professional_id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
