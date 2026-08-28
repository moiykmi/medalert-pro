-- V8__notificacion_estado_entrega.sql
-- Estado real de entrega reportado por Twilio (SMS/WhatsApp) vía webhook de
-- status callback, distinto de estado_envio (que solo refleja si NUESTRO
-- intento de envío tuvo éxito de forma síncrona). Un mensaje puede quedar
-- estado_envio=ENVIADO (Twilio lo aceptó) y aun así fallar la entrega real
-- (ej. WhatsApp sandbox sin sesión activa) — sin esto, esa falla es invisible.
--
-- Numerado V8: los 3 microservicios comparten flyway_schema_history
-- (V1 en los tres, V2/V3/V6 de fhir-integration-service, V4/V5/V7 de
-- notification-service).

ALTER TABLE notificacion
    ADD COLUMN estado_entrega VARCHAR(30),
    ADD COLUMN entregado_en   TIMESTAMP;
