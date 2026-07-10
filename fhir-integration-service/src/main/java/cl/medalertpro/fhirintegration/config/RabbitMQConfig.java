package cl.medalertpro.fhirintegration.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Define el exchange, la cola y el binding que usará el motor de notificaciones
 * (notification-service) para consumir los eventos de cancelación publicados aquí.
 *
 * Cola resultante: "cancelaciones.eventos"
 * Ábrela en la consola de RabbitMQ: http://localhost:15672 -> Queues
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
        // durable=true: la cola sobrevive a un reinicio de RabbitMQ
        return new Queue(queueName, true);
    }

    @Bean
    public Binding binding(Queue cancelacionesQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(cancelacionesQueue).to(eventosExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Serializa/deserializa los mensajes como JSON en vez de Java serializado por defecto
        return new Jackson2JsonMessageConverter();
    }
}
