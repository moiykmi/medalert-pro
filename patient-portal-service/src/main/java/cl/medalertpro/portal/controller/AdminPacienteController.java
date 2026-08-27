package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.entity.Notificacion;
import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.NotificacionRepository;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
}
