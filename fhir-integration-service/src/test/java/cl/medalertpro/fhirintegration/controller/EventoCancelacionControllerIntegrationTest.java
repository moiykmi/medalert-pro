package cl.medalertpro.fhirintegration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class EventoCancelacionControllerIntegrationTest {

    // Se fijan las mismas imágenes que usa docker-compose.yml en la raíz del proyecto.
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("medalert")
            .withUsername("medalert")
            .withPassword("medalert_dev_pw");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitmq.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Long profesionalId;
    private Long pacienteId;
    private Long citaId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM notificacion");
        jdbcTemplate.update("DELETE FROM evento_cancelacion");
        jdbcTemplate.update("DELETE FROM cita");
        jdbcTemplate.update("DELETE FROM paciente");
        jdbcTemplate.update("DELETE FROM profesional_salud");
        jdbcTemplate.update("DELETE FROM usuario_admin");
        jdbcTemplate.update("DELETE FROM establecimiento");

        Long establecimientoId = jdbcTemplate.queryForObject(
                "INSERT INTO establecimiento (nombre, servicio_salud) VALUES (?, ?) RETURNING id",
                Long.class, "CESFAM Test", "Servicio Salud Test");

        profesionalId = jdbcTemplate.queryForObject(
                "INSERT INTO profesional_salud (nombre, especialidad, establecimiento_id) VALUES (?, ?, ?) RETURNING id",
                Long.class, "Dra. Ana Rojas", "Medicina General", establecimientoId);

        pacienteId = jdbcTemplate.queryForObject(
                "INSERT INTO paciente (rut, nombre, telefono, email, canal_preferido) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, "11111111-1", "Juana Pérez", "+56911111111", "juana@example.com", "SMS");

        citaId = jdbcTemplate.queryForObject(
                "INSERT INTO cita (paciente_id, profesional_id, fecha_hora, estado) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, pacienteId, profesionalId, LocalDateTime.of(2026, 8, 1, 9, 0), "AGENDADA");

        rabbitTemplate.receive("cancelaciones.eventos", 100);
    }

    @Test
    void registrarCancelacion_marcaCitasComoCanceladasYPublicaEnRabbit() throws Exception {
        String body = """
                {
                  "profesionalId": %d,
                  "fecha": "2026-08-01",
                  "motivo": "Licencia médica"
                }
                """.formatted(profesionalId);

        mockMvc.perform(post("/eventos/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.profesionalId").value(profesionalId));

        String estadoCita = jdbcTemplate.queryForObject(
                "SELECT estado FROM cita WHERE id = ?", String.class, citaId);
        assertThat(estadoCita).isEqualTo("CANCELADA");

        Integer eventosCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM evento_cancelacion WHERE profesional_id = ?", Integer.class, profesionalId);
        assertThat(eventosCount).isEqualTo(1);

        Message message = rabbitTemplate.receive("cancelaciones.eventos", 5000);
        assertThat(message).isNotNull();

        JsonNode json = objectMapper.readTree(message.getBody());
        assertThat(json.get("profesionalId").asLong()).isEqualTo(profesionalId);
        assertThat(json.get("pacientesAfectados")).hasSize(1);
        assertThat(json.get("pacientesAfectados").get(0).get("pacienteId").asLong()).isEqualTo(pacienteId);
    }

    @Test
    void registrarCancelacion_sinCitasAgendadas_noModificaCitasYPublicaListaVacia() throws Exception {
        jdbcTemplate.update("UPDATE cita SET estado = 'ATENDIDA' WHERE id = ?", citaId);

        String body = """
                {
                  "profesionalId": %d,
                  "fecha": "2026-08-01",
                  "motivo": "Sin citas agendadas"
                }
                """.formatted(profesionalId);

        mockMvc.perform(post("/eventos/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));

        String estadoCita = jdbcTemplate.queryForObject(
                "SELECT estado FROM cita WHERE id = ?", String.class, citaId);
        assertThat(estadoCita).isEqualTo("ATENDIDA");

        Message message = rabbitTemplate.receive("cancelaciones.eventos", 5000);
        assertThat(message).isNotNull();

        JsonNode json = objectMapper.readTree(message.getBody());
        assertThat(json.get("pacientesAfectados")).isEmpty();
    }
}
