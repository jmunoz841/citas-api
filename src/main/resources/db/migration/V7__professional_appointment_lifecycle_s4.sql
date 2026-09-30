-- S4: el profesional asignado puede cerrar su propia cita y queda identificado en el historial.
ALTER TABLE appointment_status_history DROP CHECK chk_ash_source;
ALTER TABLE appointment_status_history
    MODIFY source VARCHAR(20) NOT NULL,
    ADD CONSTRAINT chk_ash_source CHECK (source IN ('SYSTEM', 'USER', 'ADMIN', 'PROFESSIONAL'));
