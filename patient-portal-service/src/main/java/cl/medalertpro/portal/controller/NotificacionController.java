package cl.medalertpro.portal.controller;

import cl.medalertpro.portal.entity.Notificacion;
import cl.medalertpro.portal.repository.NotificacionRepository;
import cl.medalertpro.portal.service.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU11: el paciente ve sus notificaciones y puede confirmar recepción,
 * lo que detiene el escalamiento automático en notification-service
 * (ambos leen/escriben la misma tabla "notificacion").
 */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;
    private final AuthGuard authGuard;

    public NotificacionController(NotificacionRepository notificacionRepository, AuthGuard authGuard) {
        this.notificacionRepository = notificacionRepository;
        this.authGuard = authGuard;
    }

    @GetMapping
    public List<Notificacion> misNotificaciones(HttpServletRequest request) {
        Long pacienteId = authGuard.pacienteAutenticado(request);
        return notificacionRepository.findByPacienteIdOrderByEnviadoEnDesc(pacienteId);
    }

    @PostMapping("/{id}/confirmar")
    public Notificacion confirmar(@PathVariable Long id, HttpServletRequest request) {
        Long pacienteId = authGuard.pacienteAutenticado(request);

        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no existe"));

        // Seguridad: un paciente solo puede confirmar sus propias notificaciones
        if (!notificacion.getPacienteId().equals(pacienteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta notificación no pertenece al paciente autenticado");
        }

        notificacion.setEstadoEnvio("CONFIRMADO");
        notificacion.setConfirmadoEn(LocalDateTime.now());
        return notificacionRepository.save(notificacion);
    }
}
