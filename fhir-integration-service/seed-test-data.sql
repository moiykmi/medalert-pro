-- seed-test-data.sql
-- Datos mínimos para poder probar POST /eventos/cancelacion de punta a punta.
-- Esto NO es una migración Flyway — ejecútalo aparte, solo en tu ambiente de desarrollo.

INSERT INTO establecimiento (nombre, servicio_salud)
VALUES ('Consultorio Tomás Rojas Vergara', 'Servicio de Salud Los Ríos');

INSERT INTO usuario_admin (nombre, email, rol, establecimiento_id)
VALUES ('Admin de prueba', 'admin@medalertpro.test', 'PERSONAL_ADMINISTRATIVO', 1);

INSERT INTO profesional_salud (nombre, especialidad, establecimiento_id)
VALUES ('Dra. Fuentes', 'Medicina General', 1);

-- IMPORTANTE: reemplaza TU_NUMERO_VERIFICADO por tu número real verificado en Twilio
-- (formato internacional completo, ej: +56912345678) antes de ejecutar este archivo.
-- Los otros dos pacientes se quedan con números falsos — no van a recibir SMS real,
-- solo sirven para probar que el flujo maneja varios pacientes a la vez.
INSERT INTO paciente (rut, nombre, telefono, email, canal_preferido)
VALUES
    ('11111111-1', 'Juan Pérez', '+56934812424', 'moises.valencia.f@gmail.com', 'SMS'),
    ('22222222-2', 'María López', '+56930565365', 'csalgadobachman@gmail.com', 'EMAIL'),
    ('33333333-3', 'Pedro Soto', '+56933333333', 'pedro.soto@test.cl', 'WHATSAPP');

-- 3 citas AGENDADAS con la Dra. Fuentes (profesional_id=1) para el 2026-08-01
INSERT INTO cita (paciente_id, profesional_id, fecha_hora, estado)
VALUES
    (1, 1, '2026-08-01 09:00:00', 'AGENDADA'),
    (2, 1, '2026-08-01 09:30:00', 'AGENDADA'),
    (3, 1, '2026-08-01 10:00:00', 'AGENDADA');