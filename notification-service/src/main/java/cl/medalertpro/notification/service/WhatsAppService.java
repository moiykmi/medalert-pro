package cl.medalertpro.notification.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${medalert.twilio.whatsapp-from-number}")
    private String fromNumber; // formato Twilio sandbox: whatsapp:+14155238886

    /**
     * Envía un WhatsApp. El paciente debe haberse unido al sandbox de Twilio
     * primero (mensaje "join <palabra-clave>" al número de sandbox) — a diferencia
     * de SMS, esto no tiene la restricción geográfica de cuentas trial para Chile.
     */
    public String enviarWhatsApp(String telefonoDestino, String textoMensaje) {
        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + telefonoDestino),
                new PhoneNumber(fromNumber),
                textoMensaje
        ).create();

        log.info("WhatsApp enviado a {} — SID: {} — estado: {}", telefonoDestino, message.getSid(), message.getStatus());
        return message.getSid();
    }
}
