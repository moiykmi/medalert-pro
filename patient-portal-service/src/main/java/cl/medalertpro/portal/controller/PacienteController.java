package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.dto.ActualizarDatosRequest;
import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * HU13: autoactualización de datos de contacto — ataca la causa raíz de
 * datos de contacto desactualizados (15-20% de los registros, según el
 * diagnóstico de la tesis).
 */
@RestController
@RequestMapping("/paciente")
public class PacienteController {

    private final PacienteRepository pacienteRepository;
    private final AuthGuard authGuard;

    public PacienteController(PacienteRepository pacienteRepository, AuthGuard authGuard) {
        this.pacienteRepository = pacienteRepository;
        this.authGuard = authGuard;
    }

    @GetMapping("/perfil")
    public Paciente miPerfil(HttpServletRequest request) {
        Long pacienteId = authGuard.pacienteAutenticado(request);
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no existe"));
    }

    @PutMapping("/datos-contacto")
    public Paciente actualizarDatosContacto(@Valid @RequestBody ActualizarDatosRequest request,
                                             HttpServletRequest httpRequest) {
        Long pacienteId = authGuard.pacienteAutenticado(httpRequest);

        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no existe"));

        // Solo actualiza los campos que vinieron en la petición — permite
        // actualizar solo el teléfono sin tener que reenviar el email, etc.
        if (request.getTelefono() != null) paciente.setTelefono(request.getTelefono());
        if (request.getEmail() != null) paciente.setEmail(request.getEmail());
        if (request.getCanalPreferido() != null) paciente.setCanalPreferido(request.getCanalPreferido());
        paciente.setDatosActualizadosEn(LocalDateTime.now());

        return pacienteRepository.save(paciente);
    }
}
