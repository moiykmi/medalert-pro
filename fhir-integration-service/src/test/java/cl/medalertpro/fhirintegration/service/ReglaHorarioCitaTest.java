package cl.medalertpro.fhirintegration.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglaHorarioCitaTest {

    private static LocalDateTime enHora(int hora, int minuto) {
        return LocalDateTime.of(2026, 9, 17, hora, minuto);
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:00", "09:15", "12:59", "14:00", "16:45", "17:30"})
    void aceptaHorariosDentroDeLaJornadaYFueraDeColacion(String horaTexto) {
        String[] partes = horaTexto.split(":");
        assertThatCode(() -> ReglaHorarioCita.validar(enHora(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]))))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaAntesDeLas8am() {
        assertThatThrownBy(() -> ReglaHorarioCita.validar(enHora(7, 59)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rechazaDespuesDelUltimoBloqueDeLas1730() {
        assertThatThrownBy(() -> ReglaHorarioCita.validar(enHora(17, 31)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @ParameterizedTest
    @ValueSource(strings = {"13:00", "13:15", "13:30", "13:59"})
    void rechazaHorariosDeColacion(String horaTexto) {
        String[] partes = horaTexto.split(":");
        assertThatThrownBy(() -> ReglaHorarioCita.validar(enHora(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void ventanaDeSolapamientoCubreExactamenteLosTreintaMinutosDelBloque() {
        LocalDateTime fecha = enHora(10, 0);

        assertThat(ReglaHorarioCita.inicioVentanaSolapamiento(fecha)).isEqualTo(enHora(9, 31));
        assertThat(ReglaHorarioCita.finVentanaSolapamiento(fecha)).isEqualTo(enHora(10, 29));
    }
}
