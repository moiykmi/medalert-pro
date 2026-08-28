package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.PacienteRepository;
import cl.medalertpro.notification.service.AdminAuthGuard;
import cl.medalertpro.notification.service.ConfiguracionService;
import cl.medalertpro.notification.service.MensajeBuilder;
import cl.medalertpro.notification.service.NotificacionDispatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * "Enviar prueba" del detalle de paciente (medalert_pro_mockup_v2.html,
 * pantalla Pacientes): dispara un envío real por el canal resuelto del
 * paciente (su preferido, o el primero habilitado si el admin lo deshabilitó),
 * sin asociarlo a ningún evento de cancelación — solo para verificar que el
 * canal efectivamente llega.
 */
@RestController
@RequestMapping("/admin/pacientes")
public class AdminNotificacionPruebaController {

    private final PacienteRepository pacienteRepository;
    private final ConfiguracionService configuracionService;
    private final NotificacionDispatchService dispatchService;
    private final AdminAuthGuard authGuard;
    private final MensajeBuilder mensajeBuilder;

    public AdminNotificacionPruebaController(PacienteRepository pacienteRepository, ConfiguracionService configuracionService,
                                              NotificacionDispatchService dispatchService, AdminAuthGuard authGuard,
                                              MensajeBuilder mensajeBuilder) {
        this.pacienteRepository = pacienteRepository;
        this.configuracionService = configuracionService;
        this.dispatchService = dispatchService;
        this.authGuard = authGuard;
        this.mensajeBuilder = mensajeBuilder;
    }

    @PostMapping("/{id}/notificacion-prueba")
    public Notificacion enviarPrueba(@PathVariable Long id, HttpServletRequest request) {
        authGuard.validar(request);

        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));

        String canal = configuracionService.resolverCanalEnvio(paciente.getCanalPreferido())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay ningún canal de notificación habilitado — revisa Configuración"));

        String texto = mensajeBuilder.construirPrueba(paciente.getNombre());
        return dispatchService.enviarRecordatorioYRegistrar(
                null, paciente.getId(), "PRUEBA", canal, paciente.getTelefono(), paciente.getEmail(),
                "Mensaje de prueba — MedAlert Pro", texto);
    }
}
