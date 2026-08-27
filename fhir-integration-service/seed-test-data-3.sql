-- seed-test-data-3.sql
-- Ejecutar en el editor SQL del servicio Postgres de Railway (Data / Query tab).
-- Reemplaza a seed-test-data-2.sql (nunca se ejecutó — este script no lo asume
-- corrido). Ids ya usados hoy: establecimiento=1, profesional_salud=1 (Dra.
-- Fuentes), paciente=1 (Juan Pérez), 2 (María López), 3 (Pedro Soto) — no se
-- vuelven a insertar.

-- ═══════════════════════════════════════════════════════════════════
-- 1) CITA DE PRUEBA PARA RECORDATORIOS — paciente real (Juan Pérez, id 1,
--    tu propio teléfono/email verificados en Twilio). Fechada 28 de agosto
--    para que la corrida de recordatorios de mañana (27 ago 09:00 hora
--    Chile) la tome como recordatorio de 24h. Bórrala después si no la
--    quieres mezclada con datos reales.
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO cita (paciente_id, profesional_id, fecha_hora, estado)
VALUES (1, 1, '2026-08-28 10:00:00', 'AGENDADA');

-- ═══════════════════════════════════════════════════════════════════
-- 2) Dos profesionales adicionales
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO profesional_salud (nombre, especialidad, establecimiento_id)
VALUES
    ('Dr. Ignacio Reyes', 'Pediatría', 1),
    ('Dra. Camila Vidal', 'Medicina Familiar', 1);

-- ═══════════════════════════════════════════════════════════════════
-- 3) 10 pacientes nuevos. RUTs con dígito verificador válido. Todos con
--    números ficticios (no reciben SMS/WhatsApp real) — salvo que
--    reemplaces alguno por un número tuyo verificado en Twilio.
-- ═══════════════════════════════════════════════════════════════════
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

-- ═══════════════════════════════════════════════════════════════════
-- 4) Citas AGENDADA repartidas entre el 27 de agosto y el 3 de septiembre,
--    con los pacientes nuevos + los 2 ya existentes (María, Pedro) por RUT
--    (no se vuelven a insertar). Cubre varios días de corridas de
--    recordatorios (28-ago a 3-sep) además de material para "Registrar
--    cancelación".
-- ═══════════════════════════════════════════════════════════════════
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
    ('33333333-3', 'Dra. Camila Vidal', '2026-08-30 09:00:00'::timestamp),
    ('12345678-5', 'Dra. Fuentes',      '2026-09-01 09:00:00'::timestamp),
    ('22222222-2', 'Dr. Ignacio Reyes', '2026-09-01 09:30:00'::timestamp),
    ('33333333-3', 'Dra. Camila Vidal', '2026-09-01 10:00:00'::timestamp),
    ('44444444-4', 'Dra. Fuentes',      '2026-09-03 09:00:00'::timestamp),
    ('55555555-5', 'Dr. Ignacio Reyes', '2026-09-03 09:30:00'::timestamp)
) AS v(rut, prof_nombre, fecha_hora)
JOIN paciente p ON p.rut = v.rut
JOIN profesional_salud prof ON prof.nombre = v.prof_nombre;
