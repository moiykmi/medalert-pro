package cl.medalertpro.fhirintegration.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MedicoAuthGuard {

    private final MedicoAuthService medicoAuthService;

    public MedicoAuthGuard(MedicoAuthService medicoAuthService) {
        this.medicoAuthService = medicoAuthService;
    }

    /**
     * Extrae y valida el token del header "Authorization: Bearer <token>" y
     * devuelve el id del profesional autenticado. Lanza 401 si falta o expiró.
     */
    public Long medicoAutenticado(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el header Authorization: Bearer <token>");
        }
        String token = header.substring("Bearer ".length());
        return medicoAuthService.resolverProfesional(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida o expirada"));
    }
}
