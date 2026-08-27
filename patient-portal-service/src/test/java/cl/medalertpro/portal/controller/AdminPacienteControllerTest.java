package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.entity.Notificacion;
import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.NotificacionRepository;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPacienteControllerTest {

    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private AdminAuthGuard authGuard;
    @Mock
    private HttpServletRequest httpRequest;

    private AdminPacienteController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminPacienteController(pacienteRepository, notificacionRepository, authGuard);
    }

    private Notificacion notificacion(Long pacienteId) {
        Notificacion n = new Notificacion();
        n.setId(1L);
        n.setPacienteId(pacienteId);
        n.setCanal("SMS");
        n.setEstadoEnvio("CONFIRMADO");
        return n;
    }

    @Test
    void listarDevuelveTodosLosPacientes() {
        when(pacienteRepository.findAll()).thenReturn(List.of(new Paciente()));

        List<Paciente> resultado = controller.listar(httpRequest);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void historialNotificacionesDevuelveLasDelPaciente() {
        when(pacienteRepository.existsById(7L)).thenReturn(true);
        when(notificacionRepository.findByPacienteIdOrderByEnviadoEnDesc(7L)).thenReturn(List.of(notificacion(7L)));

        List<Notificacion> resultado = controller.historialNotificaciones(7L, httpRequest);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPacienteId()).isEqualTo(7L);
    }

    @Test
    void historialNotificacionesConPacienteInexistenteLanza404() {
        when(pacienteRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> controller.historialNotificaciones(999L, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
