# fhir-integration-service (HU01-02)

Microservicio que registra un evento de cancelación y publica el listado de pacientes
afectados en RabbitMQ, para que `notification-service` (Sprint 1, Paso 3) dispare el
envío multicanal.

## Requisitos
- Java 17
- Maven 3.9+
- El entorno de `docker-compose.dev.yml` (carpeta raíz del proyecto) levantado y con
  el esquema aplicado (ver README principal).

## 1. Cargar datos de prueba
Con el entorno Docker levantado (Postgres accesible en `localhost:5432`):
```powershell
docker cp seed-test-data.sql medalert-postgres:/seed-test-data.sql
docker exec -it medalert-postgres psql -U medalert -d medalert -f /seed-test-data.sql
```

## 2. Compilar y ejecutar el microservicio
```bash
mvn spring-boot:run
```
Debería levantar en `http://localhost:8081`. En los logs deberías ver a Flyway
confirmando que el esquema ya está aplicado (`Schema "public" is up to date`).

## 3. Probar el flujo completo (Postman o curl)

**Windows PowerShell:**
```powershell
curl.exe -X POST http://localhost:8081/eventos/cancelacion `
  -H "Content-Type: application/json" `
  -d '{"profesionalId": 1, "fecha": "2026-08-01", "motivo": "Licencia médica", "registradoPor": 1}'
```

**Postman:**
```
POST http://localhost:8081/eventos/cancelacion
Content-Type: application/json

{
  "profesionalId": 1,
  "fecha": "2026-08-01",
  "motivo": "Licencia médica",
  "registradoPor": 1
}
```

## 4. Verificar el resultado (Definition of Done del Paso 2)

1. **Respuesta HTTP:** debería devolver `201 Created` con el evento y estado `"COMPLETADO"`.
2. **RabbitMQ:** entra a `http://localhost:15672` (usuario/clave de tu `.env`) → pestaña
   *Queues* → `cancelaciones.eventos` → deberías ver `Ready: 1` (o el mensaje ya consumido
   si tienes un listener corriendo). Puedes usar *Get messages* para ver el JSON publicado,
   que debe traer los 3 pacientes de prueba con sus datos de contacto.
3. **Base de datos:** las 3 citas de prueba deberían quedar en estado `CANCELADA`:
   ```powershell
   docker exec -it medalert-postgres psql -U medalert -d medalert -c "SELECT id, estado FROM cita;"
   ```

Si ves los 3 pasos correctos, **HU01-02 está funcionalmente lista** — puedes pasar al
Paso 3 del plan (motor de notificación SMS/email en `notification-service`).

## Troubleshooting rápido

- **`Connection to localhost:5432 refused`** → el Docker Compose no está levantado o el
  puerto de Postgres no está mapeado (ver semana 0).
- **Error de Flyway al iniciar** → probablemente ya corriste `V1__init_schema.sql`
  manualmente antes de tener este proyecto. Si Flyway se queja de que las tablas ya
  existen, corre: `docker exec -it medalert-postgres psql -U medalert -d medalert -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"`
  y deja que Flyway cree el esquema desde cero al iniciar la app (no ejecutes el SQL a mano en ese caso).
- **La cola no aparece en RabbitMQ** → confirma que la app arrancó sin errores y que
  `spring.rabbitmq.*` en `application.yml` coincide con las credenciales de tu `.env`.
