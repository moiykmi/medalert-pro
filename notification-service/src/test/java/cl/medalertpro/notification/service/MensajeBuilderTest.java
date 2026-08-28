package cl.medalertpro.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeBuilderTest {

    private final MensajeBuilder mensajeBuilder = new MensajeBuilder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mensajeBuilder, "portalUrl", "https://portal.test");
    }

    @Test
    void construyeMensajeConNombreMotivoYUrlDelPortal() {
        String mensaje = mensajeBuilder.construir("Juan Perez", "falta de personal");

        assertThat(mensaje).isEqualTo(
                "MedAlert Pro: Juan Perez, su cita fue CANCELADA (motivo: falta de personal). "
                        + "Ingrese a https://portal.test para confirmar o reagendar.");
    }

    @Test
    void incluyeNombreMotivoYUrlDiferentesEnElTexto() {
        String mensaje = mensajeBuilder.construir("Maria Soto", "emergencia medica");

        assertThat(mensaje)
                .startsWith("MedAlert Pro:")
                .contains("Maria Soto")
                .contains("emergencia medica")
                .contains("CANCELADA")
                .contains("https://portal.test");
    }

    @Test
    void noLanzaExcepcionConMotivoNulo() {
        String mensaje = mensajeBuilder.construir("Pedro Rojas", null);

        assertThat(mensaje).contains("Pedro Rojas").contains("motivo: null");
    }

    @Test
    void construyeRecordatorioConUrlDelPortal() {
        String mensaje = mensajeBuilder.construirRecordatorio("Juan Perez", LocalDateTime.of(2026, 8, 28, 10, 30), 24);

        assertThat(mensaje)
                .contains("Juan Perez")
                .contains("28/08 a las 10:30")
                .contains("24h")
                .contains("https://portal.test");
    }

    @Test
    void construyePruebaSinUrlDelPortal() {
        String mensaje = mensajeBuilder.construirPrueba("Juan Perez");

        assertThat(mensaje)
                .contains("Juan Perez")
                .contains("prueba")
                .doesNotContain("https://portal.test");
    }
}
