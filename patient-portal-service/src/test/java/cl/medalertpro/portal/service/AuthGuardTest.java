package cl.medalertpro.portal.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthGuardTest {

    @Mock
    private SesionService sesionService;

    @Mock
    private HttpServletRequest request;

    private AuthGuard authGuard;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authGuard = new AuthGuard(sesionService);
    }

    @Test
    void conTokenValidoRetornaElIdDelPaciente() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(sesionService.resolverPaciente("token-valido")).thenReturn(Optional.of(42L));

        Long pacienteId = authGuard.pacienteAutenticado(request);

        assertThat(pacienteId).isEqualTo(42L);
    }

    @Test
    void sinHeaderAuthorizationLanza401() {
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThatThrownBy(() -> authGuard.pacienteAutenticado(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void conHeaderSinPrefijoBearerLanza401() {
        when(request.getHeader("Authorization")).thenReturn("token-sin-prefijo");

        assertThatThrownBy(() -> authGuard.pacienteAutenticado(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void conTokenExpiradoOInexistenteLanza401() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-expirado");
        when(sesionService.resolverPaciente("token-expirado")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authGuard.pacienteAutenticado(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
