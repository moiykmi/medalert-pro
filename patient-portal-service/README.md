# patient-portal-service (HU09-11)

Backend del portal de pacientes: autenticación RUT + OTP, consulta de citas, y
confirmación de notificaciones. Puerto **8083**.

## Requisitos previos
- Entorno Docker levantado (incluye Redis, que este servicio usa por primera vez).
- Al menos un paciente cargado en la tabla `paciente` (del `seed-test-data.sql`).
- Tu `.env` con las mismas credenciales de Postgres, Twilio, Mailtrap ya usadas
  en los otros microservicios (Redis no necesita credenciales en este entorno dev).

## Cómo funciona

1. **`POST /auth/solicitar-otp`** — el paciente ingresa su RUT. El sistema genera
   un código de 6 dígitos, lo guarda en Redis por 5 minutos, y lo envía por SMS
   o email según el `canal_preferido` del paciente.
2. **`POST /auth/verificar-otp`** — el paciente ingresa el código recibido. Si es
   correcto, el sistema entrega un `token` de sesión (válido 30 min).
3. Con ese token, el paciente puede llamar (header `Authorization: Bearer <token>`):
   - **`GET /citas`** — sus citas.
   - **`GET /notificaciones`** — sus notificaciones recibidas.
   - **`POST /notificaciones/{id}/confirmar`** — confirma una notificación (detiene
     el escalamiento automático en `notification-service`, porque ambos leen y
     escriben la misma tabla `notificacion`).

## Prueba paso a paso (Postman)

### 1. Solicitar OTP
```
POST http://localhost:8083/auth/solicitar-otp
Content-Type: application/json

{ "rut": "11111111-1" }
```
Respuesta: `200 OK` sin cuerpo. **Revisa los logs de `patient-portal-service`** —
por comodidad de desarrollo, el código OTP se imprime ahí también (línea
`OTP generado para RUT ... : 123456`), así no dependes de que el SMS/email
llegue para poder seguir probando.

### 2. Verificar OTP
```
POST http://localhost:8083/auth/verificar-otp
Content-Type: application/json

{ "rut": "11111111-1", "codigo": "123456" }
```
Respuesta esperada:
```json
{ "token": "a1b2c3d4-...", "pacienteId": 1, "nombre": "Juan Pérez" }
```
**Copia el `token`** — lo necesitas para los siguientes pasos.

### 3. Consultar citas
```
GET http://localhost:8083/citas
Authorization: Bearer a1b2c3d4-...
```
Debería devolver las citas del paciente 1.

### 4. Consultar notificaciones
```
GET http://localhost:8083/notificaciones
Authorization: Bearer a1b2c3d4-...
```
Debería devolver el historial de notificaciones (si ya disparaste algún evento
de cancelación antes).

### 5. Confirmar una notificación
```
POST http://localhost:8083/notificaciones/{id}/confirmar
Authorization: Bearer a1b2c3d4-...
```
Debería devolver la notificación con `estadoEnvio: "CONFIRMADO"`.

## Casos de error a probar (para tu informe/demo)
- **OTP incorrecto** → `401 Unauthorized`.
- **OTP correcto pero después de 5 minutos** → `401` (expiró en Redis).
- **Reutilizar el mismo OTP dos veces** → falla la segunda vez (de un solo uso).
- **Llamar `/citas` sin header Authorization** → `401`.
- **Confirmar una notificación de otro paciente** → `403 Forbidden`.

## HU12-13 — Reagendamiento y actualización de datos de contacto

### Reagendar una cita cancelada
```
POST http://localhost:8083/citas/{id}/reagendar
Authorization: Bearer {token}
Content-Type: application/json

{ "nuevaFechaHora": "2026-08-15T10:00:00" }
```
Reglas:
- La cita debe estar en estado `CANCELADA` (si no, devuelve `409 Conflict`).
- La cita debe pertenecer al paciente autenticado (si no, `403 Forbidden`).
- La nueva fecha debe ser futura (validación automática).
- Al reagendar: se crea una cita nueva en estado `AGENDADA`, la original pasa a
  `REAGENDADA`, y queda un registro en la tabla `reagendamiento` vinculando ambas.

**Limitación conocida (documentar en el informe):** como RAS no expone horarios
disponibles reales, el paciente propone la fecha libremente — el sistema no
valida contra la disponibilidad real del profesional. Esto queda como trabajo
futuro condicionado a una integración real con RAS.

### Actualizar datos de contacto
```
PUT http://localhost:8083/paciente/datos-contacto
Authorization: Bearer {token}
Content-Type: application/json

{ "telefono": "+56987654321", "email": "nuevo@correo.cl", "canalPreferido": "WHATSAPP" }
```
Puedes enviar solo los campos que quieras cambiar (los demás quedan igual).
Actualiza automáticamente `datos_actualizados_en`.

### Ver el perfil propio
```
GET http://localhost:8083/paciente/perfil
Authorization: Bearer {token}
```

### Prueba paso a paso
1. Resetea una cita a `CANCELADA` para poder probar el reagendamiento:
   ```powershell
   docker exec -it medalert-postgres psql -U medalert -d medalert -c "UPDATE cita SET estado = 'CANCELADA' WHERE id = 1;"
   ```
2. Llama a `POST /citas/1/reagendar` con una fecha futura.
3. Verifica en base de datos:
   ```powershell
   docker exec -it medalert-postgres psql -U medalert -d medalert -c "SELECT id, estado FROM cita ORDER BY id DESC LIMIT 5;"
   docker exec -it medalert-postgres psql -U medalert -d medalert -c "SELECT * FROM reagendamiento ORDER BY id DESC LIMIT 5;"
   ```
   Deberías ver la cita original en `REAGENDADA`, una cita nueva en `AGENDADA`,
   y una fila en `reagendamiento` conectando ambas.
4. Prueba `PUT /paciente/datos-contacto` y confirma con `GET /paciente/perfil`
   que los datos cambiaron.

## Troubleshooting
- **`Connection refused` a Redis** → confirma que el contenedor `medalert-redis`
  esté corriendo (`docker ps`) y que el puerto 6379 esté mapeado.
- **`RUT no registrado` (404)** → revisa que el RUT tenga exactamente el mismo
  formato que en la base de datos (`11111111-1`, con guion).
- **El OTP no llega por SMS/email pero sí aparece en el log** → normal en este
  entorno de desarrollo si Twilio/Mailtrap tienen alguna restricción puntual
  (mismas causas ya vistas en `notification-service`); usa el código del log
  para seguir probando sin bloquearte.
