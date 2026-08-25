package cl.medalertpro.notification.integration;

import cl.medalertpro.notification.service.EmailService;
import cl.medalertpro.notification.service.SmsService;
import cl.medalertpro.notification.service.WhatsAppService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para las pruebas de integración del notification-service: levanta Postgres,
 * RabbitMQ y Redis reales vía Testcontainers, y sustituye los servicios que hablan
 * con proveedores externos (Twilio, SMTP) por mocks para no hacer llamadas reales.
 * Requiere un daemon de Docker accesible; si no está disponible, estas pruebas
 * fallan al arrancar los contenedores (no es un fallo del código bajo prueba).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("medalert")
            .withUsername("medalert")
            .withPassword("medalert_dev_pw");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management-alpine"))
            .withUser("medalert", "medalert_dev_pw");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registrarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "medalert");
        registry.add("spring.rabbitmq.password", () -> "medalert_dev_pw");

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // Credenciales dummy: TwilioConfig y JavaMailSender solo necesitan valores
        // resolubles para arrancar el contexto; SmsService/WhatsAppService/EmailService
        // están mockeados con @MockBean, así que nunca se hace una llamada real.
        registry.add("medalert.twilio.account-sid", () -> "ACtest0000000000000000000000000");
        registry.add("medalert.twilio.auth-token", () -> "test-token");
        registry.add("medalert.twilio.sms-from-number", () -> "+10000000000");
        registry.add("spring.mail.username", () -> "test-mail-user");
        registry.add("spring.mail.password", () -> "test-mail-pass");
    }

    @MockBean
    protected SmsService smsService;

    @MockBean
    protected WhatsAppService whatsAppService;

    @MockBean
    protected EmailService emailService;
}
