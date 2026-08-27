package cl.medalertpro.fhirintegration.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

/**
 * Doble vía de acceso administrativo bajo el mismo header X-Admin-Token:
 * el secreto estático (acceso maestro, comportamiento sin cambios) o un
 * token de sesión emitido por AdminSesionService tras un login individual,
 * que sí trae un rol asociado para diferenciar permisos.
 */
@Component
public class AdminAuthGuard {

    public static final String ROL_SUPERUSER = "SUPERUSER";
    private static final String SESION_KEY_PREFIX = "admin-sesion:";

    @Value("${medalert.admin.token}")
    private String adminToken;

    private final StringRedisTemplate redisTemplate;

    public AdminAuthGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void validar(HttpServletRequest request) {
        resolverRol(request);
    }

    public String requerirRol(HttpServletRequest request, String... rolesPermitidos) {
        String rol = resolverRol(request);
        if (ROL_SUPERUSER.equals(rol) || Arrays.asList(rolesPermitidos).contains(rol)) {
            return rol;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para esta acción");
    }

    public String requerirSuperuser(HttpServletRequest request) {
        String rol = resolverRol(request);
        if (!ROL_SUPERUSER.equals(rol)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el acceso maestro puede realizar esta acción");
        }
        return rol;
    }

    private String resolverRol(HttpServletRequest request) {
        String token = request.getHeader("X-Admin-Token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el header X-Admin-Token");
        }
        if (adminToken.equals(token)) {
            return ROL_SUPERUSER;
        }
        String sesion = redisTemplate.opsForValue().get(SESION_KEY_PREFIX + token);
        if (sesion == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token administrativo inválido");
        }
        String[] partes = sesion.split("\\|", 2);
        return partes.length == 2 ? partes[1] : ROL_SUPERUSER;
    }
}
