package cl.medalertpro.notification.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${medalert.twilio.sms-from-number}")
    private String fromNumber;

    /**
     * Envía un SMS y devuelve el SID del mensaje de Twilio (para guardarlo como
     * proveedor_message_id en la tabla notificacion). Lanza excepción si falla,
     * para que el llamador la capture y marque la notificación como FALLIDO.
     */
    public String enviarSms(String telefonoDestino, String textoMensaje) {
        Message message = Message.creator(
                new PhoneNumber(telefonoDestino),
                new PhoneNumber(fromNumber),
                textoMensaje
        ).create();

        log.info("SMS enviado a {} — SID: {} — estado: {}", telefonoDestino, message.getSid(), message.getStatus());
        return message.getSid();
    }
}
