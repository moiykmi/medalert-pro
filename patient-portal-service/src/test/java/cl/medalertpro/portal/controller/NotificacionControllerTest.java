package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.entity.Notificacion;
import cl.medalertpro.portal.repository.NotificacionRepository;
import cl.medalertpro.portal.service.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private AuthGuard authGuard;
    @Mock
    private HttpServletRequest httpRequest;

    private NotificacionController notificacionController;

    @BeforeEach
    void setUp() {
        notificacionController = new NotificacionController(notificacionRepository, authGuard);
    }

    private Notificacion notificacion() {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(1L);
        notificacion.setEventoId(1L);
        notificacion.setPacienteId(1L);
        notificacion.setCanal("SMS");
        notificacion.setEstadoEnvio("ENVIADO");
        return notificacion;
    }

    @Test
    void misNotificacionesRetornaLasDelPacienteAutenticado() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(notificacionRepository.findByPacienteIdOrderByEnviadoEnDesc(1L)).thenReturn(List.of(notificacion()));

        List<Notificacion> resultado = notificacionController.misNotificaciones(httpRequest);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void confirmarConNotificacionInexistenteLanza404() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(notificacionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionController.confirmar(1L, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void confirmarConNotificacionDeOtroPacienteLanza403() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(999L);
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion()));

        assertThatThrownBy(() -> notificacionController.confirmar(1L, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void confirmarMarcaLaNotificacionComoConfirmada() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        Notificacion notificacion = notificacion();
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notificacion resultado = notificacionController.confirmar(1L, httpRequest);

        assertThat(resultado.getEstadoEnvio()).isEqualTo("CONFIRMADO");
        assertThat(resultado.getConfirmadoEn()).isNotNull();
    }
}
