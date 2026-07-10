-- seed-test-data.sql
-- Datos mínimos para poder probar POST /eventos/cancelacion de punta a punta.
-- Esto NO es una migración Flyway — ejecútalo aparte, solo en tu ambiente de desarrollo.

INSERT INTO establecimiento (nombre, servicio_salud)
VALUES ('Consultorio Tomás Rojas Vergara', 'Servicio de Salud Los Ríos');

INSERT INTO usuario_admin (nombre, email, rol, establecimiento_id)
VALUES ('Admin de prueba', 'admin@medalertpro.test', 'PERSONAL_ADMINISTRATIVO', 1);

INSERT INTO profesional_salud (nombre, especialidad, establecimiento_id)
VALUES ('Dra. Fuentes', 'Medicina General', 1);

-- Reemplaza el teléfono y email por los tuyos si quieres probar el envío real más adelante (Sprint 1, Paso 3)
INSERT INTO paciente (rut, nombre, telefono, email, canal_preferido)
VALUES
    ('11111111-1', 'Juan Pérez', '+56911111111', 'juan.perez@test.cl', 'SMS'),
    ('22222222-2', 'María López', '+56922222222', 'maria.lopez@test.cl', 'EMAIL'),
    ('33333333-3', 'Pedro Soto', '+56933333333', 'pedro.soto@test.cl', 'WHATSAPP');

-- 3 citas AGENDADAS con la Dra. Fuentes (profesional_id=1) para el 2026-08-01
INSERT INTO cita (paciente_id, profesional_id, fecha_hora, estado)
VALUES
    (1, 1, '2026-08-01 09:00:00', 'AGENDADA'),
    (2, 1, '2026-08-01 09:30:00', 'AGENDADA'),
    (3, 1, '2026-08-01 10:00:00', 'AGENDADA');
