Reseña del proyecto MedAlert Pro

Objetivo
- Sistema de notificaciones de cancelación de citas (simulación RAS) que detecta eventos de cancelación en un mock FHIR, publica pacientes afectados a RabbitMQ y envía notificaciones multicanal (SMS, WhatsApp, Email) con trazabilidad y escalamiento.

Componentes principales
- fhir-integration-service (Spring Boot, Java 17, Maven)
  - Puerto: 8081
  - Función: registra evento de cancelación, actualiza estado de citas, publica mensaje en exchange `medalert.eventos` con routing-key `cancelacion.registrada` y cola `cancelaciones.eventos`.
  - Endpoint principal: POST /eventos/cancelacion
  - Config: src/main/resources/application.yml (DB, RabbitMQ, medalert.fhir.server-url)

- notification-service (Spring Boot, Gradle)
  - Puerto: 8082
  - Función: consume cola `cancelaciones.eventos`, envía SMS (Twilio) y email (SendGrid/Mailtrap), registra envíos en tabla `notificacion` y soporta escalamiento automático.
  - Características: escalación (SMS→WHATSAPP→EMAIL), scheduler, endpoint para confirmar notificaciones: POST /notificaciones/{id}/confirmar

- patient-portal-service (Spring Boot, Gradle)
  - Puerto: 8083
  - Función: autenticación por RUT + OTP (Redis), consulta de citas, historial de notificaciones y confirmación desde paciente.
  - Endpoints: POST /auth/solicitar-otp, POST /auth/verificar-otp, GET /citas, GET /notificaciones, POST /notificaciones/{id}/confirmar
  - Usa Redis para OTP y Flyway para migraciones.

- patient-portal (Frontend, React+TypeScript, Vite)
  - Dev: npm run dev (http://localhost:5173)
  - Consume patient-portal-service (configurable en src/api/client.ts)
  - Diseño: mobile-first, accesibilidad (WCAG), componente ChannelTrail para mostrar ruta de escalamiento.

- admin-dashboard: carpeta eliminada del repo (estaba vacía, sin código). El dashboard de admin SÍ está implementado, pero repartido en:
  - notification-service: controller `AdminDashboardController` con `GET /admin/dashboard/kpis` y `GET /admin/dashboard/eventos-recientes`, protegido por `AdminAuthGuard`.
  - patient-portal (frontend): página `src/pages/AdminDashboard.tsx` + `AdminDashboard.css`.

Infra y servicios auxiliares (docker-compose.dev.yml)
- Postgres 15 (medalert database) — puerto 5432
- RabbitMQ (management) — AMQP 5672, UI 15672
- HAPI FHIR mock — puerto 8080 (mock RAS)
- Redis — puerto 6379
- Volumen: medalert_postgres_data

Esquema y datos
- migrations/V1__init_schema.sql — tablas: establecimiento, usuario_admin, profesional_salud, paciente, cita, evento_cancelacion, notificacion, reagendamiento, índices útiles.
- seed-test-data.sql — script para cargar datos de prueba (copiar al contenedor Postgres y ejecutar).

Variables de entorno clave (.env.example)
- POSTGRES_DB/USER/PASSWORD
- RABBITMQ_DEFAULT_USER/PASS
- FHIR_SERVER_URL
- TWILIO_*, SENDGRID_API_KEY, MAIL_*
- REDIS_HOST/PORT

Cómo levantar el entorno (desarrollo)
1. Copiar .env.example -> .env y completar credenciales (no commitear .env).
2. docker compose -f docker-compose.dev.yml up -d
3. docker cp seed-test-data.sql medalert-postgres:/seed-test-data.sql && docker exec -it medalert-postgres psql -U medalert -d medalert -f /seed-test-data.sql
4. Levantar microservicios: fhir-integration-service (mvn spring-boot:run, puerto 8081), notification-service (Gradle, puerto 8082), patient-portal-service (Gradle, puerto 8083). Frontend: patient-portal npm install && npm run dev (http://localhost:5173).

Estado de desarrollo (resumen, verificado contra el código el 2026-08-08)
- HU01-02 (fhir-integration): Implementado — POST /eventos/cancelacion registra evento y publica en RabbitMQ. Entidades Cita, EventoCancelacion, Paciente, ProfesionalSalud.
- HU03-05 (notification): Implementado — listener de cola, envíos SMS/email, trazabilidad en tabla notificacion. Endpoints GET /notificaciones y POST /notificaciones/{id}/confirmar.
- HU06-08 (escalamiento y WhatsApp): Implementado — EscalacionScheduler, MensajeBuilder, WhatsAppService vía Twilio; falta solo configurar el sandbox de Twilio para probar WhatsApp end-to-end.
- HU09-11 (portal paciente): Implementado — auth por RUT+OTP (Redis), GET /citas, GET/confirmar notificaciones, GET /paciente/perfil.
- HU12-13 (reagendamiento y datos de contacto): Implementado — no estaba registrado en versiones previas de este doc. POST /citas/{id}/reagendar (HU12) y PUT /paciente/datos-contacto (HU13), en patient-portal-service.
- Admin dashboard: Implementado. Vive en notification-service (AdminDashboardController, AdminAuthGuard) y en patient-portal (página AdminDashboard.tsx). No tiene número de HU asignado en el código — revisar si corresponde a una HU del backlog original.
- Migraciones: V1__init_schema.sql presente y Flyway configurado en los servicios.

Brechas detectadas (2026-08-08)
- Sin tests automatizados: no existe ninguna carpeta src/test en los 3 microservicios Java, y patient-portal (React) no tiene script de test en package.json (solo dev/build/preview). Riesgo para la defensa si se pide evidencia de testing.
- Sin Dockerfile para los microservicios ni el frontend: docker-compose.dev.yml solo levanta infraestructura (Postgres, RabbitMQ, HAPI FHIR mock, Redis). Los 3 backends y el frontend se corren manualmente (gradle/npm). Si se necesita demo "todo en Docker", falta este paso.
- Sin CI/CD: no hay carpeta .github/workflows ni pipeline configurado.
- admin-dashboard/: carpeta vacía eliminada del repo (ver arriba); su funcionalidad real vive en notification-service + patient-portal.
- No existe plan_inicio_desarrollo.md (referenciado en el README raíz) ni ningún documento de backlog/plan con la lista completa de HUs — la numeración HU01-13 se reconstruyó leyendo comentarios Javadoc en el código, no hay fuente única de verdad.

Archivos clave para revisar
- README.md (raíz): instrucciones generales de setup
- docker-compose.dev.yml: infraestructura local
- .env.example: variables necesarias
- migrations/V1__init_schema.sql
- fhir-integration-service/src/main/resources/application.yml
- notification-service/README.md
- patient-portal/README.md y package.json
- patient-portal-service/src/main/resources/application.yml

Siguientes pasos recomendados
- Verificar admin-dashboard: confirmar existencia y endpoints (README faltante o ruta distinta).
- Añadir o actualizar documentación de API (OpenAPI/Swagger) en cada microservicio si se requiere para la defensa.
- Preparar datos adicionales para demostración (números verificados en Twilio trial, Mailtrap inbox, OTP test accounts).

Contacto rápido
- Para ejecutar la demo: docker-compose up, cargar seed-test-data.sql, arrancar los tres servicios backend y el frontend. Probar POST /eventos/cancelacion y seguir el flujo hasta confirmar notificaciones.

(Documento generado automáticamente por Copilot CLI — pedir ajustes o ampliaciones específicas si quieres más detalle por servicio.)