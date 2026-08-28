package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.CrearCitaRequest;
import cl.medalertpro.fhirintegration.entity.Cita;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.PacienteRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creación manual de citas AGENDADA desde el admin — útil para cargar datos
 * de prueba (no hay sincronización real con un RAS todavía) y así poder
 * ejercitar el flujo de cancelación con pacientes concretos.
 */
@RestController
@RequestMapping("/admin/citas")
public class AdminCitaController {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final AdminAuthGuard authGuard;

    public AdminCitaController(CitaRepository citaRepository, PacienteRepository pacienteRepository,
                                ProfesionalRepository profesionalRepository, AdminAuthGuard authGuard) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.authGuard = authGuard;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cita crear(@Valid @RequestBody CrearCitaRequest request, HttpServletRequest httpRequest) {
        authGuard.validar(httpRequest);

        if (!pacienteRepository.existsById(request.getPacienteId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado");
        }
        if (!profesionalRepository.existsById(request.getProfesionalId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesional no encontrado");
        }

        Cita cita = new Cita();
        cita.setPacienteId(request.getPacienteId());
        cita.setProfesionalId(request.getProfesionalId());
        cita.setFechaHora(request.getFechaHora());
        cita.setEstado("AGENDADA");

        return citaRepository.save(cita);
    }
}
