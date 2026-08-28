package cl.medalertpro.portal.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reglas de negocio del horario de atención, aplicadas a toda cita nueva
 * (reagendamiento del paciente o creación manual desde el admin): bloques
 * fijos de 30 minutos, de 08:00 a 18:00 (último bloque empieza a las 17:30),
 * sin agendar entre las 13:00 y las 14:00 (colación). No exige que la hora
 * caiga justo en :00/:30 — solo que el bloque completo quepa dentro de la
 * jornada y fuera de colación.
 */
public class ReglaHorarioCita {

    public static final int DURACION_MINUTOS = 30;

    private static final LocalTime INICIO_JORNADA = LocalTime.of(8, 0);
    private static final LocalTime ULTIMO_BLOQUE = LocalTime.of(17, 30);
    private static final LocalTime INICIO_COLACION = LocalTime.of(13, 0);
    private static final LocalTime FIN_COLACION = LocalTime.of(14, 0);

    private ReglaHorarioCita() {}

    public static void validar(LocalDateTime fechaHora) {
        LocalTime hora = fechaHora.toLocalTime();
        if (hora.isBefore(INICIO_JORNADA) || hora.isAfter(ULTIMO_BLOQUE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las citas solo se pueden agendar entre las 08:00 y las 18:00 (último bloque: 17:30)");
        }
        if (!hora.isBefore(INICIO_COLACION) && hora.isBefore(FIN_COLACION)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pueden agendar citas entre las 13:00 y las 14:00 (colación)");
        }
    }

    /** Ventana [inicio, fin] a usar en la consulta de solapamiento: cualquier otra cita
     * del mismo profesional cuya hora caiga aquí choca con un bloque de 30 min en fechaHora. */
    public static LocalDateTime inicioVentanaSolapamiento(LocalDateTime fechaHora) {
        return fechaHora.minusMinutes(DURACION_MINUTOS - 1);
    }

    public static LocalDateTime finVentanaSolapamiento(LocalDateTime fechaHora) {
        return fechaHora.plusMinutes(DURACION_MINUTOS - 1);
    }
}
