package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.MedicoAgendaHoyResponse;
import cl.medalertpro.fhirintegration.dto.MedicoLoginRequest;
import cl.medalertpro.fhirintegration.dto.MedicoSesionResponse;
import cl.medalertpro.fhirintegration.dto.RegistrarCancelacionRequest;
import cl.medalertpro.fhirintegration.dto.ReportarAusenciaRequest;
import cl.medalertpro.fhirintegration.entity.EventoCancelacion;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.service.EventoCancelacionService;
import cl.medalertpro.fhirintegration.service.MedicoAuthGuard;
import cl.medalertpro.fhirintegration.service.MedicoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Portal del profesional de salud: login propio (email+contraseña) y reporte
 * de su propia ausencia — misma mecánica que el registro administrativo
 * (EventoCancelacionService), pero el profesionalId siempre viene de la
 * sesión autenticada, nunca del cuerpo de la petición, para que un médico
 * no pueda reportar una ausencia a nombre de otro.
 */
@RestController
@RequestMapping("/medico")
public class MedicoController {

    private final MedicoAuthService medicoAuthService;
    private final MedicoAuthGuard medicoAuthGuard;
    private final ProfesionalRepository profesionalRepository;
    private final CitaRepository citaRepository;
    private final EventoCancelacionService eventoCancelacionService;

    public MedicoController(MedicoAuthService medicoAuthService, MedicoAuthGuard medicoAuthGuard,
                             ProfesionalRepository profesionalRepository, CitaRepository citaRepository,
                             EventoCancelacionService eventoCancelacionService) {
        this.medicoAuthService = medicoAuthService;
        this.medicoAuthGuard = medicoAuthGuard;
        this.profesionalRepository = profesionalRepository;
        this.citaRepository = citaRepository;
        this.eventoCancelacionService = eventoCancelacionService;
    }

    @PostMapping("/auth/login")
    public MedicoSesionResponse login(@Valid @RequestBody MedicoLoginRequest request) {
        return medicoAuthService.login(request);
    }

    @GetMapping("/agenda-hoy")
    public MedicoAgendaHoyResponse agendaHoy(HttpServletRequest httpRequest) {
        Long profesionalId = medicoAuthGuard.medicoAutenticado(httpRequest);
        ProfesionalSalud profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesional no encontrado"));

        LocalDate hoy = LocalDate.now();
        int citasHoy = citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(
                profesionalId, "AGENDADA", hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX)).size();

        return new MedicoAgendaHoyResponse(profesional.getNombre(), profesional.getEspecialidad(), citasHoy);
    }

    @PostMapping("/ausencia")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoCancelacion reportarAusencia(@Valid @RequestBody ReportarAusenciaRequest request,
                                               HttpServletRequest httpRequest) {
        Long profesionalId = medicoAuthGuard.medicoAutenticado(httpRequest);

        RegistrarCancelacionRequest cancelacion = new RegistrarCancelacionRequest();
        cancelacion.setProfesionalId(profesionalId);
        cancelacion.setFecha(request.getFecha());
        cancelacion.setHoraInicio(request.getHoraInicio());
        cancelacion.setHoraFin(request.getHoraFin());
        cancelacion.setMotivo(request.getMotivo());
        // registradoPor queda null: lo reportó el propio médico, no un usuario_admin.

        return eventoCancelacionService.registrarYPublicar(cancelacion);
    }
}
