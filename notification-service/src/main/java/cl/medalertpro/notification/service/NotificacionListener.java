package cl.medalertpro.notification.service;

import cl.medalertpro.notification.dto.CancelacionEventoMessage;
import cl.medalertpro.notification.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * HU03-05 + HU06: consume "cancelaciones.eventos" y envía la notificación inicial
 * por el canal_preferido de cada paciente (no los 3 canales a la vez). El
 * escalamiento a los siguientes canales si no hay confirmación lo maneja
 * EscalacionScheduler (HU06-08).
 */
@Service
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    private final NotificacionDispatchService dispatchService;
    private final NotificacionRepository notificacionRepository;
    private final ConfiguracionService configuracionService;
    private final MensajeBuilder mensajeBuilder;

    public NotificacionListener(NotificacionDispatchService dispatchService,
                                 NotificacionRepository notificacionRepository,
                                 ConfiguracionService configuracionService,
                                 MensajeBuilder mensajeBuilder) {
        this.dispatchService = dispatchService;
        this.notificacionRepository = notificacionRepository;
        this.configuracionService = configuracionService;
        this.mensajeBuilder = mensajeBuilder;
    }

    @RabbitListener(queues = "${medalert.rabbitmq.queue}")
    public void procesarEventoCancelacion(CancelacionEventoMessage evento) {
        log.info("Procesando evento {} — {} pacientes afectados",
                evento.getEventoId(), evento.getPacientesAfectados().size());

        for (CancelacionEventoMessage.PacienteAfectado paciente : evento.getPacientesAfectados()) {

            // Idempotencia: si RabbitMQ reentrega este mensaje (ej. tras un reinicio
            // del microservicio con un mensaje sin confirmar), evita duplicar el
            // envío inicial para este paciente en este evento.
            if (notificacionRepository.existsByEventoIdAndPacienteId(evento.getEventoId(), paciente.getPacienteId())) {
                log.warn("Evento {} paciente {} ya tiene una notificación registrada — se ignora reentrega duplicada",
                        evento.getEventoId(), paciente.getPacienteId());
                continue;
            }

            Optional<String> canalInicial = configuracionService.resolverCanalEnvio(paciente.getCanalPreferido());
            if (canalInicial.isEmpty()) {
                log.warn("Evento {} paciente {} — el admin deshabilitó los 3 canales de notificación, no se envía nada",
                        evento.getEventoId(), paciente.getPacienteId());
                continue;
            }

            String texto = mensajeBuilder.construir(paciente.getNombre(), evento.getMotivo());
            dispatchService.enviarYRegistrar(
                    evento.getEventoId(), paciente.getPacienteId(), paciente.getCitaId(),
                    canalInicial.get(), paciente.getTelefono(), paciente.getEmail(), texto, (short) 1);
        }
    }
}
