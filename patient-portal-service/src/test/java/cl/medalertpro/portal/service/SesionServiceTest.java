package cl.medalertpro.portal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SesionService sesionService;

    @BeforeEach
    void setUp() {
        sesionService = new SesionService(redisTemplate);
        ReflectionTestUtils.setField(sesionService, "ttlMinutos", 30);
    }

    @Test
    void crearSesionGeneraUnTokenYLoAlmacenaEnRedisConElTtlConfigurado() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = sesionService.crearSesion(42L);

        assertThat(token).isNotBlank();
        verify(valueOperations).set(eq("session:" + token), eq("42"), eq(Duration.ofMinutes(30)));
    }

    @Test
    void resolverPacienteConTokenValidoRetornaElId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("session:abc-123")).thenReturn("42");

        Optional<Long> pacienteId = sesionService.resolverPaciente("abc-123");

        assertThat(pacienteId).contains(42L);
    }

    @Test
    void resolverPacienteConTokenInexistenteRetornaVacio() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("session:no-existe")).thenReturn(null);

        Optional<Long> pacienteId = sesionService.resolverPaciente("no-existe");

        assertThat(pacienteId).isEmpty();
    }
}
