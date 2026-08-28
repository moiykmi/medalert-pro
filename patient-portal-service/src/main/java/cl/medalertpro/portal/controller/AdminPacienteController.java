package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.dto.ActualizarDatosRequest;
import cl.medalertpro.portal.entity.Notificacion;
import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.NotificacionRepository;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Listado de pacientes para el panel administrativo
 * (medalert_pro_mockup_v2.html, pantalla Pacientes). La búsqueda se hace
 * en el frontend sobre este listado — el volumen de pacientes de un
 * consultorio no justifica paginación server-side en el MVP.
 */
@RestController
@RequestMapping("/admin/pacientes")
public class AdminPacienteController {

    private final PacienteRepository pacienteRepository;
    private final NotificacionRepository notificacionRepository;
    private final AdminAuthGuard authGuard;

    public AdminPacienteController(PacienteRepository pacienteRepository, NotificacionRepository notificacionRepository,
                                    AdminAuthGuard authGuard) {
        this.pacienteRepository = pacienteRepository;
        this.notificacionRepository = notificacionRepository;
        this.authGuard = authGuard;
    }

    @GetMapping
    public List<Paciente> listar(HttpServletRequest request) {
        authGuard.validar(request);
        return pacienteRepository.findAll();
    }

    @GetMapping("/{id}/notificaciones")
    public List<Notificacion> historialNotificaciones(@PathVariable Long id, HttpServletRequest request) {
        authGuard.validar(request);

        if (!pacienteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado");
        }

        return notificacionRepository.findByPacienteIdOrderByEnviadoEnDesc(id);
    }

    /**
     * El personal administrativo corrige los datos de contacto de un paciente
     * (mismo cuerpo parcial que el autoservicio del paciente en /paciente/datos-contacto,
     * pero guardado con AdminAuthGuard en vez de la sesión del propio paciente).
     */
    @PutMapping("/{id}")
    public Paciente actualizarDatos(@PathVariable Long id, @Valid @RequestBody ActualizarDatosRequest request,
                                     HttpServletRequest httpRequest) {
        authGuard.validar(httpRequest);

        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));

        if (request.getTelefono() != null) paciente.setTelefono(request.getTelefono());
        if (request.getEmail() != null) paciente.setEmail(request.getEmail());
        if (request.getCanalPreferido() != null) paciente.setCanalPreferido(request.getCanalPreferido());
        paciente.setDatosActualizadosEn(LocalDateTime.now());

        return pacienteRepository.save(paciente);
    }
}
