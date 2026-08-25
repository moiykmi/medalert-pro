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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(redisTemplate);
        ReflectionTestUtils.setField(otpService, "ttlMinutos", 5);
    }

    @Test
    void generarProduceUnCodigoDeSeisDigitos() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String codigo = otpService.generar("11111111-1");

        assertThat(codigo).matches("\\d{6}");
    }

    @Test
    void generarGuardaElCodigoEnRedisConLaLlaveYTtlCorrectos() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String codigo = otpService.generar("11111111-1");

        verify(valueOperations).set(eq("otp:11111111-1"), eq(codigo), eq(Duration.ofMinutes(5)));
    }

    @Test
    void validarConCodigoCorrectoRetornaTrueYBorraLaLlave() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:11111111-1")).thenReturn("123456");

        boolean resultado = otpService.validar("11111111-1", "123456");

        assertThat(resultado).isTrue();
        verify(redisTemplate).delete("otp:11111111-1");
    }

    @Test
    void validarConCodigoIncorrectoRetornaFalseYNoBorraLaLlave() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:11111111-1")).thenReturn("123456");

        boolean resultado = otpService.validar("11111111-1", "999999");

        assertThat(resultado).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void validarConCodigoInexistenteOExpiradoRetornaFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:11111111-1")).thenReturn(null);

        boolean resultado = otpService.validar("11111111-1", "123456");

        assertThat(resultado).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }
}
