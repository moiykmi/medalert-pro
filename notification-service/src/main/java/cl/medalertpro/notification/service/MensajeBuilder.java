package cl.medalertpro.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MensajeBuilder {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM 'a las' HH:mm");

    @Value("${medalert.portal.url}")
    private String portalUrl;

    /**
     * Mensaje corto y sin tildes a propósito: las cuentas trial de Twilio limitan
     * el SMS a un solo segmento (160 caracteres GSM-7, o solo 70 si hay tildes/ñ,
     * que fuerzan codificación UCS-2). Ver Error 30044 "Trial Message Length Exceeded".
     * Ojo: el nombre del paciente y el motivo sí pueden traer tildes (vienen de
     * datos reales) — agregar la URL empuja el mensaje más cerca de ese límite.
     */
    public String construir(String nombrePaciente, String motivo) {
        return String.format(
                "MedAlert Pro: %s, su cita fue CANCELADA (motivo: %s). Ingrese a %s para confirmar o reagendar.",
                nombrePaciente, motivo, portalUrl);
    }

    /** Mismo criterio sin tildes que construir(); horasAntes es 48 o 24. */
    public String construirRecordatorio(String nombrePaciente, LocalDateTime fechaHoraCita, int horasAntes) {
        return String.format(
                "MedAlert Pro: %s, recordatorio de su cita el %s (en %dh). Ingrese a %s para confirmar o reagendar.",
                nombrePaciente, fechaHoraCita.format(FORMATO_FECHA), horasAntes, portalUrl);
    }

    /** Enviado manualmente desde el panel admin para verificar que un canal llega al paciente. */
    public String construirPrueba(String nombrePaciente) {
        return String.format(
                "MedAlert Pro: %s, este es un mensaje de prueba enviado desde el panel administrativo. Puede ignorarlo.",
                nombrePaciente);
    }
}
