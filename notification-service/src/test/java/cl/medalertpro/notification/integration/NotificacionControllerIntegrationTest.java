package cl.medalertpro.notification.integration;

import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificacionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificacionRepository notificacionRepository;

    private Long notificacionId;

    @BeforeEach
    void crearDatosDePrueba() {
        jdbcTemplate.update("DELETE FROM notificacion");
        jdbcTemplate.update("DELETE FROM evento_cancelacion");
        jdbcTemplate.update("DELETE FROM paciente");
        jdbcTemplate.update("DELETE FROM profesional_salud");
        jdbcTemplate.update("DELETE FROM establecimiento");

        jdbcTemplate.update("INSERT INTO establecimiento (id, nombre, servicio_salud) VALUES (1, 'CESFAM Test', 'Servicio Salud Test')");
        jdbcTemplate.update("INSERT INTO profesional_salud (id, nombre, especialidad, establecimiento_id) VALUES (1, 'Dr. Test', 'Medicina General', 1)");
        jdbcTemplate.update("INSERT INTO paciente (id, rut, nombre, telefono, email, canal_preferido) VALUES (1, '11111111-1', 'Paciente Test', '+56911111111', 'paciente@test.cl', 'SMS')");
        jdbcTemplate.update("INSERT INTO evento_cancelacion (id, profesional_id, motivo) VALUES (1, 1, 'motivo de prueba')");

        Notificacion notificacion = new Notificacion();
        notificacion.setEventoId(1L);
        notificacion.setPacienteId(1L);
        notificacion.setCanal("SMS");
        notificacion.setIntentoNumero((short) 1);
        notificacion.setEstadoEnvio("ENVIADO");
        notificacionId = notificacionRepository.save(notificacion).getId();
    }

    @Test
    void listarDevuelveLasNotificacionesRegistradas() throws Exception {
        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(notificacionId))
                .andExpect(jsonPath("$[0].canal").value("SMS"))
                .andExpect(jsonPath("$[0].estadoEnvio").value("ENVIADO"));
    }

    @Test
    void confirmarActualizaEstadoYFechaDeConfirmacion() throws Exception {
        mockMvc.perform(post("/notificaciones/{id}/confirmar", notificacionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificacionId))
                .andExpect(jsonPath("$.estadoEnvio").value("CONFIRMADO"))
                .andExpect(jsonPath("$.confirmadoEn").isNotEmpty());

        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoEnvio").value("CONFIRMADO"));
    }

    @Test
    void confirmarNotificacionInexistenteRespondeError() throws Exception {
        mockMvc.perform(post("/notificaciones/{id}/confirmar", 999999))
                .andExpect(status().is5xxServerError());
    }
}
