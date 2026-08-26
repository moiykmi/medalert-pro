package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.ProfesionalConCitasResponse;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.ProfesionalRepository;
import cl.medalertpro.fhirintegration.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Endpoint de apoyo para el formulario admin de registro de cancelación:
 * lista los profesionales con su conteo de citas AGENDADA en una fecha,
 * para mostrar el selector y el aviso de "se notificará a N pacientes"
 * antes de confirmar (ver medalert_pro_mockup_v2.html, pantalla Agenda).
 */
@RestController
@RequestMapping("/profesionales")
public class ProfesionalController {

    private final ProfesionalRepository profesionalRepository;
    private final CitaRepository citaRepository;
    private final AdminAuthGuard authGuard;

    public ProfesionalController(ProfesionalRepository profesionalRepository, CitaRepository citaRepository,
                                  AdminAuthGuard authGuard) {
        this.profesionalRepository = profesionalRepository;
        this.citaRepository = citaRepository;
        this.authGuard = authGuard;
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
                                p.getId(), "AGENDADA", desde, hasta).size()))
                .toList();
    }
}
