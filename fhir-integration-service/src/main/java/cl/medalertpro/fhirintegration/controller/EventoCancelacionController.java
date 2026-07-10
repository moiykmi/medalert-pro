package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.RegistrarCancelacionRequest;
import cl.medalertpro.fhirintegration.entity.EventoCancelacion;
import cl.medalertpro.fhirintegration.service.EventoCancelacionService;
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

    public EventoCancelacionController(EventoCancelacionService service) {
        this.service = service;
    }

    @PostMapping("/cancelacion")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoCancelacion registrarCancelacion(@Valid @RequestBody RegistrarCancelacionRequest request) {
        return service.registrarYPublicar(request);
    }
}
