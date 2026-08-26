package cl.medalertpro.fhirintegration.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicoAuthGuardTest {

    @Mock
    private MedicoAuthService medicoAuthService;

    @Mock
    private HttpServletRequest request;

    @Test
    void medicoAutenticado_sinHeaderAuthorization_lanza401() {
        when(request.getHeader("Authorization")).thenReturn(null);
        MedicoAuthGuard guard = new MedicoAuthGuard(medicoAuthService);

        assertThatThrownBy(() -> guard.medicoAutenticado(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void medicoAutenticado_conHeaderMalFormado_lanza401() {
        when(request.getHeader("Authorization")).thenReturn("Token abc123");
        MedicoAuthGuard guard = new MedicoAuthGuard(medicoAuthService);

        assertThatThrownBy(() -> guard.medicoAutenticado(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void medicoAutenticado_conTokenValido_devuelveElProfesionalId() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(medicoAuthService.resolverProfesional("token-valido")).thenReturn(Optional.of(7L));
        MedicoAuthGuard guard = new MedicoAuthGuard(medicoAuthService);

        assertThat(guard.medicoAutenticado(request)).isEqualTo(7L);
    }

    @Test
    void medicoAutenticado_conTokenInvalidoOExpirado_lanza401() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-vencido");
        when(medicoAuthService.resolverProfesional("token-vencido")).thenReturn(Optional.empty());
        MedicoAuthGuard guard = new MedicoAuthGuard(medicoAuthService);

        assertThatThrownBy(() -> guard.medicoAutenticado(request)).isInstanceOf(ResponseStatusException.class);
    }
}
