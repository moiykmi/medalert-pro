package cl.medalertpro.fhirintegration.controller;

import cl.medalertpro.fhirintegration.dto.CitaAgendaResponse;
import cl.medalertpro.fhirintegration.entity.Cita;
import cl.medalertpro.fhirintegration.entity.Paciente;
import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.PacienteRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vista de calendario del día completo (medalert_pro_mockup_v2.html, pantalla
 * Agenda): todas las citas de todos los profesionales de una fecha, con
 * nombre de paciente y profesional resueltos — distinto de /profesionales,
 * que solo entrega un conteo por profesional para el formulario de cancelación.
 */
@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final CitaRepository citaRepository;
    private final ProfesionalRepository profesionalRepository;
    private final PacienteRepository pacienteRepository;
    private final AdminAuthGuard authGuard;

    public AgendaController(CitaRepository citaRepository, ProfesionalRepository profesionalRepository,
                             PacienteRepository pacienteRepository, AdminAuthGuard authGuard) {
        this.citaRepository = citaRepository;
        this.profesionalRepository = profesionalRepository;
        this.pacienteRepository = pacienteRepository;
        this.authGuard = authGuard;
    }

    @GetMapping
    public List<CitaAgendaResponse> agendaDelDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpServletRequest request) {
        authGuard.validar(request);

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(LocalTime.MAX);
        List<Cita> citas = citaRepository.findByFechaHoraBetweenOrderByFechaHoraAsc(desde, hasta);

        Map<Long, ProfesionalSalud> profesionales = profesionalRepository
                .findAllById(citas.stream().map(Cita::getProfesionalId).distinct().toList())
                .stream().collect(Collectors.toMap(ProfesionalSalud::getId, Function.identity()));

        Map<Long, Paciente> pacientes = pacienteRepository
                .findAllById(citas.stream().map(Cita::getPacienteId).distinct().toList())
                .stream().collect(Collectors.toMap(Paciente::getId, Function.identity()));

        return citas.stream().map(c -> {
            ProfesionalSalud prof = profesionales.get(c.getProfesionalId());
            Paciente pac = pacientes.get(c.getPacienteId());
            return new CitaAgendaResponse(
                    c.getId(), c.getFechaHora(), c.getEstado(),
                    c.getPacienteId(), pac != null ? pac.getNombre() : "Paciente no encontrado",
                    c.getProfesionalId(), prof != null ? prof.getNombre() : "Profesional no encontrado",
                    prof != null ? prof.getEspecialidad() : null);
        }).toList();
    }
}
