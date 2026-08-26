package cl.medalertpro.fhirintegration.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminAuthGuard {

    @Value("${medalert.admin.token}")
    private String adminToken;

    public void validar(HttpServletRequest request) {
        String token = request.getHeader("X-Admin-Token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el header X-Admin-Token");
        }
        if (!adminToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token administrativo inválido");
        }
    }
}
