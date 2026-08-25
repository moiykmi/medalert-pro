package cl.medalertpro.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara la misma topología que fhir-integration-service (exchange, cola, binding).
 * Esto es idempotente: si ya existe (porque fhir-integration-service arrancó primero),
 * RabbitMQ no hace nada; si no existe todavía, la crea. Así notification-service puede
 * arrancar en cualquier orden respecto al otro microservicio.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${medalert.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${medalert.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${medalert.rabbitmq.queue}")
    private String queueName;

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue cancelacionesQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding binding(Queue cancelacionesQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(cancelacionesQueue).to(eventosExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
