package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.repository.NotificacionRepository;
import com.twilio.security.RequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Recibe el statusCallback de Twilio (configurado en SmsService/WhatsAppService)
 * con el estado REAL de entrega de cada mensaje — distinto de estadoEnvio, que
 * solo refleja si Twilio aceptó el envío de forma síncrona. Endpoint público
 * (Twilio no manda ningún token nuestro), protegido validando la firma
 * X-Twilio-Signature con el auth token de la cuenta (ver TwilioConfig).
 */
@RestController
public class TwilioWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TwilioWebhookController.class);
    private static final Set<String> ESTADOS_FINALES_DE_ENTREGA = Set.of("delivered", "read");

    private final NotificacionRepository notificacionRepository;
    private final RequestValidator requestValidator;

    @Value("${medalert.public-url.self}/webhooks/twilio/estado-mensaje")
    private String callbackUrl;

    public TwilioWebhookController(NotificacionRepository notificacionRepository, RequestValidator requestValidator) {
        this.notificacionRepository = notificacionRepository;
        this.requestValidator = requestValidator;
    }

    @PostMapping(value = "/webhooks/twilio/estado-mensaje", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> recibirEstadoMensaje(
            @RequestParam MultiValueMap<String, String> parametros,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String firma) {

        if (firma == null || !requestValidator.validate(callbackUrl, parametros.toSingleValueMap(), firma)) {
            log.warn("Callback de Twilio con firma inválida o ausente — se descarta");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String messageSid = parametros.getFirst("MessageSid");
        String messageStatus = parametros.getFirst("MessageStatus");
        if (messageSid == null || messageStatus == null) {
            return ResponseEntity.badRequest().build();
        }

        notificacionRepository.findByProveedorMessageId(messageSid).ifPresentOrElse(notificacion -> {
            notificacion.setEstadoEntrega(messageStatus);
            if (ESTADOS_FINALES_DE_ENTREGA.contains(messageStatus)) {
                notificacion.setEntregadoEn(LocalDateTime.now());
            }
            notificacionRepository.save(notificacion);
            log.info("Notificación {} (SID {}) actualizada a estado_entrega={}",
                    notificacion.getId(), messageSid, messageStatus);
        }, () -> log.warn("Callback de Twilio para SID {} sin notificación asociada — se ignora", messageSid));

        return ResponseEntity.ok().build();
    }
}
