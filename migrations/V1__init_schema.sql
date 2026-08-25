-- V1__init_schema.sql
-- MedAlert Pro — Esquema inicial de base de datos
-- Este es el modelo de datos PROPIO de MedAlert Pro (no confundir con los recursos FHIR
-- del servidor mock, que solo se usan como disparador simulado del evento de cancelación).

CREATE TABLE establecimiento (
                                 id              BIGSERIAL PRIMARY KEY,
                                 nombre          VARCHAR(150) NOT NULL,
                                 servicio_salud  VARCHAR(150) NOT NULL,
                                 created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE usuario_admin (
                               id                  BIGSERIAL PRIMARY KEY,
                               nombre              VARCHAR(150) NOT NULL,
                               email               VARCHAR(150) NOT NULL UNIQUE,
                               rol                 VARCHAR(50)  NOT NULL, -- ADMIN | PERSONAL_ADMINISTRATIVO | AUDITORIA
                               establecimiento_id  BIGINT NOT NULL REFERENCES establecimiento(id),
                               activo              BOOLEAN NOT NULL DEFAULT true,
                               created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE profesional_salud (
                                   id              BIGSERIAL PRIMARY KEY,
                                   nombre          VARCHAR(150) NOT NULL,
                                   especialidad    VARCHAR(100) NOT NULL,
                                   establecimiento_id BIGINT NOT NULL REFERENCES establecimiento(id),
                                   created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE paciente (
                          id                          BIGSERIAL PRIMARY KEY,
                          rut                         VARCHAR(12) NOT NULL UNIQUE,
                          nombre                      VARCHAR(150) NOT NULL,
                          telefono                    VARCHAR(20),
                          email                       VARCHAR(150),
                          canal_preferido             VARCHAR(20) NOT NULL DEFAULT 'SMS', -- SMS | WHATSAPP | EMAIL
                          datos_actualizados_en       TIMESTAMP,
                          adulto_mayor                BOOLEAN NOT NULL DEFAULT false,
                          created_at                  TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at                  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE cita (
                      id                  BIGSERIAL PRIMARY KEY,
                      paciente_id         BIGINT NOT NULL REFERENCES paciente(id),
                      profesional_id      BIGINT NOT NULL REFERENCES profesional_salud(id),
                      fecha_hora          TIMESTAMP NOT NULL,
                      estado              VARCHAR(30) NOT NULL DEFAULT 'AGENDADA', -- AGENDADA | CANCELADA | REAGENDADA | ATENDIDA | NO_ASISTIO
                      origen_ras_id       VARCHAR(100), -- id del recurso Appointment en el mock FHIR (si aplica)
                      created_at          TIMESTAMP NOT NULL DEFAULT now(),
                      updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE evento_cancelacion (
                                    id                  BIGSERIAL PRIMARY KEY,
                                    profesional_id      BIGINT NOT NULL REFERENCES profesional_salud(id),
                                    fecha_evento        TIMESTAMP NOT NULL DEFAULT now(),
                                    motivo              VARCHAR(200),
                                    estado              VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO', -- REGISTRADO | PROCESANDO | COMPLETADO
                                    registrado_por      BIGINT REFERENCES usuario_admin(id),
                                    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE notificacion (
                              id                          BIGSERIAL PRIMARY KEY,
                              evento_id                   BIGINT NOT NULL REFERENCES evento_cancelacion(id),
                              paciente_id                 BIGINT NOT NULL REFERENCES paciente(id),
                              cita_id                     BIGINT REFERENCES cita(id),
                              canal                       VARCHAR(20) NOT NULL, -- SMS | WHATSAPP | EMAIL
                              intento_numero               SMALLINT NOT NULL DEFAULT 1,
                              estado_envio                VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE | ENVIADO | FALLIDO | LEIDO | CONFIRMADO
                              proveedor_message_id        VARCHAR(150), -- id devuelto por Twilio/SendGrid, útil para debugging
                              enviado_en                  TIMESTAMP,
                              confirmado_en                TIMESTAMP,
                              created_at                  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reagendamiento (
                                id                  BIGSERIAL PRIMARY KEY,
                                cita_original_id    BIGINT NOT NULL REFERENCES cita(id),
                                cita_nueva_id        BIGINT REFERENCES cita(id),
                                paciente_id         BIGINT NOT NULL REFERENCES paciente(id),
                                fecha_solicitud      TIMESTAMP NOT NULL DEFAULT now(),
                                estado               VARCHAR(30) NOT NULL DEFAULT 'SOLICITADO' -- SOLICITADO | CONFIRMADO | RECHAZADO
);

-- Índices para las consultas más frecuentes del motor de notificaciones
CREATE INDEX idx_cita_profesional_fecha ON cita(profesional_id, fecha_hora);
CREATE INDEX idx_notificacion_evento ON notificacion(evento_id);
CREATE INDEX idx_notificacion_estado ON notificacion(estado_envio);
CREATE INDEX idx_paciente_rut ON paciente(rut);