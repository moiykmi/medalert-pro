package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.CrearCitaRequest;
import cl.medalertpro.fhirintegration.entity.Cita;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.NotificacionRepository;
import cl.medalertpro.fhirintegration.repository.PacienteRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.repository.ReagendamientoRepository;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import cl.medalertpro.fhirintegration.service.ReglaHorarioCita;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creación manual de citas AGENDADA desde el admin — útil para cargar datos
 * de prueba (no hay sincronización real con un RAS todavía) y así poder
 * ejercitar el flujo de cancelación con pacientes concretos. Sujeta a las
 * mismas reglas de horario que el reagendamiento del paciente (ver
 * ReglaHorarioCita): bloques de 30 min, 08:00-18:00, sin colación 13-14h,
 * sin solaparse con otra cita AGENDADA del mismo profesional.
 */
@RestController
@RequestMapping("/admin/citas")
public class AdminCitaController {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final ReagendamientoRepository reagendamientoRepository;
    private final NotificacionRepository notificacionRepository;
    private final AdminAuthGuard authGuard;

    public AdminCitaController(CitaRepository citaRepository, PacienteRepository pacienteRepository,
                                ProfesionalRepository profesionalRepository, ReagendamientoRepository reagendamientoRepository,
                                NotificacionRepository notificacionRepository, AdminAuthGuard authGuard) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.reagendamientoRepository = reagendamientoRepository;
        this.notificacionRepository = notificacionRepository;
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
        ReglaHorarioCita.validar(request.getFechaHora());
        boolean solapada = !citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(
                request.getProfesionalId(), "AGENDADA",
                ReglaHorarioCita.inicioVentanaSolapamiento(request.getFechaHora()),
                ReglaHorarioCita.finVentanaSolapamiento(request.getFechaHora())).isEmpty();
        if (solapada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El profesional ya tiene una cita agendada en ese horario — elige otro horario");
        }

        Cita cita = new Cita();
        cita.setPacienteId(request.getPacienteId());
        cita.setProfesionalId(request.getProfesionalId());
        cita.setFechaHora(request.getFechaHora());
        cita.setEstado("AGENDADA");

        return citaRepository.save(cita);
    }

    /** Limpieza de citas de prueba mal cargadas — borrado físico, no un cambio de estado. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void eliminar(@PathVariable Long id, HttpServletRequest httpRequest) {
        authGuard.validar(httpRequest);

        if (!citaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada");
        }
        // reagendamiento y notificacion referencian cita sin ON DELETE CASCADE
        // — hay que soltar ambas referencias antes o la eliminación de la
        // cita viola la llave foránea.
        reagendamientoRepository.deleteByCitaOriginalIdOrCitaNuevaId(id, id);
        notificacionRepository.deleteByCitaId(id);
        citaRepository.deleteById(id);
    }
}
