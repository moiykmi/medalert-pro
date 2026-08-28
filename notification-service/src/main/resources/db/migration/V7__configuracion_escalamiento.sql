-- V7__configuracion_escalamiento.sql
-- Hace configurable desde el admin lo que hoy es fijo en EscalacionScheduler:
-- el tiempo de espera antes de escalar de canal y el máximo de intentos.
-- Default 60/3 preserva el comportamiento actual (antes venía de
-- medalert.escalacion.minutos-espera en application.yml).
--
-- Numerado V7: los 3 microservicios comparten flyway_schema_history
-- (V1 en los tres, V2/V3/V6 de fhir-integration-service, V4/V5 de notification-service).

ALTER TABLE configuracion_sistema
    ADD COLUMN escalacion_minutos_espera INTEGER NOT NULL DEFAULT 60,
    ADD COLUMN escalacion_max_intentos   INTEGER NOT NULL DEFAULT 3;
