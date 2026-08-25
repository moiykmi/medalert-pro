# Prompt: corregir Informe 2 (AINC421) para que sea consistente con el código real

Pega este prompt completo en una nueva sesión (o úsalo tal cual en esta) cuando quieras hacer la corrección. Incluye todo el contexto necesario — no depende de conversación previa.

---

## Contexto

Tengo un documento Word en `C:\Users\Moises\Downloads\ainc421_s8mvalencia.docx` — es el "Informe 2" del curso AINC421 (Semana 8), titulado "MedAlert Pro: Sistema Automatizado de Notificaciones Médicas... Diseño de la propuesta de solución. Aplicando UML". El código real del proyecto está en `C:\Users\Moises\Documents\seminario de ingenieria\codigo\medalert-pro`.

El documento describe una arquitectura objetivo que **ya no coincide con lo que está realmente implementado** en el código. Necesito que edites el `.docx` (texto y las 3 figuras/diagramas afectadas) para que describa fielmente la arquitectura tal como existe hoy en el repo — sin inventar nada nuevo, solo alineando el documento a la realidad del código. Usa el skill de `docx` (unzip → editar `word/document.xml` → rezip; las figuras son imágenes PNG embebidas en `word/media/`, hay que regenerarlas).

## Desajustes verificados que hay que corregir

### 1. No existe "API Gateway"
El documento (texto en 2.5, Figura 5, Figura 6, Figura 8, y la columna "Componente de diseño" de la Tabla 2 en HU09/HU10/HU11) describe un "API Gateway (Spring Cloud Gateway)" que centraliza autenticación y enrutamiento hacia los tres microservicios.

**Realidad:** no existe ningún API Gateway. El frontend (`patient-portal/src/api/client.ts`) llama directo a cada microservicio con URLs hardcodeadas: `http://localhost:8083` (patient-portal-service) y `http://localhost:8082` (notification-service, endpoints `/admin/dashboard/*`).

### 2. No hay dos portales separados, es una sola app React
El documento describe "Portal Paciente" y "Portal Administrativo" como dos componentes `«component» React/TS` distintos.

**Realidad:** es una única app (`patient-portal/`, un solo `package.json`, un solo `App.tsx`) con tres páginas: `Login.tsx`, `Portal.tsx` y `AdminDashboard.tsx` (esta última sí usa Recharts — eso el documento lo tiene bien).

### 3. No existe "MS Reagendamiento" como microservicio separado
El documento (Figura 5, Figura 6, y la Tabla 2 en HU12) presenta "MS Reagendamiento" como un microservicio Spring Boot independiente que además se comunica con "MS Integración FHIR".

**Realidad:** el reagendamiento es un endpoint (`POST /citas/{id}/reagendar`, en `CitasController.java`) dentro de `patient-portal-service`. Verifiqué el código: **no hace ninguna llamada HTTP a fhir-integration-service ni al mock FHIR** — solo crea una nueva `Cita` y un registro `Reagendamiento` en su propia base de datos. Así que la celda de la Tabla 2 para HU12 debe decir simplemente `patient-portal-service`, sin mencionar FHIR.

### 4. No existe una "Librería Común (DTO)" compartida
La Figura 5 muestra un componente `«library» Librería Común (DTO)` usado por los tres microservicios.

**Realidad:** cada servicio tiene sus propios DTOs duplicados de forma independiente (ej. `CancelacionEventoMessage` existe como clase separada en `fhir-integration-service` y en `notification-service`, sin compartir código).

### 5. No hay Nginx como proxy inverso ni TLS
El documento (2.7, Figura 6, Figura 8) describe un "Balanceador / Proxy inverso (Nginx) + TLS" que termina las conexiones HTTPS antes de enrutar a los contenedores.

**Realidad:** `docker-compose.yml` expone cada servicio en su puerto propio directamente (8081, 8082, 8083, 5173 para el frontend, etc.), sin proxy inverso ni terminación TLS. El único `nginx.conf` que existe en el repo (`patient-portal/nginx.conf`) solo sirve los archivos estáticos del frontend con fallback de rutas para el SPA — no enruta hacia los backends.

### 6. GitHub Actions (CI/CD) todavía no existe
El diagrama de despliegue (Figura 6) muestra "GitHub Actions (CI/CD)" conectado por webhook.

**Realidad:** el repo no tiene carpeta `.github/workflows` — no hay ningún pipeline configurado todavía.

### 7. El modelo entidad-relación (Figura 7, sección 2.6) está incompleto y con nombres distintos
Comparar contra el esquema real: `migrations/V1__init_schema.sql`.

Faltan dos entidades completas:
- `usuario_admin` (no aparece en el diagrama)
- `evento_cancelacion` (no aparece — y es central: cada `Notificacion` real tiene FK `evento_id` hacia `evento_cancelacion`, no una FK directa a `Cita` como muestra el diagrama actual; `cita_id` en `notificacion` es una FK secundaria opcional)

Nombre de campo incorrecto: el documento llama al campo puente con FHIR `fhir_appointment_id`; en el esquema real la columna se llama **`origen_ras_id`** (tabla `cita`).

Convención de nombres: el diagrama usa prefijo `id_` (`id_paciente`, `id_cita`); el esquema real usa sufijo `_id` (`paciente_id`, `cita_id`, `profesional_id`, `establecimiento_id`). Ajustar los nombres de columnas del diagrama a los reales.

## Qué hacer

1. **Reescribe el texto** de las secciones 2.5 ("Arquitectura de software a implementar") y 2.7 ("Arquitectura de hardware y comunicaciones") quitando toda mención a API Gateway, Spring Cloud Gateway, librería DTO compartida, Nginx/proxy inverso/TLS, MS Reagendamiento como servicio separado, GitHub Actions, y "dos portales". Descríbelo tal como es: tres microservicios Spring Boot independientes (`fhir-integration-service`, `notification-service`, `patient-portal-service`) expuestos cada uno en su puerto, un único frontend React con tres páginas, comunicación asíncrona vía RabbitMQ, persistencia en PostgreSQL compartida.
2. **Regenera las Figuras 5, 6 y 8** (diagramas de componentes, despliegue y hardware) reflejando la arquitectura real descrita arriba — sin API Gateway, sin Nginx, con un solo componente de frontend, sin MS Reagendamiento separado, sin GitHub Actions (a menos que se agregue de verdad antes de entregar).
3. **Corrige la Figura 7 y la sección 2.6** agregando `usuario_admin` y `evento_cancelacion`, corrigiendo la relación `Notificacion → evento_cancelacion` (FK `evento_id`) con `cita_id` como FK secundaria opcional, renombrando `fhir_appointment_id` a `origen_ras_id`, y usando la convención `_id` como sufijo en todos los campos FK.
4. **Actualiza la Tabla 2** (matriz de trazabilidad), columna "Componente de diseño":
   - HU09, HU10, HU11: cambiar `Portal Paciente / API Gateway` → `Portal Web (patient-portal) / patient-portal-service`
   - HU12: cambiar `MS Reagendamiento / MS Integración FHIR` → `patient-portal-service`
   - HU14: verificar que diga que el dashboard vive dentro de la misma app `patient-portal` (página `AdminDashboard.tsx`), no en un "Portal Administrativo" separado.
5. Deja intacto todo lo demás: la reflexión sobre UML (sección 1), la tabla de controles (2.2), la organización de sprints (2.3), las Figuras 1-4 (casos de uso) y A1/B1/C1 (secuencia) — no encontré desajustes ahí.
6. Guarda el resultado como un archivo nuevo (no sobrescribas el original) en `C:\Users\Moises\Downloads\ainc421_s8mvalencia_corregido.docx`, y renderízalo a PDF/imágenes para verificar visualmente antes de terminar.

## Notas
- No hay `pandoc`, `python` ni LibreOffice/`soffice` instalados en esta máquina — la extracción de texto se hizo parseando `word/document.xml` con PowerShell (`[xml]` + XPath sobre `w:t`), y las figuras se revisaron abriendo directamente los PNG de `word/media/` con el visor de imágenes. Si necesitas convertir a PDF para verificar el resultado final, instala LibreOffice o Python primero, o revisa las imágenes de `word/media/` directamente.
- El documento no tiene comentarios de Word activos (`word/comments.xml` existe pero vacío, 0 `<w:comment>`), así que no hay feedback previo del profesor que conservar.
