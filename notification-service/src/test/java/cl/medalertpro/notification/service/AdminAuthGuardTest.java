package cl.medalertpro.notification.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthGuardTest {

    @Mock
    private HttpServletRequest request;

    private AdminAuthGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AdminAuthGuard();
        ReflectionTestUtils.setField(guard, "adminToken", "secreto-123");
    }

    @Test
    void aceptaElTokenCorrecto() {
        when(request.getHeader("X-Admin-Token")).thenReturn("secreto-123");

        assertThatCode(() -> guard.validar(request)).doesNotThrowAnyException();
    }

    @Test
    void rechazaCuandoFaltaElHeader() {
        when(request.getHeader("X-Admin-Token")).thenReturn(null);

        assertThatThrownBy(() -> guard.validar(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rechazaCuandoElHeaderEstaEnBlanco() {
        when(request.getHeader("X-Admin-Token")).thenReturn("   ");

        assertThatThrownBy(() -> guard.validar(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rechazaTokenIncorrecto() {
        when(request.getHeader("X-Admin-Token")).thenReturn("token-malo");

        assertThatThrownBy(() -> guard.validar(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
