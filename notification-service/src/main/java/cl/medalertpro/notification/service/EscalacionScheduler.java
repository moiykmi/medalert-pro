package cl.medalertpro.notification.service;

import cl.medalertpro.notification.entity.EventoCancelacion;
import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.EventoCancelacionRepository;
import cl.medalertpro.notification.repository.NotificacionRepository;
import cl.medalertpro.notification.repository.PacienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HU06-08: revisa periódicamente las notificaciones ENVIADO sin confirmar hace
 * más de N minutos, y escala al siguiente canal disponible (máx. 3 intentos,
 * ciclo SMS -> WHATSAPP -> EMAIL, saltando el/los canal(es) ya intentados).
 */
@Service
public class EscalacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalacionScheduler.class);
    private static final List<String> ORDEN_CANALES = List.of("SMS", "WHATSAPP", "EMAIL");

    private final NotificacionRepository notificacionRepository;
    private final PacienteRepository pacienteRepository;
    private final EventoCancelacionRepository eventoRepository;
    private final NotificacionDispatchService dispatchService;
    private final ConfiguracionService configuracionService;

    public EscalacionScheduler(NotificacionRepository notificacionRepository,
                               PacienteRepository pacienteRepository,
                               EventoCancelacionRepository eventoRepository,
                               NotificacionDispatchService dispatchService,
                               ConfiguracionService configuracionService) {
        this.notificacionRepository = notificacionRepository;
        this.pacienteRepository = pacienteRepository;
        this.eventoRepository = eventoRepository;
        this.dispatchService = dispatchService;
        this.configuracionService = configuracionService;
    }

    // Revisa cada 1 minuto. La ventana de espera y el máximo de intentos son
    // configurables desde el admin (pantalla Configuración) — configuracionService
    // los lee en cada ciclo, así un cambio se aplica sin reiniciar el servicio.
    @Scheduled(fixedRate = 60_000)
    public void revisarYEscalar() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(configuracionService.escalacionMinutosEspera());
        List<Notificacion> candidatas = notificacionRepository
                .findByEstadoEnvioAndTipoAndConfirmadoEnIsNullAndEnviadoEnBefore("ENVIADO", "CANCELACION", limite);

        if (candidatas.isEmpty()) return;

        // Solo procesar la notificación MÁS RECIENTE por cada par (evento, paciente),
        // para no re-escalar sobre intentos antiguos que ya fueron superados.
        var ultimasPorPar = candidatas.stream()
                .collect(Collectors.toMap(
                        n -> n.getEventoId() + "-" + n.getPacienteId(),
                        n -> n,
                        (a, b) -> a.getIntentoNumero() >= b.getIntentoNumero() ? a : b));

        for (Notificacion ultima : ultimasPorPar.values()) {
            escalarSiCorresponde(ultima);
        }
    }

    private void escalarSiCorresponde(Notificacion candidata) {
        List<Notificacion> historial = notificacionRepository.findByEventoIdAndPacienteIdOrderByIntentoNumeroAsc(
                candidata.getEventoId(), candidata.getPacienteId());

        // Si dos intentos del mismo par (evento, paciente) vencen en ciclos
        // distintos — posible cuando el tiempo de espera configurado es corto,
        // cercano al intervalo de revisión de 1 minuto — la candidata de este
        // ciclo puede ser un intento antiguo ya superado por uno más nuevo que
        // todavía no vence. Si no es realmente el último intento, no hacemos
        // nada: el intento nuevo se evaluará por sí solo cuando también venza.
        Notificacion ultimaNotificacion = historial.stream()
                .max(Comparator.comparing(Notificacion::getIntentoNumero))
                .orElse(candidata);
        if (!ultimaNotificacion.getId().equals(candidata.getId())) {
            return;
        }

        int maxIntentos = configuracionService.escalacionMaxIntentos();
        if (ultimaNotificacion.getIntentoNumero() >= maxIntentos) {
            log.info("Paciente {} (evento {}) alcanzó el máximo de {} intentos sin confirmar — no se escala más",
                    ultimaNotificacion.getPacienteId(), ultimaNotificacion.getEventoId(), maxIntentos);
            marcarSinRespuesta(ultimaNotificacion);
            return;
        }

        List<String> canalesYaUsados = historial.stream().map(Notificacion::getCanal).toList();

        String siguienteCanal = ORDEN_CANALES.stream()
                .filter(c -> !canalesYaUsados.contains(c))
                .filter(configuracionService::isCanalHabilitado)
                .findFirst()
                .orElse(null);

        if (siguienteCanal == null) {
            log.info("Paciente {} (evento {}) ya intentó todos los canales disponibles (o los restantes están deshabilitados) — no se escala más",
                    ultimaNotificacion.getPacienteId(), ultimaNotificacion.getEventoId());
            marcarSinRespuesta(ultimaNotificacion);
            return;
        }

        Paciente paciente = pacienteRepository.findById(ultimaNotificacion.getPacienteId()).orElse(null);
        EventoCancelacion evento = eventoRepository.findById(ultimaNotificacion.getEventoId()).orElse(null);
        if (paciente == null || evento == null) {
            log.warn("No se pudo escalar notificación {} — paciente o evento ya no existen", ultimaNotificacion.getId());
            return;
        }

        String texto = MensajeBuilder.construir(paciente.getNombre(), evento.getMotivo());
        short nuevoIntento = (short) (historial.size() + 1);

        log.info("Escalando paciente {} (evento {}) de {} a {} — intento {}/{}",
                paciente.getId(), evento.getId(), ultimaNotificacion.getCanal(), siguienteCanal,
                nuevoIntento, maxIntentos);

        dispatchService.enviarYRegistrar(
                evento.getId(), paciente.getId(), ultimaNotificacion.getCitaId(),
                siguienteCanal, paciente.getTelefono(), paciente.getEmail(), texto, nuevoIntento);
    }

    /**
     * Marca la notificación como SIN_RESPUESTA (en vez de dejarla en ENVIADO
     * para siempre) para que el scheduler deje de volver a evaluarla en cada
     * ciclo. Sin esto, cada notificación que agota sus intentos queda
     * generando el mismo log y la misma consulta a la BD cada minuto, de
     * forma indefinida, mientras el proyecto siga corriendo.
     */
    private void marcarSinRespuesta(Notificacion notificacion) {
        notificacion.setEstadoEnvio("SIN_RESPUESTA");
        notificacionRepository.save(notificacion);
    }
}