package cl.medalertpro.fhirintegration.service;

import cl.medalertpro.fhirintegration.dto.MedicoLoginRequest;
import cl.medalertpro.fhirintegration.dto.MedicoSesionResponse;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicoAuthServiceTest {

    private static final int TTL_MINUTOS = 240;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MedicoAuthService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        service = new MedicoAuthService(profesionalRepository, redisTemplate);
        ReflectionTestUtils.setField(service, "ttlMinutos", TTL_MINUTOS);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private ProfesionalSalud profesionalConCredenciales(Long id, String email, String passwordEnClaro) {
        ProfesionalSalud p = new ProfesionalSalud();
        ReflectionTestUtils.setField(p, "id", id);
        p.setNombre("Dr. Martín Fuentes");
        p.setEspecialidad("Medicina General");
        p.setEmail(email);
        p.setPasswordHash(passwordEnClaro == null ? null : encoder.encode(passwordEnClaro));
        return p;
    }

    @Test
    void login_conCredencialesCorrectas_emiteSesionYLaGuardaEnRedis() {
        ProfesionalSalud profesional = profesionalConCredenciales(7L, "mfuentes@consultorio.cl", "clave-segura-123");
        when(profesionalRepository.findByEmail("mfuentes@consultorio.cl")).thenReturn(Optional.of(profesional));

        MedicoLoginRequest request = new MedicoLoginRequest();
        request.setEmail("mfuentes@consultorio.cl");
        request.setPassword("clave-segura-123");

        MedicoSesionResponse sesion = service.login(request);

        assertThat(sesion.getProfesionalId()).isEqualTo(7L);
        assertThat(sesion.getNombre()).isEqualTo("Dr. Martín Fuentes");
        assertThat(sesion.getToken()).isNotBlank();

        verify(valueOperations).set(eq("medico-sesion:" + sesion.getToken()), eq("7"), eq(Duration.ofMinutes(TTL_MINUTOS)));
    }

    @Test
    void login_conPasswordIncorrecta_lanza401() {
        ProfesionalSalud profesional = profesionalConCredenciales(7L, "mfuentes@consultorio.cl", "clave-segura-123");
        when(profesionalRepository.findByEmail("mfuentes@consultorio.cl")).thenReturn(Optional.of(profesional));

        MedicoLoginRequest request = new MedicoLoginRequest();
        request.setEmail("mfuentes@consultorio.cl");
        request.setPassword("clave-incorrecta");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_conEmailNoRegistrado_lanza401() {
        when(profesionalRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        MedicoLoginRequest request = new MedicoLoginRequest();
        request.setEmail("nadie@consultorio.cl");
        request.setPassword("cualquiera123");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_conProfesionalSinCredencialesConfiguradas_lanza401() {
        ProfesionalSalud profesionalSinPassword = profesionalConCredenciales(9L, "sinpass@consultorio.cl", null);
        when(profesionalRepository.findByEmail("sinpass@consultorio.cl")).thenReturn(Optional.of(profesionalSinPassword));

        MedicoLoginRequest request = new MedicoLoginRequest();
        request.setEmail("sinpass@consultorio.cl");
        request.setPassword("cualquiera123");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resolverProfesional_conTokenValidoEnRedis_devuelveElId() {
        when(valueOperations.get("medico-sesion:token-valido")).thenReturn("42");

        Optional<Long> resultado = service.resolverProfesional("token-valido");

        assertThat(resultado).contains(42L);
    }

    @Test
    void resolverProfesional_conTokenAusenteEnRedis_devuelveVacio() {
        when(valueOperations.get("medico-sesion:token-expirado")).thenReturn(null);

        assertThat(service.resolverProfesional("token-expirado")).isEmpty();
    }

    @Test
    void hashear_generaUnHashQueElPropioEncoderPuedeVerificar() {
        String hash = service.hashear("clave-de-prueba");

        assertThat(hash).isNotEqualTo("clave-de-prueba");
        assertThat(encoder.matches("clave-de-prueba", hash)).isTrue();
    }
}
