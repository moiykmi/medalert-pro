package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.dto.ActualizarDatosRequest;
import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.AuthGuard;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteControllerTest {

    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private AuthGuard authGuard;
    @Mock
    private HttpServletRequest httpRequest;

    private PacienteController pacienteController;

    @BeforeEach
    void setUp() {
        pacienteController = new PacienteController(pacienteRepository, authGuard);
    }

    private Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setRut("11111111-1");
        paciente.setNombre("Juan Perez");
        paciente.setTelefono("+56911111111");
        paciente.setEmail("juan@test.cl");
        paciente.setCanalPreferido("SMS");
        return paciente;
    }

    @Test
    void miPerfilConPacienteInexistenteLanza404() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteController.miPerfil(httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void miPerfilRetornaElPacienteAutenticado() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente()));

        Paciente resultado = pacienteController.miPerfil(httpRequest);

        assertThat(resultado.getRut()).isEqualTo("11111111-1");
    }

    @Test
    void actualizarDatosContactoActualizaSoloLosCamposEnviados() {
        Paciente existente = paciente();
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActualizarDatosRequest request = new ActualizarDatosRequest();
        request.setTelefono("+56999999999");

        Paciente resultado = pacienteController.actualizarDatosContacto(request, httpRequest);

        assertThat(resultado.getTelefono()).isEqualTo("+56999999999");
        assertThat(resultado.getEmail()).isEqualTo("juan@test.cl");
        assertThat(resultado.getCanalPreferido()).isEqualTo("SMS");
        assertThat(resultado.getDatosActualizadosEn()).isNotNull();
    }

    @Test
    void actualizarDatosContactoConPacienteInexistenteLanza404() {
        when(authGuard.pacienteAutenticado(httpRequest)).thenReturn(1L);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.empty());
        ActualizarDatosRequest request = new ActualizarDatosRequest();
        request.setTelefono("+56999999999");

        assertThatThrownBy(() -> pacienteController.actualizarDatosContacto(request, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
