package cl.medalertpro.fhirintegration.service;

import cl.medalertpro.fhirintegration.dto.AdminLoginRequest;
import cl.medalertpro.fhirintegration.dto.AdminSesionResponse;
import cl.medalertpro.fhirintegration.entity.UsuarioAdmin;
import cl.medalertpro.fhirintegration.repository.UsuarioAdminRepository;
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
class AdminSesionServiceTest {

    private static final int TTL_MINUTOS = 480;

    @Mock
    private UsuarioAdminRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AdminSesionService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        service = new AdminSesionService(repository, redisTemplate);
        ReflectionTestUtils.setField(service, "ttlMinutos", TTL_MINUTOS);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private UsuarioAdmin usuarioConCredenciales(Long id, String email, String rol, boolean activo, String passwordEnClaro) {
        UsuarioAdmin u = new UsuarioAdmin();
        ReflectionTestUtils.setField(u, "id", id);
        u.setNombre("Carla Rivas");
        u.setEmail(email);
        u.setRol(rol);
        u.setEstablecimientoId(1L);
        u.setActivo(activo);
        u.setPasswordHash(passwordEnClaro == null ? null : encoder.encode(passwordEnClaro));
        return u;
    }

    @Test
    void login_conCredencialesCorrectas_emiteSesionConRolYLaGuardaEnRedis() {
        UsuarioAdmin usuario = usuarioConCredenciales(3L, "crivas@clinica.cl", "PERSONAL_ADMINISTRATIVO", true, "clave-segura-123");
        when(repository.findByEmail("crivas@clinica.cl")).thenReturn(Optional.of(usuario));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("crivas@clinica.cl");
        request.setPassword("clave-segura-123");

        AdminSesionResponse sesion = service.login(request);

        assertThat(sesion.getNombre()).isEqualTo("Carla Rivas");
        assertThat(sesion.getRol()).isEqualTo("PERSONAL_ADMINISTRATIVO");
        assertThat(sesion.getToken()).isNotBlank();

        verify(valueOperations).set(eq("admin-sesion:" + sesion.getToken()), eq("3|PERSONAL_ADMINISTRATIVO"), eq(Duration.ofMinutes(TTL_MINUTOS)));
    }

    @Test
    void login_conPasswordIncorrecta_lanza401() {
        UsuarioAdmin usuario = usuarioConCredenciales(3L, "crivas@clinica.cl", "ADMIN", true, "clave-segura-123");
        when(repository.findByEmail("crivas@clinica.cl")).thenReturn(Optional.of(usuario));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("crivas@clinica.cl");
        request.setPassword("clave-incorrecta");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_conEmailNoRegistrado_lanza401() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());

        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("nadie@clinica.cl");
        request.setPassword("cualquiera123");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_conUsuarioSinCredencialesConfiguradas_lanza401() {
        UsuarioAdmin usuario = usuarioConCredenciales(4L, "sinpass@clinica.cl", "AUDITORIA", true, null);
        when(repository.findByEmail("sinpass@clinica.cl")).thenReturn(Optional.of(usuario));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("sinpass@clinica.cl");
        request.setPassword("cualquiera123");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_conUsuarioInactivo_lanza401() {
        UsuarioAdmin usuario = usuarioConCredenciales(5L, "inactivo@clinica.cl", "ADMIN", false, "clave-segura-123");
        when(repository.findByEmail("inactivo@clinica.cl")).thenReturn(Optional.of(usuario));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("inactivo@clinica.cl");
        request.setPassword("clave-segura-123");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void hashear_generaUnHashQueElPropioEncoderPuedeVerificar() {
        String hash = service.hashear("clave-de-prueba");

        assertThat(hash).isNotEqualTo("clave-de-prueba");
        assertThat(encoder.matches("clave-de-prueba", hash)).isTrue();
    }
}
