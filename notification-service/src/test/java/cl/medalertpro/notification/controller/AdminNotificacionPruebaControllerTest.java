package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.PacienteRepository;
import cl.medalertpro.notification.service.AdminAuthGuard;
import cl.medalertpro.notification.service.ConfiguracionService;
import cl.medalertpro.notification.service.MensajeBuilder;
import cl.medalertpro.notification.service.NotificacionDispatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificacionPruebaControllerTest {

    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private ConfiguracionService configuracionService;
    @Mock
    private NotificacionDispatchService dispatchService;
    @Mock
    private AdminAuthGuard authGuard;
    @Mock
    private HttpServletRequest httpRequest;

    private AdminNotificacionPruebaController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminNotificacionPruebaController(pacienteRepository, configuracionService, dispatchService, authGuard, new MensajeBuilder());
    }

    private Paciente paciente() {
        Paciente p = new Paciente();
        p.setId(1L);
        p.setNombre("Ana Rios");
        p.setTelefono("+56911111111");
        p.setEmail("ana@test.cl");
        p.setCanalPreferido("SMS");
        return p;
    }

    @Test
    void enviaPruebaPorElCanalResuelto() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente()));
        when(configuracionService.resolverCanalEnvio("SMS")).thenReturn(Optional.of("SMS"));
        Notificacion enviada = new Notificacion();
        enviada.setEstadoEnvio("ENVIADO");
        when(dispatchService.enviarRecordatorioYRegistrar(isNull(), eq(1L), eq("PRUEBA"), eq("SMS"),
                eq("+56911111111"), eq("ana@test.cl"), anyString(), anyString())).thenReturn(enviada);

        Notificacion resultado = controller.enviarPrueba(1L, httpRequest);

        assertThat(resultado.getEstadoEnvio()).isEqualTo("ENVIADO");
    }

    @Test
    void conPacienteInexistenteLanza404() {
        when(pacienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.enviarPrueba(999L, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void conTodosLosCanalesDeshabilitadosLanza409() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente()));
        when(configuracionService.resolverCanalEnvio("SMS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.enviarPrueba(1L, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }
}
