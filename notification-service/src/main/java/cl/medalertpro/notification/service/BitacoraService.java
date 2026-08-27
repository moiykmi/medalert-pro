package cl.medalertpro.notification.service;

import cl.medalertpro.notification.dto.BitacoraEntryDto;
import cl.medalertpro.notification.dto.BitacoraResponse;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.PacienteRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bitácora de un día: reconstruye la cronología real de eventos del sistema
 * (evento_cancelacion, notificacion, reagendamiento) — no es un registro
 * separado que haya que mantener, se arma en el momento a partir de las
 * mismas tablas que ya alimentan el resto del dashboard.
 */
@Service
public class BitacoraService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PacienteRepository pacienteRepository;

    public BitacoraService(NamedParameterJdbcTemplate jdbcTemplate, PacienteRepository pacienteRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.pacienteRepository = pacienteRepository;
    }

    public BitacoraResponse obtener(LocalDate fecha) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();

        List<BitacoraEntryDto> entradas = new ArrayList<>();
        entradas.addAll(cargarEventosRegistrados(desde, hasta));
        entradas.addAll(cargarNotificacionesEnviadas(desde, hasta));
        entradas.addAll(cargarEscalamientos(desde, hasta));
        entradas.addAll(cargarRecordatorios(desde, hasta));
        entradas.addAll(cargarReagendamientos(desde, hasta));
        entradas.sort(Comparator.comparing(BitacoraEntryDto::getFecha));

        BitacoraResponse response = new BitacoraResponse();
        response.setFecha(fecha.toString());
        response.setEntradas(entradas);
        response.setErroresHoy(contarErrores(desde, hasta));
        return response;
    }

    private MapSqlParameterSource rango(LocalDateTime desde, LocalDateTime hasta) {
        return new MapSqlParameterSource().addValue("desde", desde).addValue("hasta", hasta);
    }

    private List<BitacoraEntryDto> cargarEventosRegistrados(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT e.id AS evento_id, e.fecha_evento, e.motivo, prof.nombre AS profesional_nombre
                FROM evento_cancelacion e
                LEFT JOIN profesional_salud prof ON prof.id = e.profesional_id
                WHERE e.fecha_evento >= :desde AND e.fecha_evento < :hasta
                """;
        return jdbcTemplate.query(sql, rango(desde, hasta), (rs, rowNum) -> {
            BitacoraEntryDto dto = new BitacoraEntryDto();
            dto.setFecha(rs.getObject("fecha_evento", LocalDateTime.class));
            dto.setTipo("EVENTO_REGISTRADO");
            String profesional = rs.getString("profesional_nombre");
            dto.setTitulo("Evento de ausencia registrado" + (profesional != null ? " — " + profesional : ""));
            String motivo = rs.getString("motivo");
            dto.setDetalle("Motivo: " + (motivo != null ? motivo : "sin motivo") + " · Evento #" + rs.getLong("evento_id"));
            return dto;
        });
    }

    private List<BitacoraEntryDto> cargarNotificacionesEnviadas(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT e.id AS evento_id, e.fecha_evento, COUNT(*) AS total, STRING_AGG(DISTINCT n.canal, ', ') AS canales
                FROM evento_cancelacion e
                JOIN notificacion n ON n.evento_id = e.id AND n.intento_numero = 1
                WHERE e.fecha_evento >= :desde AND e.fecha_evento < :hasta
                GROUP BY e.id, e.fecha_evento
                """;
        return jdbcTemplate.query(sql, rango(desde, hasta), (rs, rowNum) -> {
            BitacoraEntryDto dto = new BitacoraEntryDto();
            dto.setFecha(rs.getObject("fecha_evento", LocalDateTime.class));
            dto.setTipo("NOTIFICACIONES_ENVIADAS");
            dto.setTitulo(rs.getLong("total") + " notificaciones publicadas — evento #" + rs.getLong("evento_id"));
            dto.setDetalle("Canales: " + rs.getString("canales"));
            return dto;
        });
    }

    private List<BitacoraEntryDto> cargarEscalamientos(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT n.evento_id, n.canal, MIN(n.enviado_en) AS primera_vez, COUNT(DISTINCT n.paciente_id) AS pacientes
                FROM notificacion n
                WHERE n.tipo = 'CANCELACION' AND n.intento_numero > 1
                  AND n.enviado_en >= :desde AND n.enviado_en < :hasta
                GROUP BY n.evento_id, n.canal
                """;
        return jdbcTemplate.query(sql, rango(desde, hasta), (rs, rowNum) -> {
            BitacoraEntryDto dto = new BitacoraEntryDto();
            dto.setFecha(rs.getObject("primera_vez", LocalDateTime.class));
            dto.setTipo("ESCALAMIENTO");
            dto.setTitulo("Escalamiento automático a " + rs.getString("canal") + " — evento #" + rs.getLong("evento_id"));
            dto.setDetalle(rs.getLong("pacientes") + " paciente(s) sin respuesta en el canal anterior");
            return dto;
        });
    }

    private List<BitacoraEntryDto> cargarRecordatorios(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT tipo, MIN(enviado_en) AS primera_vez, COUNT(*) AS total
                FROM notificacion
                WHERE tipo IN ('RECORDATORIO_48H', 'RECORDATORIO_24H')
                  AND enviado_en >= :desde AND enviado_en < :hasta
                GROUP BY tipo
                """;
        return jdbcTemplate.query(sql, rango(desde, hasta), (rs, rowNum) -> {
            BitacoraEntryDto dto = new BitacoraEntryDto();
            dto.setFecha(rs.getObject("primera_vez", LocalDateTime.class));
            dto.setTipo("RECORDATORIO");
            String etiqueta = "RECORDATORIO_48H".equals(rs.getString("tipo")) ? "48h" : "24h";
            dto.setTitulo(rs.getLong("total") + " recordatorios " + etiqueta + " enviados");
            return dto;
        });
    }

    private List<BitacoraEntryDto> cargarReagendamientos(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT fecha_solicitud, paciente_id
                FROM reagendamiento
                WHERE estado = 'CONFIRMADO' AND fecha_solicitud >= :desde AND fecha_solicitud < :hasta
                """;
        return jdbcTemplate.query(sql, rango(desde, hasta), (rs, rowNum) -> {
            BitacoraEntryDto dto = new BitacoraEntryDto();
            dto.setFecha(rs.getObject("fecha_solicitud", LocalDateTime.class));
            dto.setTipo("REAGENDAMIENTO");
            Paciente paciente = pacienteRepository.findById(rs.getLong("paciente_id")).orElse(null);
            dto.setTitulo("Reagendamiento confirmado" + (paciente != null ? " — " + paciente.getNombre() : ""));
            return dto;
        });
    }

    private long contarErrores(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT COUNT(*) FROM notificacion
                WHERE estado_envio = 'FALLIDO' AND created_at >= :desde AND created_at < :hasta
                """;
        Long total = jdbcTemplate.queryForObject(sql, rango(desde, hasta), Long.class);
        return total == null ? 0 : total;
    }
}
