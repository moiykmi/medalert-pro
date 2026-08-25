package cl.medalertpro.notification.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * medalert.admin.token no se sobreescribe vía @DynamicPropertySource, así que se
 * usa el valor por defecto declarado en application.yml (medalert-admin-dev-token).
 */
class AdminDashboardControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String TOKEN_VALIDO = "medalert-admin-dev-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void limpiarDatos() {
        jdbcTemplate.update("DELETE FROM notificacion");
        jdbcTemplate.update("DELETE FROM evento_cancelacion");
        jdbcTemplate.update("DELETE FROM paciente");
        jdbcTemplate.update("DELETE FROM profesional_salud");
        jdbcTemplate.update("DELETE FROM establecimiento");
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void kpisConTokenCorrectoDevuelve200() throws Exception {
        mockMvc.perform(get("/admin/dashboard/kpis").header("X-Admin-Token", TOKEN_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryRate").exists())
                .andExpect(jsonPath("$.deliveryRate.totalNotificaciones").value(0))
                .andExpect(jsonPath("$.contactEffectiveness").exists())
                .andExpect(jsonPath("$.notificationTime").exists());
    }

    @Test
    void kpisSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/admin/dashboard/kpis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void kpisConTokenIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/admin/dashboard/kpis").header("X-Admin-Token", "token-erroneo"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eventosRecientesConTokenCorrectoDevuelve200() throws Exception {
        mockMvc.perform(get("/admin/dashboard/eventos-recientes").header("X-Admin-Token", TOKEN_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void eventosRecientesSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/admin/dashboard/eventos-recientes"))
                .andExpect(status().isUnauthorized());
    }
}
