package cl.medalertpro.notification.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${medalert.twilio.whatsapp-from-number}")
    private String fromNumber; // formato Twilio sandbox: whatsapp:+14155238886

    @Value("${medalert.public-url.self}/webhooks/twilio/estado-mensaje")
    private String statusCallbackUrl;

    /**
     * Envía un WhatsApp. El paciente debe haberse unido al sandbox de Twilio
     * primero (mensaje "join <palabra-clave>" al número de sandbox, sesión que
     * expira a las 72h de inactividad) — a diferencia de SMS, esto no tiene la
     * restricción geográfica de cuentas trial para Chile. Twilio puede aceptar
     * el envío (SID válido) y aun así no entregarlo si esa sesión venció — el
     * statusCallback es lo único que nos avisa de esa falla real.
     */
    public String enviarWhatsApp(String telefonoDestino, String textoMensaje) {
        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + telefonoDestino),
                new PhoneNumber(fromNumber),
                textoMensaje
        ).setStatusCallback(URI.create(statusCallbackUrl)).create();

        log.info("WhatsApp enviado a {} — SID: {} — estado: {}", telefonoDestino, message.getSid(), message.getStatus());
        return message.getSid();
    }
}
