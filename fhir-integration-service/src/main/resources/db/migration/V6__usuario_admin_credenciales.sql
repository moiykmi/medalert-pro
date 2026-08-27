-- V6__usuario_admin_credenciales.sql
-- Habilita el login individual del personal administrativo (además del
-- X-Admin-Token compartido, que sigue funcionando como acceso maestro sin
-- cambios). Sin password_hash, un usuario_admin simplemente no puede usar el
-- login individual — el token compartido lo sigue cubriendo.
--
-- Numerado V6 (los 3 microservicios comparten flyway_schema_history: V2/V3
-- de fhir-integration-service, V4/V5 de notification-service).

ALTER TABLE usuario_admin
    ADD COLUMN password_hash VARCHAR(100);
