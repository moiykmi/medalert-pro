package cl.medalertpro.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${medalert.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un email simple. Incluye una pequeña pausa antes del envío porque
     * el plan gratuito de Mailtrap (sandbox de desarrollo) limita la cantidad de
     * emails por segundo — sin esto, ráfagas de varios pacientes seguidos
     * disparan "550 5.7.0 Too many emails per second". En un proveedor de
     * producción (SendGrid, SES, etc.) este límite es mucho más alto y esta
     * pausa dejaría de ser necesaria.
     */
    public String enviarEmail(String emailDestino, String asunto, String cuerpo) {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(fromAddress);
        mensaje.setTo(emailDestino);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        mailSender.send(mensaje);

        String mensajeId = "mailtrap-" + UUID.randomUUID();
        log.info("Email enviado a {} — id local: {}", emailDestino, mensajeId);
        return mensajeId;
    }
}
