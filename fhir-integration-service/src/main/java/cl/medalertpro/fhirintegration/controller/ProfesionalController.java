package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.CrearProfesionalRequest;
import cl.medalertpro.fhirintegration.dto.ProfesionalConCitasResponse;
import cl.medalertpro.fhirintegration.dto.SetCredencialesRequest;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import cl.medalertpro.fhirintegration.service.MedicoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Lista profesionales con su conteo de citas AGENDADA en una fecha (apoyo
 * para el formulario admin de registro de cancelación — ver
 * medalert_pro_mockup_v2.html, pantalla Agenda) y permite dar de alta
 * nuevos profesionales. No hay sincronización real con un RAS todavía.
 */
@RestController
@RequestMapping("/profesionales")
public class ProfesionalController {

    private final ProfesionalRepository profesionalRepository;
    private final CitaRepository citaRepository;
    private final AdminAuthGuard authGuard;
    private final MedicoAuthService medicoAuthService;

    public ProfesionalController(ProfesionalRepository profesionalRepository, CitaRepository citaRepository,
                                  AdminAuthGuard authGuard, MedicoAuthService medicoAuthService) {
        this.profesionalRepository = profesionalRepository;
        this.citaRepository = citaRepository;
        this.authGuard = authGuard;
        this.medicoAuthService = medicoAuthService;
    }

    @GetMapping
    public List<ProfesionalConCitasResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
            HttpServletRequest request) {
        authGuard.validar(request);

        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        LocalDateTime desde = horaInicio != null ? fechaConsulta.atTime(horaInicio) : fechaConsulta.atStartOfDay();
        LocalDateTime hasta = horaFin != null ? fechaConsulta.atTime(horaFin) : fechaConsulta.atTime(LocalTime.MAX);

        List<ProfesionalSalud> profesionales = profesionalRepository.findAll();
        return profesionales.stream()
                .map(p -> new ProfesionalConCitasResponse(
                        p.getId(),
                        p.getNombre(),
                        p.getEspecialidad(),
                        citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(
                                p.getId(), "AGENDADA", desde, hasta).size(),
                        p.getEmail()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfesionalSalud crear(@Valid @RequestBody CrearProfesionalRequest request, HttpServletRequest httpRequest) {
        authGuard.validar(httpRequest);

        ProfesionalSalud profesional = new ProfesionalSalud();
        profesional.setNombre(request.getNombre());
        profesional.setEspecialidad(request.getEspecialidad());
        profesional.setEstablecimientoId(request.getEstablecimientoId());

        return profesionalRepository.save(profesional);
    }

    /**
     * Asigna o cambia el acceso al portal médico de un profesional. Solo el
     * personal administrativo puede hacerlo (guardado con AdminAuthGuard) —
     * no existe autoregistro de médicos.
     */
    @PutMapping("/{id}/credenciales")
    public void asignarCredenciales(@PathVariable Long id, @Valid @RequestBody SetCredencialesRequest request,
                                     HttpServletRequest httpRequest) {
        authGuard.validar(httpRequest);

        ProfesionalSalud profesional = profesionalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesional no encontrado"));

        profesionalRepository.findByEmail(request.getEmail())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está en uso por otro profesional");
                });

        profesional.setEmail(request.getEmail());
        profesional.setPasswordHash(medicoAuthService.hashear(request.getPassword()));
        profesionalRepository.save(profesional);
    }
}
