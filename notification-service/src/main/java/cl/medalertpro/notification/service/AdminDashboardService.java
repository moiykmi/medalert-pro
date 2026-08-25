package cl.medalertpro.notification.service;

import cl.medalertpro.notification.dto.AdminDashboardEventDto;
import cl.medalertpro.notification.dto.AdminDashboardKpisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AdminDashboardService {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(20);
    private static final String KPI_CACHE_KEY = "admin-dashboard:kpis";
    private static final String EVENTS_CACHE_KEY_PREFIX = "admin-dashboard:events:";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AdminDashboardService(NamedParameterJdbcTemplate jdbcTemplate,
                                 StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public AdminDashboardKpisResponse obtenerKpis() {
        return leerConCache(KPI_CACHE_KEY, AdminDashboardKpisResponse.class, this::consultarKpis);
    }

    public List<AdminDashboardEventDto> obtenerEventosRecientes(int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 100));
        JavaType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, AdminDashboardEventDto.class);
        return leerConCache(EVENTS_CACHE_KEY_PREFIX + limiteSeguro, listType,
                () -> consultarEventosRecientes(limiteSeguro));
    }

    private AdminDashboardKpisResponse consultarKpis() {
        AdminDashboardKpisResponse response = new AdminDashboardKpisResponse();
        response.setGeneradoEn(LocalDateTime.now());
        response.setDeliveryRate(cargarDeliveryRate());
        response.setContactEffectiveness(cargarContactEffectiveness());
        response.setNotificationTime(cargarNotificationTime());
        response.setPortalReschedule(cargarPortalReschedule());
        response.setContactUpdate(cargarContactUpdate());
        response.setChannelStatusDistribution(cargarChannelStatusDistribution());
        response.setWeeklyCancellationHistory(cargarWeeklyCancellationHistory());
        return response;
    }

    private AdminDashboardKpisResponse.DeliveryRate cargarDeliveryRate() {
        String sql = """
                SELECT COUNT(*) AS total_notificaciones,
                       SUM(CASE WHEN estado_envio IN ('ENVIADO','CONFIRMADO','LEIDO','SIN_RESPUESTA') THEN 1 ELSE 0 END) AS entregas_exitosas
                FROM notificacion
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            long total = rs.getLong("total_notificaciones");
            long exitosas = rs.getLong("entregas_exitosas");
            AdminDashboardKpisResponse.DeliveryRate dto = new AdminDashboardKpisResponse.DeliveryRate();
            dto.setTotalNotificaciones(total);
            dto.setEntregasExitosas(exitosas);
            dto.setPorcentajeExito(porcentaje(exitosas, total));
            return dto;
        });
    }

    private AdminDashboardKpisResponse.ContactEffectiveness cargarContactEffectiveness() {
        String sql = """
                WITH contactos AS (
                    SELECT evento_id, paciente_id, MIN(intento_numero) AS intento_efectivo
                    FROM notificacion
                    WHERE estado_envio = 'CONFIRMADO'
                    GROUP BY evento_id, paciente_id
                )
                SELECT COUNT(*) AS total_contactos,
                       SUM(CASE WHEN intento_efectivo = 1 THEN 1 ELSE 0 END) AS primer_intento,
                       SUM(CASE WHEN intento_efectivo > 1 THEN 1 ELSE 0 END) AS tras_escalamiento
                FROM contactos
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            long total = rs.getLong("total_contactos");
            long primerIntento = rs.getLong("primer_intento");
            long trasEscalamiento = rs.getLong("tras_escalamiento");
            AdminDashboardKpisResponse.ContactEffectiveness dto = new AdminDashboardKpisResponse.ContactEffectiveness();
            dto.setTotalContactosEfectivos(total);
            dto.setPrimerIntento(primerIntento);
            dto.setTrasEscalamiento(trasEscalamiento);
            dto.setPorcentajePrimerIntento(porcentaje(primerIntento, total));
            dto.setPorcentajeTrasEscalamiento(porcentaje(trasEscalamiento, total));
            return dto;
        });
    }

    private AdminDashboardKpisResponse.NotificationTime cargarNotificationTime() {
        String sql = """
                WITH evento_tiempos AS (
                    SELECT e.id AS evento_id,
                           EXTRACT(EPOCH FROM (MAX(COALESCE(n.confirmado_en, n.enviado_en, n.created_at)) - e.fecha_evento)) / 60.0 AS minutos_totales
                    FROM evento_cancelacion e
                    LEFT JOIN notificacion n ON n.evento_id = e.id
                    GROUP BY e.id, e.fecha_evento
                    HAVING COUNT(n.id) > 0
                )
                SELECT COUNT(*) AS total_eventos,
                       AVG(minutos_totales) AS promedio_minutos,
                       MAX(minutos_totales) AS maximo_minutos
                FROM evento_tiempos
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            AdminDashboardKpisResponse.NotificationTime dto = new AdminDashboardKpisResponse.NotificationTime();
            dto.setTotalEventos(rs.getLong("total_eventos"));
            dto.setPromedioMinutos(redondear2(rs.getDouble("promedio_minutos")));
            dto.setMaximoMinutos(redondear2(rs.getDouble("maximo_minutos")));
            return dto;
        });
    }

    private AdminDashboardKpisResponse.PortalReschedule cargarPortalReschedule() {
        String sql = """
                WITH canceladas AS (
                    SELECT COUNT(*) AS total_canceladas
                    FROM cita
                    WHERE estado IN ('CANCELADA', 'REAGENDADA')
                ),
                reag AS (
                    SELECT COUNT(DISTINCT cita_original_id) AS total_reagendadas
                    FROM reagendamiento
                    WHERE estado = 'CONFIRMADO'
                )
                SELECT canceladas.total_canceladas, reag.total_reagendadas
                FROM canceladas CROSS JOIN reag
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            long totalCanceladas = rs.getLong("total_canceladas");
            long totalReagendadas = rs.getLong("total_reagendadas");
            AdminDashboardKpisResponse.PortalReschedule dto = new AdminDashboardKpisResponse.PortalReschedule();
            dto.setTotalCitasCanceladas(totalCanceladas);
            dto.setCitasReagendadas(totalReagendadas);
            dto.setPorcentajeReagendamiento(porcentaje(totalReagendadas, totalCanceladas));
            return dto;
        });
    }

    private AdminDashboardKpisResponse.ContactUpdate cargarContactUpdate() {
        String sql = """
                SELECT COUNT(*) AS total_pacientes,
                       SUM(CASE WHEN datos_actualizados_en IS NOT NULL THEN 1 ELSE 0 END) AS pacientes_actualizados
                FROM paciente
                """;
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            long totalPacientes = rs.getLong("total_pacientes");
            long actualizados = rs.getLong("pacientes_actualizados");
            AdminDashboardKpisResponse.ContactUpdate dto = new AdminDashboardKpisResponse.ContactUpdate();
            dto.setTotalPacientes(totalPacientes);
            dto.setPacientesActualizados(actualizados);
            dto.setPorcentajeActualizados(porcentaje(actualizados, totalPacientes));
            return dto;
        });
    }

    private List<AdminDashboardKpisResponse.ChannelStatusPoint> cargarChannelStatusDistribution() {
        String sql = """
                SELECT canal,
                       CASE
                           WHEN estado_envio = 'CONFIRMADO' THEN 'confirmado'
                           WHEN estado_envio = 'SIN_RESPUESTA' THEN 'sin_respuesta'
                           WHEN estado_envio = 'FALLIDO' THEN 'reintentando'
                           ELSE 'pendiente'
                       END AS estado_dashboard,
                       COUNT(*) AS total
                FROM notificacion
                GROUP BY canal, estado_dashboard
                ORDER BY canal, estado_dashboard
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            AdminDashboardKpisResponse.ChannelStatusPoint dto = new AdminDashboardKpisResponse.ChannelStatusPoint();
            dto.setCanal(rs.getString("canal"));
            dto.setEstado(rs.getString("estado_dashboard"));
            dto.setTotal(rs.getLong("total"));
            return dto;
        });
    }

    private List<AdminDashboardKpisResponse.WeeklyCancellationPoint> cargarWeeklyCancellationHistory() {
        String sql = """
                SELECT semana_inicio, total_eventos
                FROM (
                    SELECT DATE_TRUNC('week', fecha_evento)::date AS semana_inicio,
                           COUNT(*) AS total_eventos
                    FROM evento_cancelacion
                    GROUP BY semana_inicio
                    ORDER BY semana_inicio DESC
                    LIMIT 12
                ) historial
                ORDER BY semana_inicio ASC
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            AdminDashboardKpisResponse.WeeklyCancellationPoint dto = new AdminDashboardKpisResponse.WeeklyCancellationPoint();
            dto.setSemanaInicio(rs.getObject("semana_inicio", LocalDate.class));
            dto.setTotalEventos(rs.getLong("total_eventos"));
            return dto;
        });
    }

    private List<AdminDashboardEventDto> consultarEventosRecientes(int limite) {
        String sql = """
                SELECT e.id AS evento_id,
                       e.fecha_evento,
                       e.motivo,
                       COUNT(DISTINCT n.paciente_id) AS pacientes_notificados,
                       COUNT(DISTINCT CASE WHEN n.estado_envio = 'CONFIRMADO' THEN n.paciente_id END) AS notificaciones_confirmadas,
                       EXTRACT(EPOCH FROM (MAX(COALESCE(n.confirmado_en, n.enviado_en, n.created_at)) - e.fecha_evento)) / 60.0 AS minutos_totales_notificacion,
                       MAX(COALESCE(n.confirmado_en, n.enviado_en, n.created_at)) AS ultimo_hito
                FROM evento_cancelacion e
                LEFT JOIN notificacion n ON n.evento_id = e.id
                GROUP BY e.id, e.fecha_evento, e.motivo
                ORDER BY e.fecha_evento DESC
                LIMIT :limite
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("limite", limite), (rs, rowNum) -> {
            AdminDashboardEventDto dto = new AdminDashboardEventDto();
            dto.setEventoId(rs.getLong("evento_id"));
            dto.setFechaEvento(rs.getObject("fecha_evento", LocalDateTime.class));
            dto.setMotivo(rs.getString("motivo"));
            dto.setPacientesNotificados(rs.getLong("pacientes_notificados"));
            dto.setNotificacionesConfirmadas(rs.getLong("notificaciones_confirmadas"));
            double minutos = rs.getDouble("minutos_totales_notificacion");
            dto.setMinutosTotalesNotificacion(rs.wasNull() ? null : redondear2(minutos));
            dto.setUltimoHito(rs.getObject("ultimo_hito", LocalDateTime.class));
            return dto;
        });
    }

    private <T> T leerConCache(String key, Class<T> type, Supplier<T> supplier) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (RedisConnectionFailureException | SerializationException e) {
            log.warn("Redis no disponible para clave {}: {}", key, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("No se pudo deserializar caché {}: {}", key, e.getMessage());
        }

        T fresh = supplier.get();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(fresh), CACHE_TTL);
        } catch (RedisConnectionFailureException | SerializationException e) {
            log.warn("Redis no disponible para guardar clave {}: {}", key, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar caché {}: {}", key, e.getMessage());
        }
        return fresh;
    }

    private <T> T leerConCache(String key, JavaType type, Supplier<T> supplier) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (RedisConnectionFailureException | SerializationException e) {
            log.warn("Redis no disponible para clave {}: {}", key, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("No se pudo deserializar caché {}: {}", key, e.getMessage());
        }

        T fresh = supplier.get();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(fresh), CACHE_TTL);
        } catch (RedisConnectionFailureException | SerializationException e) {
            log.warn("Redis no disponible para guardar clave {}: {}", key, e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar caché {}: {}", key, e.getMessage());
        }
        return fresh;
    }

    private static double porcentaje(long parte, long total) {
        if (total <= 0) return 0.0;
        return redondear2((parte * 100.0) / total);
    }

    private static double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
