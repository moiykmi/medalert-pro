package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.entity.Paciente;
import cl.medalertpro.portal.repository.PacienteRepository;
import cl.medalertpro.portal.service.AdminAuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final AdminAuthGuard authGuard;

    public AdminPacienteController(PacienteRepository pacienteRepository, AdminAuthGuard authGuard) {
        this.pacienteRepository = pacienteRepository;
        this.authGuard = authGuard;
    }

    @GetMapping
    public List<Paciente> listar(HttpServletRequest request) {
        authGuard.validar(request);
        return pacienteRepository.findAll();
    }
}
