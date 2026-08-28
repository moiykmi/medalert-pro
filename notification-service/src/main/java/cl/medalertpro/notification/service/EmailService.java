package cl.medalertpro.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envía por la API HTTP del sandbox de Mailtrap, no por SMTP: Railway bloquea
 * la salida por los puertos SMTP habituales (25/587/2525) — confirmado en
 * producción, colgaba/fallaba con "Connect timed out" en ambos puertos —,
 * mientras que el tráfico HTTPS normal (puerto 443) sí sale sin problema.
 * Sigue siendo el inbox de prueba (no entrega a bandejas reales) — solo
 * cambia el transporte, no el destino final del mensaje.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${medalert.mailtrap.api-token}")
    private String apiToken;

    @Value("${medalert.mailtrap.inbox-id}")
    private String inboxId;

    @Value("${medalert.mail.from}")
    private String fromAddress;

    /**
     * Envía un email vía la API de Mailtrap y devuelve el message_id (para
     * guardarlo como proveedor_message_id en la tabla notificacion). Lanza
     * excepción si falla, para que el llamador la capture y marque la
     * notificación como FALLIDO.
     */
    public String enviarEmail(String emailDestino, String asunto, String cuerpo) {
        try {
            URI endpoint = URI.create("https://sandbox.api.mailtrap.io/api/send/" + inboxId);

            ObjectNode body = objectMapper.createObjectNode();
            body.putObject("from").put("email", fromAddress);
            body.putArray("to").addObject().put("email", emailDestino);
            body.put("subject", asunto);
            body.put("text", cuerpo);

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new IOException("Mailtrap respondió " + response.statusCode() + ": " + response.body());
            }

            JsonNode responseBody = objectMapper.readTree(response.body());
            String messageId = responseBody.path("message_ids").path(0).asText("mailtrap-sin-id");
            log.info("Email enviado a {} vía Mailtrap — id: {}", emailDestino, messageId);
            return messageId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Envío de email interrumpido", e);
        } catch (IOException e) {
            throw new RuntimeException("Fallo al enviar email vía Mailtrap: " + e.getMessage(), e);
        }
    }
}
