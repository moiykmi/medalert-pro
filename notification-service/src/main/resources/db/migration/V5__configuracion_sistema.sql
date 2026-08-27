-- V5__configuracion_sistema.sql
-- Configuración global editable desde el admin (pantalla "Configuración" del
-- mockup): habilitar/deshabilitar canales de notificación y los dos tipos de
-- recordatorio preventivo. Fila única (id=1) — no hay multi-tenant todavía.
--
-- Numerado V5: los 3 microservicios comparten flyway_schema_history (V1 en los
-- tres, V2/V3 de fhir-integration-service, V4 de notification-service).

CREATE TABLE configuracion_sistema (
    id                            BIGINT PRIMARY KEY,
    canal_sms_habilitado          BOOLEAN NOT NULL DEFAULT true,
    canal_whatsapp_habilitado     BOOLEAN NOT NULL DEFAULT true,
    canal_email_habilitado        BOOLEAN NOT NULL DEFAULT true,
    recordatorio_48h_habilitado   BOOLEAN NOT NULL DEFAULT true,
    recordatorio_24h_habilitado   BOOLEAN NOT NULL DEFAULT true,
    actualizado_en                TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO configuracion_sistema (id) VALUES (1);
