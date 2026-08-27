package cl.medalertpro.notification.service;

import cl.medalertpro.notification.entity.Cita;
import cl.medalertpro.notification.entity.Paciente;
import cl.medalertpro.notification.repository.CitaRepository;
import cl.medalertpro.notification.repository.NotificacionRepository;
import cl.medalertpro.notification.repository.PacienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Recordatorios preventivos de citas: envía un aviso 48h y otro 24h antes de
 * cada cita AGENDADA, por el canal preferido del paciente, sin escalamiento
 * (a diferencia del aviso de cancelación). Corre una vez al día — "48h antes"
 * y "24h antes" son, en la práctica, granularidad de día calendario: a las
 * 09:00 se avisa de las citas de pasado mañana y de mañana.
 */
@Service
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);
    private static final String ASUNTO = "Recordatorio de cita — MedAlert Pro";

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionDispatchService dispatchService;

    public RecordatorioScheduler(CitaRepository citaRepository, PacienteRepository pacienteRepository,
                                  NotificacionRepository notificacionRepository, NotificacionDispatchService dispatchService) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.notificacionRepository = notificacionRepository;
        this.dispatchService = dispatchService;
    }

    @Scheduled(cron = "${medalert.recordatorios.cron}")
    public void enviarRecordatorios() {
        LocalDate hoy = LocalDate.now();
        procesarDia(hoy.plusDays(2), "RECORDATORIO_48H", 48);
        procesarDia(hoy.plusDays(1), "RECORDATORIO_24H", 24);
    }

    private void procesarDia(LocalDate fechaCita, String tipo, int horasAntes) {
        List<Cita> citas = citaRepository.findByEstadoAndFechaHoraBetween(
                "AGENDADA", fechaCita.atStartOfDay(), fechaCita.atTime(LocalTime.MAX));

        for (Cita cita : citas) {
            if (notificacionRepository.existsByCitaIdAndTipo(cita.getId(), tipo)) {
                continue; // ya se envió este recordatorio para esta cita
            }

            Paciente paciente = pacienteRepository.findById(cita.getPacienteId()).orElse(null);
            if (paciente == null) {
                log.warn("Cita {} sin paciente asociado — se omite recordatorio {}", cita.getId(), tipo);
                continue;
            }

            String canal = normalizarCanal(paciente.getCanalPreferido());
            String texto = MensajeBuilder.construirRecordatorio(paciente.getNombre(), cita.getFechaHora(), horasAntes);

            dispatchService.enviarRecordatorioYRegistrar(
                    cita.getId(), paciente.getId(), tipo, canal, paciente.getTelefono(), paciente.getEmail(), ASUNTO, texto);
        }

        log.info("Recordatorios {} procesados para {} citas del {}", tipo, citas.size(), fechaCita);
    }

    private String normalizarCanal(String canalPreferido) {
        if (canalPreferido == null) return "SMS";
        return switch (canalPreferido.toUpperCase()) {
            case "WHATSAPP" -> "WHATSAPP";
            case "EMAIL" -> "EMAIL";
            default -> "SMS";
        };
    }
}
