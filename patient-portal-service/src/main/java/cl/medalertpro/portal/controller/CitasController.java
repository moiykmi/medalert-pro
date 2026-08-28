package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.dto.ReagendarRequest;
import cl.medalertpro.portal.entity.Cita;
import cl.medalertpro.portal.entity.Reagendamiento;
import cl.medalertpro.portal.repository.CitaRepository;
import cl.medalertpro.portal.repository.ReagendamientoRepository;
import cl.medalertpro.portal.service.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * HU10: consulta de citas del paciente autenticado.
 * HU12: reagendamiento autónomo — dado que RAS no expone horarios disponibles
 * reales, el paciente propone una nueva fecha/hora libremente (no elige de una
 * lista de cupos). Sí se valida que ese horario puntual no choque con otra
 * cita AGENDADA del mismo profesional — evita la doble reserva del mismo
 * cupo, aunque no existe un calendario de disponibilidad real que sugiera
 * horarios libres de antemano. Limitación conocida del MVP, coherente con el
 * resto del proyecto (RAS simulado, no integrado en producción).
 */
@RestController
@RequestMapping("/citas")
public class CitasController {

    private final CitaRepository citaRepository;
    private final ReagendamientoRepository reagendamientoRepository;
    private final AuthGuard authGuard;

    public CitasController(CitaRepository citaRepository, ReagendamientoRepository reagendamientoRepository,
                            AuthGuard authGuard) {
        this.citaRepository = citaRepository;
        this.reagendamientoRepository = reagendamientoRepository;
        this.authGuard = authGuard;
    }

    @GetMapping
    public List<Cita> misCitas(HttpServletRequest request) {
        Long pacienteId = authGuard.pacienteAutenticado(request);
        return citaRepository.findByPacienteIdOrderByFechaHoraDesc(pacienteId);
    }

    @PostMapping("/{id}/reagendar")
    @Transactional
    public Cita reagendar(@PathVariable Long id, @Valid @RequestBody ReagendarRequest request,
                           HttpServletRequest httpRequest) {
        Long pacienteId = authGuard.pacienteAutenticado(httpRequest);

        Cita citaOriginal = citaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no existe"));

        if (!citaOriginal.getPacienteId().equals(pacienteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta cita no pertenece al paciente autenticado");
        }
        if (!"CANCELADA".equals(citaOriginal.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden reagendar citas en estado CANCELADA (estado actual: " + citaOriginal.getEstado() + ")");
        }
        if (citaRepository.existsByProfesionalIdAndFechaHoraAndEstado(
                citaOriginal.getProfesionalId(), request.getNuevaFechaHora(), "AGENDADA")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El profesional ya tiene una cita agendada en ese horario — elige otro horario");
        }

        Cita citaNueva = new Cita();
        citaNueva.setPacienteId(citaOriginal.getPacienteId());
        citaNueva.setProfesionalId(citaOriginal.getProfesionalId());
        citaNueva.setFechaHora(request.getNuevaFechaHora());
        citaNueva.setEstado("AGENDADA");
        citaNueva = citaRepository.save(citaNueva);

        citaOriginal.setEstado("REAGENDADA");
        citaRepository.save(citaOriginal);

        Reagendamiento reagendamiento = new Reagendamiento();
        reagendamiento.setCitaOriginalId(citaOriginal.getId());
        reagendamiento.setCitaNuevaId(citaNueva.getId());
        reagendamiento.setPacienteId(pacienteId);
        reagendamiento.setEstado("CONFIRMADO");
        reagendamientoRepository.save(reagendamiento);

        return citaNueva;
    }
}
