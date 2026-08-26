-- V3__profesional_credenciales.sql
-- Habilita el login del portal médico (HU: el profesional reporta su propia
-- ausencia). Ambas columnas son opcionales: un profesional sin email/password
-- configurados simplemente no tiene acceso al portal — lo asigna el personal
-- administrativo vía PUT /profesionales/{id}/credenciales.

ALTER TABLE profesional_salud
    ADD COLUMN email          VARCHAR(150) UNIQUE,
    ADD COLUMN password_hash  VARCHAR(100);
