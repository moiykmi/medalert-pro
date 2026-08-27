package cl.medalertpro.fhirintegration.service;

import cl.medalertpro.fhirintegration.dto.AdminLoginRequest;
import cl.medalertpro.fhirintegration.dto.AdminSesionResponse;
import cl.medalertpro.fhirintegration.entity.UsuarioAdmin;
import cl.medalertpro.fhirintegration.repository.UsuarioAdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

/**
 * Login individual del personal administrativo (email + contraseña). No
 * reemplaza el X-Admin-Token compartido — es una capa adicional que sí
 * distingue quién entra y con qué rol (ver AdminAuthGuard.ROL_SUPERUSER vs.
 * los roles de usuario_admin). Mismo patrón de sesión con token opaco en
 * Redis que ya usan pacientes y médicos.
 */
@Service
public class AdminSesionService {

    static final String SESION_KEY_PREFIX = "admin-sesion:";

    private final UsuarioAdminRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${medalert.admin.sesion-ttl-minutos:480}")
    private int ttlMinutos;

    public AdminSesionService(UsuarioAdminRepository repository, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public AdminSesionResponse login(AdminLoginRequest request) {
        UsuarioAdmin usuario = repository.findByEmail(request.getEmail())
                .filter(UsuarioAdmin::isActivo)
                .filter(u -> u.getPasswordHash() != null)
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(SESION_KEY_PREFIX + token, usuario.getId() + "|" + usuario.getRol(), Duration.ofMinutes(ttlMinutos));

        return new AdminSesionResponse(token, usuario.getNombre(), usuario.getRol());
    }

    public String hashear(String passwordEnClaro) {
        return passwordEncoder.encode(passwordEnClaro);
    }
}
