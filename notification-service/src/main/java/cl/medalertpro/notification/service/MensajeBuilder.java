package cl.medalertpro.notification.service;

public class MensajeBuilder {

    private MensajeBuilder() {}

    /**
     * Mensaje corto y sin tildes a propósito: las cuentas trial de Twilio limitan
     * el SMS a un solo segmento (160 caracteres GSM-7, o solo 70 si hay tildes/ñ,
     * que fuerzan codificación UCS-2). Ver Error 30044 "Trial Message Length Exceeded".
     */
    public static String construir(String nombrePaciente, String motivo) {
        return String.format(
                "MedAlert Pro: %s, su cita fue CANCELADA (motivo: %s). Contacte al consultorio para reagendar.",
                nombrePaciente, motivo);
    }
}
