package cl.medalertpro.notification.service;

import cl.medalertpro.notification.dto.AdminDashboardEventDto;
import cl.medalertpro.notification.dto.AdminDashboardKpisResponse;
import cl.medalertpro.notification.dto.ReporteMensualResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        service = new AdminDashboardService(jdbcTemplate, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "minutosPorNotificacionManual", 3.0);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // cache miss por defecto
    }

    @Test
    void calculaKpisApartirDeLasFilasDevueltasPorLaBaseDeDatos() throws SQLException {
        stubQueryForObject("total_notificaciones", row("total_notificaciones", 10L, "entregas_exitosas", 7L));
        stubQueryForObject("total_contactos", row("total_contactos", 5L, "primer_intento", 3L, "tras_escalamiento", 2L));
        stubQueryForObject("promedio_minutos", row("total_eventos", 4L, "promedio_minutos", 12.344, "maximo_minutos", 30.0));
        stubQueryForObject("total_canceladas", row("total_canceladas", 8L, "total_reagendadas", 6L));
        stubQueryForObject("total_pacientes", row("total_pacientes", 20L, "pacientes_actualizados", 5L));

        when(jdbcTemplate.query(contains("estado_dashboard"), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(fakeResultSet(row("canal", "SMS", "estado_dashboard", "confirmado", "total", 3L)), 1));
                });
        when(jdbcTemplate.query(contains("semana_inicio"), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    Map<String, Object> data = row("semana_inicio", LocalDate.of(2026, 8, 3), "total_eventos", 5L);
                    return List.of(mapper.mapRow(fakeResultSet(data), 1));
                });

        AdminDashboardKpisResponse response = service.obtenerKpis();

        assertThat(response.getDeliveryRate().getTotalNotificaciones()).isEqualTo(10);
        assertThat(response.getDeliveryRate().getEntregasExitosas()).isEqualTo(7);
        assertThat(response.getDeliveryRate().getPorcentajeExito()).isEqualTo(70.0);

        assertThat(response.getContactEffectiveness().getTotalContactosEfectivos()).isEqualTo(5);
        assertThat(response.getContactEffectiveness().getPorcentajePrimerIntento()).isEqualTo(60.0);
        assertThat(response.getContactEffectiveness().getPorcentajeTrasEscalamiento()).isEqualTo(40.0);

        assertThat(response.getNotificationTime().getTotalEventos()).isEqualTo(4);
        assertThat(response.getNotificationTime().getPromedioMinutos()).isEqualTo(12.34);
        assertThat(response.getNotificationTime().getMaximoMinutos()).isEqualTo(30.0);

        assertThat(response.getPortalReschedule().getPorcentajeReagendamiento()).isEqualTo(75.0);
        assertThat(response.getContactUpdate().getPorcentajeActualizados()).isEqualTo(25.0);

        assertThat(response.getChannelStatusDistribution()).hasSize(1);
        assertThat(response.getChannelStatusDistribution().get(0).getCanal()).isEqualTo("SMS");

        assertThat(response.getWeeklyCancellationHistory()).hasSize(1);
        assertThat(response.getWeeklyCancellationHistory().get(0).getTotalEventos()).isEqualTo(5);

        verify(valueOperations).set(eq("admin-dashboard:kpis"), anyString(), eq(Duration.ofSeconds(20)));
    }

    @Test
    void usaCacheDeRedisSinConsultarLaBaseDeDatosCuandoHayHit() throws Exception {
        AdminDashboardKpisResponse cached = new AdminDashboardKpisResponse();
        cached.setGeneradoEn(LocalDateTime.of(2026, 8, 8, 10, 0));
        AdminDashboardKpisResponse.DeliveryRate dr = new AdminDashboardKpisResponse.DeliveryRate();
        dr.setTotalNotificaciones(99);
        dr.setEntregasExitosas(50);
        dr.setPorcentajeExito(50.5);
        cached.setDeliveryRate(dr);
        String json = objectMapper.writeValueAsString(cached);

        when(valueOperations.get("admin-dashboard:kpis")).thenReturn(json);

        AdminDashboardKpisResponse response = service.obtenerKpis();

        assertThat(response.getDeliveryRate().getTotalNotificaciones()).isEqualTo(99);
        assertThat(response.getDeliveryRate().getPorcentajeExito()).isEqualTo(50.5);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void obtieneEventosRecientesYMapeaLasColumnas() {
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    Map<String, Object> data = new HashMap<>();
                    data.put("evento_id", 55L);
                    data.put("fecha_evento", LocalDateTime.of(2026, 8, 1, 10, 0));
                    data.put("motivo", "corte de luz");
                    data.put("estado", "COMPLETADO");
                    data.put("profesional_id", 7L);
                    data.put("profesional_nombre", "Dra. Fuentes");
                    data.put("profesional_especialidad", "Medicina General");
                    data.put("registrado_por", 1L);
                    data.put("pacientes_notificados", 3L);
                    data.put("notificaciones_confirmadas", 2L);
                    data.put("minutos_totales_notificacion", 15.5);
                    data.put("ultimo_hito", LocalDateTime.of(2026, 8, 1, 10, 20));
                    data.put("canales", new String[] { "SMS", "WHATSAPP" });
                    return List.of(mapper.mapRow(fakeResultSet(data), 1));
                });

        List<AdminDashboardEventDto> eventos = service.obtenerEventosRecientes(500);

        assertThat(eventos).hasSize(1);
        AdminDashboardEventDto dto = eventos.get(0);
        assertThat(dto.getEventoId()).isEqualTo(55L);
        assertThat(dto.getMotivo()).isEqualTo("corte de luz");
        assertThat(dto.getEstado()).isEqualTo("COMPLETADO");
        assertThat(dto.getProfesionalId()).isEqualTo(7L);
        assertThat(dto.getProfesionalNombre()).isEqualTo("Dra. Fuentes");
        assertThat(dto.getProfesionalEspecialidad()).isEqualTo("Medicina General");
        assertThat(dto.getRegistradoPor()).isEqualTo(1L);
        assertThat(dto.getPacientesNotificados()).isEqualTo(3L);
        assertThat(dto.getNotificacionesConfirmadas()).isEqualTo(2L);
        assertThat(dto.getMinutosTotalesNotificacion()).isEqualTo(15.5);
        assertThat(dto.getCanales()).containsExactly("SMS", "WHATSAPP");
    }

    @Test
    void eventosRecientesSinProfesionalAsociadoDejaCamposEnNull() {
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    Map<String, Object> data = new HashMap<>();
                    data.put("evento_id", 56L);
                    data.put("fecha_evento", LocalDateTime.of(2026, 8, 2, 9, 0));
                    data.put("motivo", "licencia");
                    data.put("estado", "PROCESANDO");
                    data.put("pacientes_notificados", 0L);
                    data.put("notificaciones_confirmadas", 0L);
                    return List.of(mapper.mapRow(fakeResultSet(data), 1));
                });

        List<AdminDashboardEventDto> eventos = service.obtenerEventosRecientes(500);

        AdminDashboardEventDto dto = eventos.get(0);
        assertThat(dto.getProfesionalId()).isNull();
        assertThat(dto.getRegistradoPor()).isNull();
        assertThat(dto.getCanales()).isEmpty();
    }

    @Test
    void acotaElLimiteDeEventosRecientesAUnMaximoDeCien() {
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenReturn(List.of());

        service.obtenerEventosRecientes(500);

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).query(anyString(), paramsCaptor.capture(), ArgumentMatchersHelper.<Object>rowMapper());
        assertThat(paramsCaptor.getValue().getValue("limite")).isEqualTo(100);
    }

    @Test
    void acotaElLimiteDeEventosRecientesAUnMinimoDeUno() {
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenReturn(List.of());

        service.obtenerEventosRecientes(-5);

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).query(anyString(), paramsCaptor.capture(), ArgumentMatchersHelper.<Object>rowMapper());
        assertThat(paramsCaptor.getValue().getValue("limite")).isEqualTo(1);
    }

    @Test
    void calculaReporteMensualApartirDeLasFilasDevueltasPorLaBaseDeDatos() throws SQLException {
        YearMonth periodo = YearMonth.of(2026, 8);

        stubQueryForObject("AS exitosas", row("total", 20L, "exitosas", 18L));
        stubQueryForObjectLong("FROM reagendamiento", 4L);

        when(jdbcTemplate.query(contains("GROUP BY canal"), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(fakeResultSet(row("canal", "SMS", "enviados", 10L, "exitosos", 9L)), 1));
                });

        stubQueryForObject("sms_a_whatsapp", row("total_contactados", 12L, "sms_a_whatsapp", 5L, "whatsapp_a_email", 2L));
        stubQueryForObjectLong("WHERE rn = 1", 3L);

        // Ausentismo: distinto valor por mes según el "desde" recibido, para poder
        // distinguir el período actual, el mes anterior y los puntos de la evolución.
        when(jdbcTemplate.queryForObject(contains("FROM cita"), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    SqlParameterSource params = invocation.getArgument(1);
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    LocalDateTime desde = (LocalDateTime) params.getValue("desde");
                    YearMonth mes = YearMonth.from(desde);
                    Map<String, Object> data = mes.equals(periodo)
                            ? row("no_asistio", 2L, "resueltas", 20L)   // 10.0%
                            : row("no_asistio", 3L, "resueltas", 20L);  // 15.0% (todos los demás meses)
                    return mapper.mapRow(fakeResultSet(data), 1);
                });

        ReporteMensualResponse reporte = service.obtenerReporteMensual(periodo);

        assertThat(reporte.getPeriodo()).isEqualTo("2026-08");
        assertThat(reporte.getTotalNotificaciones()).isEqualTo(20);
        assertThat(reporte.getPorcentajeEntrega()).isEqualTo(90.0);
        assertThat(reporte.getHorasAhorradasEstimadas()).isEqualTo((18 * 3.0) / 60.0);
        assertThat(reporte.getHorasAhorradasNotaMetodologica()).contains("Estimación");
        assertThat(reporte.getReagendamientos()).isEqualTo(4);

        assertThat(reporte.getNotificacionesPorCanal()).hasSize(1);
        assertThat(reporte.getNotificacionesPorCanal().get(0).getCanal()).isEqualTo("SMS");
        assertThat(reporte.getNotificacionesPorCanal().get(0).getPorcentajeEntregado()).isEqualTo(90.0);

        assertThat(reporte.getEscalamientos().getTotalContactados()).isEqualTo(12);
        assertThat(reporte.getEscalamientos().getSmsAWhatsapp()).isEqualTo(5);
        assertThat(reporte.getEscalamientos().getWhatsappAEmail()).isEqualTo(2);
        assertThat(reporte.getEscalamientos().getSinContactoDefinitivo()).isEqualTo(3);

        assertThat(reporte.getTasaAusentismo()).isEqualTo(10.0);
        assertThat(reporte.getTasaAusentismoMesAnterior()).isEqualTo(15.0);

        assertThat(reporte.getAusentismoEvolucion()).hasSize(6);
        assertThat(reporte.getAusentismoEvolucion().get(5).getPeriodo()).isEqualTo("2026-08");
        assertThat(reporte.getAusentismoEvolucion().get(5).getTasa()).isEqualTo(10.0);
        assertThat(reporte.getAusentismoEvolucion().get(0).getPeriodo()).isEqualTo("2026-03");
        assertThat(reporte.getAusentismoEvolucion().get(0).getTasa()).isEqualTo(15.0);

        verify(valueOperations).set(eq("admin-dashboard:reporte:2026-08"), anyString(), eq(Duration.ofSeconds(20)));
    }

    private void stubQueryForObjectLong(String sqlContains, long value) {
        when(jdbcTemplate.queryForObject(contains(sqlContains), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(value);
    }

    private void stubQueryForObject(String sqlContains, Map<String, Object> data) throws SQLException {
        when(jdbcTemplate.queryForObject(contains(sqlContains), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    return mapper.mapRow(fakeResultSet(data), 1);
                });
    }

    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private ResultSet fakeResultSet(Map<String, Object> data) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        lenient().when(rs.getLong(anyString())).thenAnswer(inv -> {
            Object v = data.get(inv.getArgument(0, String.class));
            return v == null ? 0L : ((Number) v).longValue();
        });
        lenient().when(rs.getDouble(anyString())).thenAnswer(inv -> {
            Object v = data.get(inv.getArgument(0, String.class));
            return v == null ? 0.0 : ((Number) v).doubleValue();
        });
        lenient().when(rs.getString(anyString())).thenAnswer(inv -> {
            Object v = data.get(inv.getArgument(0, String.class));
            return v == null ? null : v.toString();
        });
        lenient().when(rs.getObject(anyString(), any(Class.class))).thenAnswer(inv ->
                data.get(inv.getArgument(0, String.class)));
        lenient().when(rs.getArray(anyString())).thenAnswer(inv -> {
            Object v = data.get(inv.getArgument(0, String.class));
            if (v == null) return null;
            java.sql.Array array = mock(java.sql.Array.class);
            lenient().when(array.getArray()).thenReturn(v);
            return array;
        });
        lenient().when(rs.wasNull()).thenReturn(false);
        return rs;
    }

    /**
     * Pequeño ayudante para tipar el matcher genérico any() como RowMapper&lt;Object&gt;
     * sin warnings de tipos crudos en cada llamada.
     */
    private static final class ArgumentMatchersHelper<T> {
        static <T> RowMapper<T> rowMapper() {
            return org.mockito.ArgumentMatchers.any();
        }
    }
}
