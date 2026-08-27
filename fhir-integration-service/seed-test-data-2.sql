-- seed-test-data-2.sql
-- Datos adicionales para poblar el ambiente de Railway con más volumen.
-- Ejecutar en el editor SQL del servicio Postgres de Railway (Data / Query tab).
-- Asume que ya existe establecimiento id=1 y profesional_salud id=1 (Dra. Fuentes)
-- del seed original — no los vuelve a insertar.

-- Dos profesionales adicionales
INSERT INTO profesional_salud (nombre, especialidad, establecimiento_id)
VALUES
    ('Dr. Ignacio Reyes', 'Pediatría', 1),
    ('Dra. Camila Vidal', 'Medicina Familiar', 1);

-- 10 pacientes nuevos. RUTs con dígito verificador válido.
-- Todos con números de teléfono ficticios (no van a recibir SMS/WhatsApp real),
-- salvo que reemplaces alguno por un número tuyo verificado en Twilio.
INSERT INTO paciente (rut, nombre, telefono, email, canal_preferido)
VALUES
    ('44444444-4', 'Carla Muñoz',    '+56944444444', 'carla.munoz@test.cl',    'SMS'),
    ('55555555-5', 'Diego Fuentes',  '+56955555555', 'diego.fuentes@test.cl',  'EMAIL'),
    ('66666666-6', 'Valentina Rojas','+56966666666', 'valentina.rojas@test.cl','SMS'),
    ('77777777-7', 'Matías Torres',  '+56977777777', 'matias.torres@test.cl',  'WHATSAPP'),
    ('88888888-8', 'Javiera Silva',  '+56988888888', 'javiera.silva@test.cl',  'SMS'),
    ('99999999-9', 'Benjamín Castro','+56999999999', 'benjamin.castro@test.cl','EMAIL'),
    ('20000000-5', 'Antonia Reyes',  '+56920000000', 'antonia.reyes@test.cl',  'SMS'),
    ('30000000-2', 'Tomás Herrera',  '+56930000002', 'tomas.herrera@test.cl',  'WHATSAPP'),
    ('40000000-K', 'Fernanda López', '+56940000000', 'fernanda.lopez@test.cl', 'SMS'),
    ('12345678-5', 'Cristóbal Vega', '+56912345670', 'cristobal.vega@test.cl', 'EMAIL');

-- 15 citas AGENDADA repartidas en varias fechas y profesionales,
-- para tener material real con el que seguir probando "Registrar cancelación".
-- (paciente_id/profesional_id calculados por rut/nombre para no depender de ids fijos)
INSERT INTO cita (paciente_id, profesional_id, fecha_hora, estado)
SELECT p.id, prof.id, v.fecha_hora, 'AGENDADA'
FROM (VALUES
    ('44444444-4', 'Dra. Fuentes',      '2026-08-27 09:00:00'::timestamp),
    ('55555555-5', 'Dra. Fuentes',      '2026-08-27 09:30:00'::timestamp),
    ('66666666-6', 'Dra. Fuentes',      '2026-08-27 10:00:00'::timestamp),
    ('77777777-7', 'Dr. Ignacio Reyes', '2026-08-28 09:00:00'::timestamp),
    ('88888888-8', 'Dr. Ignacio Reyes', '2026-08-28 09:30:00'::timestamp),
    ('99999999-9', 'Dr. Ignacio Reyes', '2026-08-28 10:00:00'::timestamp),
    ('20000000-5', 'Dra. Camila Vidal', '2026-08-29 09:00:00'::timestamp),
    ('30000000-2', 'Dra. Camila Vidal', '2026-08-29 09:30:00'::timestamp),
    ('40000000-K', 'Dra. Camila Vidal', '2026-08-29 10:00:00'::timestamp),
    ('12345678-5', 'Dra. Fuentes',      '2026-09-01 09:00:00'::timestamp),
    ('11111111-1', 'Dr. Ignacio Reyes', '2026-09-01 09:30:00'::timestamp),
    ('22222222-2', 'Dra. Camila Vidal', '2026-09-01 10:00:00'::timestamp),
    ('33333333-3', 'Dra. Fuentes',      '2026-09-03 09:00:00'::timestamp),
    ('44444444-4', 'Dr. Ignacio Reyes', '2026-09-03 09:30:00'::timestamp),
    ('55555555-5', 'Dra. Camila Vidal', '2026-09-03 10:00:00'::timestamp)
) AS v(rut, prof_nombre, fecha_hora)
JOIN paciente p ON p.rut = v.rut
JOIN profesional_salud prof ON prof.nombre = v.prof_nombre;
