package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.repository.NotificacionRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoint de prueba para simular que el paciente confirmó la lectura de una
 * notificación (en el sistema real, esto lo dispararía la respuesta del paciente
 * por SMS/WhatsApp, o un click en el portal web — fuera del alcance de HU06-08).
 */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping
    public List<Notificacion> listar() {
        return notificacionRepository.findAll();
    }

    @PostMapping("/{id}/confirmar")
    public Notificacion confirmar(@PathVariable Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación " + id + " no existe"));
        notificacion.setEstadoEnvio("CONFIRMADO");
        notificacion.setConfirmadoEn(LocalDateTime.now());
        return notificacionRepository.save(notificacion);
    }
}
