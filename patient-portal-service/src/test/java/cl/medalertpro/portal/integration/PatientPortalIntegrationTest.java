package cl.medalertpro.portal.integration;

import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.EmailService;
import cl.medalertpro.portal.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PatientPortalIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("medalert")
            .withUsername("medalert")
            .withPassword("medalert_dev_pw");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void propiedadesDinamicas(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private SmsService smsService;

    @MockBean
    private EmailService emailService;

    private static final Pattern CODIGO_PATTERN = Pattern.compile("(\\d{6})");

    @Test
    void citasSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/citas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void flujoCompletoLoginOtpCitasReagendarYActualizarDatos() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setRut("11111111-1");
        paciente.setNombre("Ana Paciente");
        paciente.setTelefono("+56911111111");
        paciente.setEmail("ana@test.cl");
        paciente.setCanalPreferido("SMS");
        paciente.setAdultoMayor(false);
        pacienteRepository.save(paciente);

        jdbcTemplate.update("INSERT INTO establecimiento (id, nombre, servicio_salud) VALUES (1, 'CESFAM Test', 'Servicio Test')");
        jdbcTemplate.update("INSERT INTO profesional_salud (id, nombre, especialidad, establecimiento_id) VALUES (1, 'Dr. Test', 'Medicina General', 1)");
        jdbcTemplate.update("INSERT INTO cita (id, paciente_id, profesional_id, fecha_hora, estado) VALUES (1, 1, 1, ?, 'CANCELADA')",
                LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/auth/solicitar-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"11111111-1\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> textoCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsService).enviarSms(anyString(), textoCaptor.capture());
        Matcher matcher = CODIGO_PATTERN.matcher(textoCaptor.getValue());
        assertThat(matcher.find()).isTrue();
        String codigo = matcher.group(1);

        String bodyVerificar = objectMapper.writeValueAsString(new VerificarOtpBody("11111111-1", codigo));
        String respuestaJson = mockMvc.perform(post("/auth/verificar-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyVerificar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.not(emptyOrNullString())))
                .andExpect(jsonPath("$.pacienteId").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana Paciente"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(respuestaJson).get("token").asText();

        mockMvc.perform(get("/citas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("CANCELADA"));

        LocalDateTime nuevaFecha = LocalDateTime.now().plusDays(5).withNano(0);
        String bodyReagendar = "{\"nuevaFechaHora\":\"" + nuevaFecha + "\"}";

        mockMvc.perform(post("/citas/1/reagendar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyReagendar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AGENDADA"))
                .andExpect(jsonPath("$.pacienteId").value(1));

        mockMvc.perform(put("/paciente/datos-contacto")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"+56922222222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("+56922222222"))
                .andExpect(jsonPath("$.email").value("ana@test.cl"));
    }

    private record VerificarOtpBody(String rut, String codigo) {
    }
}
