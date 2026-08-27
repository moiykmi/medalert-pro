-- V4__notificacion_recordatorios.sql
-- Habilita recordatorios preventivos de citas (48h y 24h antes), además de los
-- avisos de cancelación existentes. Un recordatorio no nace de un
-- evento_cancelacion, así que evento_id pasa a ser opcional; tipo distingue
-- el origen de cada fila (CANCELACION es el valor por defecto de las filas
-- existentes y de todo el código que ya escribe en esta tabla sin fijar tipo).
--
-- Nota: numerado V4 (no V2) porque los 3 microservicios comparten la misma
-- base de datos y por lo tanto la misma tabla flyway_schema_history — V2 y V3
-- ya los usó fhir-integration-service para otras migraciones.

ALTER TABLE notificacion
    ALTER COLUMN evento_id DROP NOT NULL,
    ADD COLUMN tipo VARCHAR(30) NOT NULL DEFAULT 'CANCELACION';
    -- tipo: CANCELACION | RECORDATORIO_48H | RECORDATORIO_24H

CREATE INDEX idx_notificacion_cita_tipo ON notificacion(cita_id, tipo);
