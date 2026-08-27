package cl.medalertpro.notification.service;

import cl.medalertpro.notification.dto.BitacoraEntryDto;
import cl.medalertpro.notification.dto.BitacoraResponse;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitacoraServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private PacienteRepository pacienteRepository;

    private BitacoraService service;

    @BeforeEach
    void setUp() {
        service = new BitacoraService(jdbcTemplate, pacienteRepository);
        // Por defecto ninguna categoría tiene filas — cada test activa solo la que le importa.
        lenient().when(jdbcTemplate.query(anyStringSafe(), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenReturn(List.of());
        lenient().when(jdbcTemplate.queryForObject(anyStringSafe(), any(SqlParameterSource.class), eqLongClass()))
                .thenReturn(0L);
    }

    private static String anyStringSafe() {
        return org.mockito.ArgumentMatchers.anyString();
    }

    private static Class<Long> eqLongClass() {
        return org.mockito.ArgumentMatchers.eq(Long.class);
    }

    @Test
    void mezclaYOrdenaCronologicamenteLasEntradasDeTodasLasCategorias() {
        stubQuery("LEFT JOIN profesional_salud", row(
                "evento_id", 1L, "fecha_evento", LocalDateTime.of(2026, 8, 27, 9, 14),
                "motivo", "licencia médica", "profesional_nombre", "Dra. Fuentes"));

        stubQuery("GROUP BY e.id, e.fecha_evento", row(
                "evento_id", 1L, "fecha_evento", LocalDateTime.of(2026, 8, 27, 9, 15),
                "total", 3L, "canales", "SMS, EMAIL"));

        stubQuery("n.intento_numero > 1", row(
                "evento_id", 1L, "canal", "WHATSAPP", "primera_vez", LocalDateTime.of(2026, 8, 27, 10, 14), "pacientes", 2L));

        stubQuery("GROUP BY tipo", row(
                "tipo", "RECORDATORIO_24H", "primera_vez", LocalDateTime.of(2026, 8, 27, 9, 0), "total", 5L));

        stubQuery("FROM reagendamiento", row(
                "fecha_solicitud", LocalDateTime.of(2026, 8, 27, 11, 0), "paciente_id", 7L));

        Paciente paciente = new Paciente();
        paciente.setNombre("Juana Pérez");
        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));

        BitacoraResponse response = service.obtener(LocalDate.of(2026, 8, 27));

        assertThat(response.getFecha()).isEqualTo("2026-08-27");
        assertThat(response.getEntradas()).hasSize(5);
        // Orden cronológico: recordatorio (09:00) < evento registrado (09:14) < notificaciones (09:15) < escalamiento (10:14) < reagendamiento (11:00)
        assertThat(response.getEntradas()).extracting(BitacoraEntryDto::getTipo)
                .containsExactly("RECORDATORIO", "EVENTO_REGISTRADO", "NOTIFICACIONES_ENVIADAS", "ESCALAMIENTO", "REAGENDAMIENTO");
        assertThat(response.getEntradas().get(1).getTitulo()).isEqualTo("Evento de ausencia registrado — Dra. Fuentes");
        assertThat(response.getEntradas().get(4).getTitulo()).isEqualTo("Reagendamiento confirmado — Juana Pérez");
    }

    @Test
    void devuelveElConteoDeErroresDelDia() {
        when(jdbcTemplate.queryForObject(contains("estado_envio = 'FALLIDO'"), any(SqlParameterSource.class), eqLongClass()))
                .thenReturn(4L);

        BitacoraResponse response = service.obtener(LocalDate.of(2026, 8, 27));

        assertThat(response.getErroresHoy()).isEqualTo(4L);
        assertThat(response.getEntradas()).isEmpty();
    }

    private void stubQuery(String sqlContains, Map<String, Object> data) {
        when(jdbcTemplate.query(contains(sqlContains), any(SqlParameterSource.class), ArgumentMatchersHelper.<Object>rowMapper()))
                .thenAnswer(invocation -> {
                    RowMapper<Object> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(fakeResultSet(data), 1));
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
        lenient().when(rs.getLong(anyStringSafe())).thenAnswer(inv -> {
            Object v = data.get(inv.getArgument(0, String.class));
            return v == null ? 0L : ((Number) v).longValue();
        });
        lenient().when(rs.getString(anyStringSafe())).thenAnswer(inv -> {
            Object v = data.get(inv.getArgument(0, String.class));
            return v == null ? null : v.toString();
        });
        lenient().when(rs.getObject(anyStringSafe(), any(Class.class))).thenAnswer(inv ->
                data.get(inv.getArgument(0, String.class)));
        lenient().when(rs.wasNull()).thenReturn(false);
        return rs;
    }

    private static final class ArgumentMatchersHelper<T> {
        static <T> RowMapper<T> rowMapper() {
            return org.mockito.ArgumentMatchers.any();
        }
    }
}
