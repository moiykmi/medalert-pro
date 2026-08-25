package cl.medalertpro.portal.service;

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

    public void enviarSms(String telefonoDestino, String textoMensaje) {
        Message message = Message.creator(
                new PhoneNumber(telefonoDestino),
                new PhoneNumber(fromNumber),
                textoMensaje
        ).create();
        log.info("SMS OTP enviado a {} — SID: {}", telefonoDestino, message.getSid());
    }
}
