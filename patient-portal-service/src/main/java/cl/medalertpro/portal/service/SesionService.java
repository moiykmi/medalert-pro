package cl.medalertpro.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Sesión simple para el portal: tras validar el OTP, se emite un token opaco
 * (no JWT, por simplicidad para el MPV) que el frontend debe enviar en el
 * header "Authorization: Bearer <token>" en las siguientes peticiones.
 */
@Service
public class SesionService {

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;

    @Value("${medalert.sesion.ttl-minutos}")
    private int ttlMinutos;

    public SesionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String crearSesion(Long pacienteId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, String.valueOf(pacienteId), Duration.ofMinutes(ttlMinutos));
        return token;
    }

    public Optional<Long> resolverPaciente(String token) {
        String valor = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        return Optional.ofNullable(valor).map(Long::parseLong);
    }
}
