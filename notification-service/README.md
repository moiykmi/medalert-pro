# notification-service (HU03-05)

Consume la cola `cancelaciones.eventos` (publicada por `fhir-integration-service`) y,
por cada paciente afectado, envía SMS (Twilio) y email (Mailtrap sandbox) con
trazabilidad completa en la tabla `notificacion`.

## Requisitos previos
- El entorno Docker (`docker-compose.dev.yml`) levantado.
- `fhir-integration-service` ya probado y funcionando (HU01-02).
- Tu `.env` completo con las credenciales de Twilio y Mailtrap.

## ⚠️ Importante — restricción de Twilio en modo trial

Mientras tu cuenta de Twilio esté en modo trial (sin tarjeta de crédito agregada),
**solo puede enviar SMS a números que hayas verificado** en Twilio Console →
Phone Numbers → Manage → Verified Caller IDs.

Antes de probar, actualiza `seed-test-data.sql` (o inserta un paciente nuevo) para
que **al menos un paciente use tu propio número de celular verificado**, con
formato internacional completo (`+56912345678`), o el envío de SMS va a fallar
con un error de Twilio del tipo "unverified number".

## 1. Ejecutar el microservicio

En IntelliJ: abre este proyecto como Gradle project (mismo procedimiento que
`fhir-integration-service`) y corre `NotificationServiceApplication`.

Debe levantar en `http://localhost:8082`. Revisa los logs: Flyway debería decir
que el esquema ya está actualizado (porque ya lo aplicaste antes), y no debería
haber errores de conexión a RabbitMQ ni a Postgres.

## 2. Disparar el flujo completo

Con `fhir-integration-service` **también corriendo** (los dos microservicios activos
a la vez, en puertos distintos: 8081 y 8082), llama de nuevo al endpoint de siempre
desde Postman:

```
POST http://localhost:8081/eventos/cancelacion
Content-Type: application/json

{
  "profesionalId": 1,
  "fecha": "2026-08-01",
  "motivo": "Licencia medica",
  "registradoPor": 1
}
```

Esta vez, en vez de quedar el mensaje esperando en RabbitMQ, `notification-service`
lo va a consumir automáticamente en cuanto llegue (si está corriendo).

## 3. Verificar el resultado (Definition of Done)

1. **Logs de `notification-service`:** deberías ver líneas como:
   ```
   Procesando evento 2 — 3 pacientes afectados
   SMS enviado a +56912345678 — SID: SM... — estado: queued
   Email enviado a juan.perez@test.cl — id local: mailtrap-...
   ```
2. **SMS real:** revisa el celular que verificaste en Twilio — debería llegarte el mensaje de cancelación.
3. **Email:** entra a tu inbox de Mailtrap (mailtrap.io → Email Testing → tu sandbox) — deberías ver los correos capturados ahí (no llegan a una bandeja real, es la gracia del sandbox).
4. **Base de datos:**
   ```powershell
   docker exec -it medalert-postgres psql -U medalert -d medalert -c "SELECT paciente_id, canal, estado_envio, proveedor_message_id FROM notificacion ORDER BY id DESC LIMIT 10;"
   ```
   Deberías ver 2 filas por paciente (una SMS, una EMAIL), con `estado_envio = ENVIADO`.

Si ves las 4 confirmaciones, **HU03-05 está funcionalmente completa** — motor de
notificación multicanal con trazabilidad, de punta a punta.

## HU06-08 — WhatsApp y escalamiento automático

### Activar el sandbox de WhatsApp de Twilio (una sola vez)
1. Twilio Console → **Messaging → Try it out → Send a WhatsApp message**
2. Copia el número de sandbox (normalmente `+1 415 523 8886`) y el código `join <palabra-clave>`.
3. Desde tu WhatsApp personal, envía ese mensaje `join <palabra-clave>` al número de sandbox.
4. Confirma que `TWILIO_WHATSAPP_FROM_NUMBER=whatsapp:+14155238886` esté en tu `.env`
   (o déjalo vacío — el `application.yml` ya trae ese valor por defecto).

### Cómo funciona ahora el flujo (cambió respecto a HU03-05)
- El mensaje inicial **ya no se manda por SMS+email a la vez** — se manda solo por
  el `canal_preferido` del paciente (columna en la tabla `paciente`).
- Un scheduler (`EscalacionScheduler`) revisa cada minuto si hay notificaciones
  `ENVIADO` sin confirmar hace más de `medalert.escalacion.minutos-espera` minutos,
  y si es así, envía por el siguiente canal disponible (ciclo SMS → WHATSAPP → EMAIL,
  saltando los ya usados), hasta un máximo de 3 intentos.
- Un paciente "confirma" llamando `POST /notificaciones/{id}/confirmar` — eso detiene
  la escalación para ese paciente.

### Probar la escalación sin esperar 60 minutos
Agrega esto a tu `.env` para que la ventana de espera sea de 1 minuto en vez de 60:
```
ESCALACION_MINUTOS_ESPERA=1
```
(Recuerda volver a subirlo a 60, o quitarlo, cuando ya no estés probando esto —
para que se comporte como especifica tu tesis en producción)

### Prueba paso a paso
1. Resetea las citas a `AGENDADA` y dispara `POST /eventos/cancelacion` como siempre.
2. Revisa los logs — debería mandar **un solo canal** por paciente (el preferido).
3. Espera ~1-2 minutos (con `ESCALACION_MINUTOS_ESPERA=1`) sin confirmar nada.
4. Revisa los logs de nuevo — deberías ver una línea `Escalando paciente X ... intento 2/3`.
5. Verifica en base de datos:
   ```powershell
   docker exec -it medalert-postgres psql -U medalert -d medalert -c "SELECT paciente_id, canal, intento_numero, estado_envio FROM notificacion ORDER BY paciente_id, intento_numero;"
   ```
   Deberías ver 2 (o 3) filas por paciente, con `canal` distinto cada vez.
6. Para probar que la confirmación **detiene** la escalación: toma el `id` de una
   notificación reciente (columna `id`, no confundir con `paciente_id`) y llama:
   ```
   POST http://localhost:8082/notificaciones/{id}/confirmar
   ```
   Espera otro minuto — ese paciente ya no debería escalar más.

## Troubleshooting (HU06-08)

- **Error de Twilio `21608 - The number is unverified`** → el paciente de prueba no
  tiene un número verificado en tu cuenta trial. Ver la advertencia de arriba.
- **Error de conexión SMTP / Authentication failed** → revisa que `MAIL_USERNAME` y
  `MAIL_PASSWORD` en tu `.env` coincidan exactamente con los de tu sandbox de
  Mailtrap (la password se ve truncada en la interfaz — haz click para revelarla completa).
- **El mensaje nunca llega a `notification-service`** → confirma que ambos
  microservicios están corriendo *al mismo tiempo* y que la cola en RabbitMQ
  (`localhost:15672` → Queues → `cancelaciones.eventos`) no tiene mensajes atascados
  de pruebas anteriores sin consumir (puedes purgarla desde la consola si es necesario).
