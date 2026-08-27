package cl.medalertpro.fhirintegration.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthGuardTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AdminAuthGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AdminAuthGuard(redisTemplate);
        ReflectionTestUtils.setField(guard, "adminToken", "secreto-123");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void aceptaElTokenEstaticoComoSuperuser() {
        when(request.getHeader("X-Admin-Token")).thenReturn("secreto-123");

        assertThatCode(() -> guard.validar(request)).doesNotThrowAnyException();
        assertThat(guard.requerirSuperuser(request)).isEqualTo(AdminAuthGuard.ROL_SUPERUSER);
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
    void rechazaTokenQueNoEsElEstaticoNiUnaSesionValida() {
        when(request.getHeader("X-Admin-Token")).thenReturn("token-malo");
        when(valueOperations.get("admin-sesion:token-malo")).thenReturn(null);

        assertThatThrownBy(() -> guard.validar(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void aceptaUnTokenDeSesionValidoYExponeSuRol() {
        when(request.getHeader("X-Admin-Token")).thenReturn("token-sesion");
        when(valueOperations.get("admin-sesion:token-sesion")).thenReturn("5|PERSONAL_ADMINISTRATIVO");

        assertThat(guard.requerirRol(request, "PERSONAL_ADMINISTRATIVO")).isEqualTo("PERSONAL_ADMINISTRATIVO");
    }

    @Test
    void requerirRol_conRolNoPermitido_lanza403() {
        when(request.getHeader("X-Admin-Token")).thenReturn("token-sesion");
        when(valueOperations.get("admin-sesion:token-sesion")).thenReturn("5|AUDITORIA");

        assertThatThrownBy(() -> guard.requerirRol(request, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void requerirSuperuser_conSesionDeRolNormal_lanza403() {
        when(request.getHeader("X-Admin-Token")).thenReturn("token-sesion");
        when(valueOperations.get("admin-sesion:token-sesion")).thenReturn("5|ADMIN");

        assertThatThrownBy(() -> guard.requerirSuperuser(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
