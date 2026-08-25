Modelo de datos — MedAlert Pro

Diagrama ER (Mermaid)

```mermaid
erDiagram
    ESTABLECIMIENTO {
        BIGSERIAL id PK
        VARCHAR nombre
        VARCHAR servicio_salud
        TIMESTAMP created_at
    }

    USUARIO_ADMIN {
        BIGSERIAL id PK
        VARCHAR nombre
        VARCHAR email
        VARCHAR rol
        BIGINT establecimiento_id FK
        BOOLEAN activo
        TIMESTAMP created_at
    }

    PROFESIONAL_SALUD {
        BIGSERIAL id PK
        VARCHAR nombre
        VARCHAR especialidad
        BIGINT establecimiento_id FK
        TIMESTAMP created_at
    }

    PACIENTE {
        BIGSERIAL id PK
        VARCHAR rut
        VARCHAR nombre
        VARCHAR telefono
        VARCHAR email
        VARCHAR canal_preferido
        TIMESTAMP datos_actualizados_en
        BOOLEAN adulto_mayor
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    CITA {
        BIGSERIAL id PK
        BIGINT paciente_id FK
        BIGINT profesional_id FK
        TIMESTAMP fecha_hora
        VARCHAR estado
        VARCHAR origen_ras_id
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    EVENTO_CANCELACION {
        BIGSERIAL id PK
        BIGINT profesional_id FK
        TIMESTAMP fecha_evento
        VARCHAR motivo
        VARCHAR estado
        BIGINT registrado_por FK  -- usuario_admin.id
        TIMESTAMP created_at
    }

    NOTIFICACION {
        BIGSERIAL id PK
        BIGINT evento_id FK
        BIGINT paciente_id FK
        BIGINT cita_id FK
        VARCHAR canal
        SMALLINT intento_numero
        VARCHAR estado_envio
        VARCHAR proveedor_message_id
        TIMESTAMP enviado_en
        TIMESTAMP confirmado_en
        TIMESTAMP created_at
    }

    REAGENDAMIENTO {
        BIGSERIAL id PK
        BIGINT cita_original_id FK
        BIGINT cita_nueva_id FK
        BIGINT paciente_id FK
        TIMESTAMP fecha_solicitud
        VARCHAR estado
    }

    %% Relaciones
    ESTABLECIMIENTO ||--o{ USUARIO_ADMIN : "tiene"
    ESTABLECIMIENTO ||--o{ PROFESIONAL_SALUD : "tiene"

    PACIENTE ||--o{ CITA : "tiene"
    PROFESIONAL_SALUD ||--o{ CITA : "atiende"

    PROFESIONAL_SALUD ||--o{ EVENTO_CANCELACION : "genera"
    USUARIO_ADMIN ||--o{ EVENTO_CANCELACION : "registra"

    EVENTO_CANCELACION ||--o{ NOTIFICACION : "genera"
    PACIENTE ||--o{ NOTIFICACION : "recibe"
    CITA ||--o{ NOTIFICACION : "relacionada"

    CITA ||--o{ REAGENDAMIENTO : "original"
    CITA ||--o{ REAGENDAMIENTO : "nueva"
    PACIENTE ||--o{ REAGENDAMIENTO : "solicita"
```

Descripción
- El SQL de migración `migrations/V1__init_schema.sql` define las tablas y claves foráneas.
- Entidades clave: paciente, cita, profesional_salud, evento_cancelacion, notificacion, reagendamiento, establecimiento, usuario_admin.
- Notas:
  - `notificacion` guarda trazabilidad de envíos (proveedor_message_id, enviado_en, confirmado_en) y permite escalamiento por `intento_numero`.
  - `cita.origen_ras_id` almacena el id del recurso Appointment del servidor FHIR cuando aplica.

Archivos relacionados
- migrations/V1__init_schema.sql — script fuente del modelo
- docs/resumen_proyecto.md — resumen general del proyecto

Sugerencias
- Para generar una imagen (PNG/SVG) se puede usar un renderizador Mermaid o PlantUML en local (VSCode + extensión, o mermaid-cli: `mmdc -i data_model.mmd -o data_model.png`).
- Si quieres, genero también un archivo PlantUML (.puml) o un fichero Graphviz DOT.
