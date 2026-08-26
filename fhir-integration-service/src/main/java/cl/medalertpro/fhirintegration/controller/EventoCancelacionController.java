package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.RegistrarCancelacionRequest;
import cl.medalertpro.fhirintegration.entity.EventoCancelacion;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import cl.medalertpro.fhirintegration.service.EventoCancelacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint interno que usa el personal administrativo para registrar una ausencia
 * médica, dado que RAS no expone una API pública que dispare esto automáticamente.
 *
 * Prueba manual (Postman):
 * POST http://localhost:8081/eventos/cancelacion
 * {
 *   "profesionalId": 1,
 *   "fecha": "2026-08-01",
 *   "motivo": "Licencia médica",
 *   "registradoPor": 1
 * }
 */
@RestController
@RequestMapping("/eventos")
public class EventoCancelacionController {

    private final EventoCancelacionService service;
    private final AdminAuthGuard authGuard;

    public EventoCancelacionController(EventoCancelacionService service, AdminAuthGuard authGuard) {
        this.service = service;
        this.authGuard = authGuard;
    }

    @PostMapping("/cancelacion")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoCancelacion registrarCancelacion(@Valid @RequestBody RegistrarCancelacionRequest request,
                                                   HttpServletRequest httpRequest) {
        authGuard.validar(httpRequest);
        return service.registrarYPublicar(request);
    }
}
