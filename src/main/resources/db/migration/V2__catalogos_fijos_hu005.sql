-- =====================================================================
-- V2 — Catálogos fijos (HU-005)
-- Origen: docs/database/normalizacion-3fn/schema.sql (subconjunto de HU-005)
-- Los roles y los tipos de documento ya se sembraron en V1 (HU-001).
-- Solo lectura desde la API: no hay endpoints de escritura (CA-03).
-- =====================================================================

-- Régimen de afiliación en salud. Valores del supuesto S-02 del diseño 3FN.
CREATE TABLE regimes (
  code  VARCHAR(20) NOT NULL,
  name  VARCHAR(60) NOT NULL,
  CONSTRAINT pk_regimes PRIMARY KEY (code),
  CONSTRAINT uk_regimes_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Sedes fijas del PRD: HIC e ICV. La dirección es atómica (supuesto S-25).
CREATE TABLE sites (
  code     VARCHAR(10)  NOT NULL,
  name     VARCHAR(120) NOT NULL,
  address  VARCHAR(255) NOT NULL,
  CONSTRAINT pk_sites PRIMARY KEY (code),
  CONSTRAINT uk_sites_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Estados del ciclo de vida de una cita. is_terminal marca los que no admiten transición.
CREATE TABLE appointment_statuses (
  code         VARCHAR(20) NOT NULL,
  name         VARCHAR(60) NOT NULL,
  is_terminal  BOOLEAN     NOT NULL,
  CONSTRAINT pk_appointment_statuses PRIMARY KEY (code),
  CONSTRAINT uk_appointment_statuses_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Estados de una solicitud de reprogramación (se usan desde HU-018).
CREATE TABLE reschedule_statuses (
  code         VARCHAR(20) NOT NULL,
  name         VARCHAR(60) NOT NULL,
  is_terminal  BOOLEAN     NOT NULL,
  CONSTRAINT pk_reschedule_statuses PRIMARY KEY (code),
  CONSTRAINT uk_reschedule_statuses_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seeds de catálogos fijos.
INSERT INTO regimes (code, name) VALUES
  ('CONTRIBUTIVO', 'Contributivo'),
  ('SUBSIDIADO',   'Subsidiado');

INSERT INTO sites (code, name, address) VALUES
  ('HIC', 'Hospital Internacional de Colombia',
          'Km 7 Autopista Bucaramanga–Piedecuesta, Valle de Menzulí, Santander'),
  ('ICV', 'Fundación Cardiovascular de Colombia / Instituto Cardiovascular',
          'Calle 155A No. 23-58, Urbanización El Bosque, Floridablanca, Santander');

INSERT INTO appointment_statuses (code, name, is_terminal) VALUES
  ('REQUESTED', 'Solicitada',   FALSE),
  ('APPROVED',  'Aprobada',     FALSE),
  ('REJECTED',  'Rechazada',    TRUE),
  ('CANCELLED', 'Cancelada',    TRUE),
  ('COMPLETED', 'Atendida',     TRUE),
  ('NO_SHOW',   'Inasistencia', TRUE);

INSERT INTO reschedule_statuses (code, name, is_terminal) VALUES
  ('PENDING',   'Pendiente', FALSE),
  ('APPROVED',  'Aprobada',  TRUE),
  ('REJECTED',  'Rechazada', TRUE),
  ('CANCELLED', 'Cancelada', TRUE);
