package cl.medalertpro.portal.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class AuthGuard {

    private final SesionService sesionService;

    public AuthGuard(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    /**
     * Extrae y valida el token del header "Authorization: Bearer <token>".
     * Lanza 401 si no hay token o si expiró/no existe en Redis.
     */
    public Long pacienteAutenticado(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el header Authorization: Bearer <token>");
        }
        String token = header.substring("Bearer ".length());
        return sesionService.resolverPaciente(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida o expirada"));
    }
}
