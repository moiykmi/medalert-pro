package cl.medalertpro.notification.config;

import com.twilio.Twilio;
import com.twilio.security.RequestValidator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Inicializa el SDK de Twilio con las credenciales de la cuenta al arrancar la app.
 * Las credenciales se leen desde variables de entorno (ver .env) — nunca hardcodeadas.
 */
@Component
public class TwilioConfig {

    @Value("${medalert.twilio.account-sid}")
    private String accountSid;

    @Value("${medalert.twilio.auth-token}")
    private String authToken;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    /** Usado por TwilioWebhookController para validar la firma de los callbacks entrantes. */
    @Bean
    public RequestValidator twilioRequestValidator() {
        return new RequestValidator(authToken);
    }
}
