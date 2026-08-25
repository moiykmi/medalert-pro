package cl.medalertpro.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * HU09: genera y valida códigos OTP de 6 dígitos, guardados en Redis con TTL
 * (expiran solos, no requieren limpieza manual). Cada código es de un solo uso:
 * se borra de Redis apenas se valida correctamente.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "otp:";

    private final StringRedisTemplate redisTemplate;

    @Value("${medalert.otp.ttl-minutos}")
    private int ttlMinutos;

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generar(String rut) {
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(KEY_PREFIX + rut, codigo, Duration.ofMinutes(ttlMinutos));
        return codigo;
    }

    public boolean validar(String rut, String codigoIngresado) {
        String key = KEY_PREFIX + rut;
        String codigoGuardado = redisTemplate.opsForValue().get(key);

        if (codigoGuardado == null) {
            return false; // no existe o expiró
        }
        if (!codigoGuardado.equals(codigoIngresado)) {
            return false;
        }

        redisTemplate.delete(key); // de un solo uso
        return true;
    }
}
