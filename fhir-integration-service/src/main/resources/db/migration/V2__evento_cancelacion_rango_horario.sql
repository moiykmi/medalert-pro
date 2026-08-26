-- V2__evento_cancelacion_rango_horario.sql
-- Permite registrar la cancelación de un rango de horas específico dentro del día
-- (ej. "09:00 a 12:00") en lugar de cancelar siempre la agenda completa del profesional.
-- Ambas columnas son opcionales: si quedan en NULL, el evento cubre el día completo
-- (comportamiento anterior, sin cambios).

ALTER TABLE evento_cancelacion
    ADD COLUMN hora_inicio TIME,
    ADD COLUMN hora_fin    TIME;
