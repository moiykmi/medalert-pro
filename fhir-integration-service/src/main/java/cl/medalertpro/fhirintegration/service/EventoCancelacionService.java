package cl.medalertpro.fhirintegration.service;

import cl.medalertpro.fhirintegration.dto.CancelacionEventoMessage;
import cl.medalertpro.fhirintegration.dto.RegistrarCancelacionRequest;
import cl.medalertpro.fhirintegration.entity.Cita;
import cl.medalertpro.fhirintegration.entity.EventoCancelacion;
import cl.medalertpro.fhirintegration.entity.Paciente;
import cl.medalertpro.fhirintegration.repository.CitaRepository;
import cl.medalertpro.fhirintegration.repository.EventoCancelacionRepository;
import cl.medalertpro.fhirintegration.repository.PacienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HU01-02: al registrarse un evento de cancelación, busca las citas AGENDADAS
 * del profesional para ese día y publica un mensaje en RabbitMQ con el listado
 * de pacientes afectados, para que notification-service dispare el envío multicanal.
 */
@Service
public class EventoCancelacionService {

    private static final Logger log = LoggerFactory.getLogger(EventoCancelacionService.class);

    private final EventoCancelacionRepository eventoRepository;
    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${medalert.rabbitmq.exchange}")
    private String exchange;

    @Value("${medalert.rabbitmq.routing-key}")
    private String routingKey;

    public EventoCancelacionService(EventoCancelacionRepository eventoRepository,
                                     CitaRepository citaRepository,
                                     PacienteRepository pacienteRepository,
                                     RabbitTemplate rabbitTemplate) {
        this.eventoRepository = eventoRepository;
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public EventoCancelacion registrarYPublicar(RegistrarCancelacionRequest request) {

        // 1. Guardar el evento
        EventoCancelacion evento = new EventoCancelacion();
        evento.setProfesionalId(request.getProfesionalId());
        evento.setMotivo(request.getMotivo());
        evento.setRegistradoPor(request.getRegistradoPor());
        evento.setEstado("PROCESANDO");
        evento = eventoRepository.save(evento);

        // 2. Buscar citas AGENDADAS de ese profesional para todo el día indicado
        LocalDateTime desde = request.getFecha().atStartOfDay();
        LocalDateTime hasta = request.getFecha().atTime(23, 59, 59);

        List<Cita> citasAfectadas = citaRepository.findByProfesionalIdAndEstadoAndFechaHoraBetween(
                request.getProfesionalId(), "AGENDADA", desde, hasta);

        log.info("Evento {} — {} citas afectadas para profesional {} el {}",
                evento.getId(), citasAfectadas.size(), request.getProfesionalId(), request.getFecha());

        // 3. Construir el listado de pacientes afectados
        List<CancelacionEventoMessage.PacienteAfectado> pacientesAfectados = citasAfectadas.stream()
                .map(cita -> {
                    Paciente p = pacienteRepository.findById(cita.getPacienteId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Paciente " + cita.getPacienteId() + " no encontrado para cita " + cita.getId()));
                    return new CancelacionEventoMessage.PacienteAfectado(
                            p.getId(), cita.getId(), p.getNombre(), p.getTelefono(),
                            p.getEmail(), p.getCanalPreferido());
                })
                .collect(Collectors.toList());

        // 4. Marcar las citas como CANCELADA
        citasAfectadas.forEach(c -> c.setEstado("CANCELADA"));
        citaRepository.saveAll(citasAfectadas);

        // 5. Publicar el mensaje en RabbitMQ
        CancelacionEventoMessage message = new CancelacionEventoMessage(
                evento.getId(), evento.getProfesionalId(), evento.getMotivo(),
                evento.getFechaEvento(), pacientesAfectados);

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Evento {} publicado en RabbitMQ (exchange={}, routingKey={})", evento.getId(), exchange, routingKey);

        evento.setEstado("COMPLETADO");
        return eventoRepository.save(evento);
    }
}
