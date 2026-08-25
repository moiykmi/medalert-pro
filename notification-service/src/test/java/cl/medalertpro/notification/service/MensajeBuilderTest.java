package cl.medalertpro.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeBuilderTest {

    @Test
    void construyeMensajeConNombreYMotivo() {
        String mensaje = MensajeBuilder.construir("Juan Perez", "falta de personal");

        assertThat(mensaje).isEqualTo(
                "MedAlert Pro: Juan Perez, su cita fue CANCELADA (motivo: falta de personal). Contacte al consultorio para reagendar.");
    }

    @Test
    void incluyeNombreYMotivoDiferentesEnElTexto() {
        String mensaje = MensajeBuilder.construir("Maria Soto", "emergencia medica");

        assertThat(mensaje)
                .startsWith("MedAlert Pro:")
                .contains("Maria Soto")
                .contains("emergencia medica")
                .contains("CANCELADA");
    }

    @Test
    void noLanzaExcepcionConMotivoNulo() {
        String mensaje = MensajeBuilder.construir("Pedro Rojas", null);

        assertThat(mensaje).contains("Pedro Rojas").contains("motivo: null");
    }
}
