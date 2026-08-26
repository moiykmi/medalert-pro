package cl.medalertpro.fhirintegration.service;

import cl.medalertpro.fhirintegration.dto.MedicoLoginRequest;
import cl.medalertpro.fhirintegration.dto.MedicoSesionResponse;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Login del portal médico (email + contraseña). Sesión con token opaco en
 * Redis, igual patrón que patient-portal-service.SesionService — no es JWT,
 * simplicidad deliberada para el alcance del proyecto.
 */
@Service
public class MedicoAuthService {

    private static final String SESION_KEY_PREFIX = "medico-sesion:";

    private final ProfesionalRepository profesionalRepository;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${medalert.medico.sesion-ttl-minutos}")
    private int ttlMinutos;

    public MedicoAuthService(ProfesionalRepository profesionalRepository, StringRedisTemplate redisTemplate) {
        this.profesionalRepository = profesionalRepository;
        this.redisTemplate = redisTemplate;
    }

    public MedicoSesionResponse login(MedicoLoginRequest request) {
        ProfesionalSalud profesional = profesionalRepository.findByEmail(request.getEmail())
                .filter(p -> p.getPasswordHash() != null)
                .filter(p -> passwordEncoder.matches(request.getPassword(), p.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(SESION_KEY_PREFIX + token, String.valueOf(profesional.getId()), Duration.ofMinutes(ttlMinutos));

        return new MedicoSesionResponse(token, profesional.getId(), profesional.getNombre(), profesional.getEspecialidad());
    }

    public Optional<Long> resolverProfesional(String token) {
        String valor = redisTemplate.opsForValue().get(SESION_KEY_PREFIX + token);
        return Optional.ofNullable(valor).map(Long::parseLong);
    }

    public String hashear(String passwordEnClaro) {
        return passwordEncoder.encode(passwordEnClaro);
    }
}
