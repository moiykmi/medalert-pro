package cl.medalertpro.notification.service;

import cl.medalertpro.notification.entity.Notificacion;
import cl.medalertpro.notification.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificacionDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionDispatchService.class);

    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;
    private final NotificacionRepository notificacionRepository;

    public NotificacionDispatchService(SmsService smsService, WhatsAppService whatsAppService,
                                        EmailService emailService, NotificacionRepository notificacionRepository) {
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
        this.emailService = emailService;
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Envía por el canal indicado y guarda el resultado (ENVIADO o FALLIDO) en la
     * tabla notificacion. intentoNumero indica si es el envío inicial (1) o una
     * escalación (2, 3...).
     */
    public Notificacion enviarYRegistrar(Long eventoId, Long pacienteId, Long citaId,
                                          String canal, String telefono, String email,
                                          String texto, short intentoNumero) {
        Notificacion notificacion = new Notificacion();
        notificacion.setEventoId(eventoId);
        notificacion.setPacienteId(pacienteId);
        notificacion.setCitaId(citaId);
        notificacion.setCanal(canal);
        notificacion.setIntentoNumero(intentoNumero);

        try {
            String proveedorMessageId = switch (canal) {
                case "SMS" -> smsService.enviarSms(telefono, texto);
                case "WHATSAPP" -> whatsAppService.enviarWhatsApp(telefono, texto);
                case "EMAIL" -> emailService.enviarEmail(email, "Cita médica cancelada — MedAlert Pro", texto);
                default -> throw new IllegalArgumentException("Canal desconocido: " + canal);
            };
            notificacion.setEstadoEnvio("ENVIADO");
            notificacion.setProveedorMessageId(proveedorMessageId);
            notificacion.setEnviadoEn(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Fallo al enviar {} (intento {}) a paciente {}: {}", canal, intentoNumero, pacienteId, e.getMessage());
            notificacion.setEstadoEnvio("FALLIDO");
        }

        return notificacionRepository.save(notificacion);
    }

    /**
     * Envía un recordatorio preventivo (48h/24h antes de la cita) y lo registra.
     * A diferencia de enviarYRegistrar, no tiene eventoId (no nace de una
     * cancelación) y no escala entre canales — un único intento por el canal
     * preferido del paciente, sin reintento si falla.
     */
    public Notificacion enviarRecordatorioYRegistrar(Long citaId, Long pacienteId, String tipo,
                                                       String canal, String telefono, String email,
                                                       String asunto, String texto) {
        Notificacion notificacion = new Notificacion();
        notificacion.setCitaId(citaId);
        notificacion.setPacienteId(pacienteId);
        notificacion.setTipo(tipo);
        notificacion.setCanal(canal);
        notificacion.setIntentoNumero((short) 1);

        try {
            String proveedorMessageId = switch (canal) {
                case "SMS" -> smsService.enviarSms(telefono, texto);
                case "WHATSAPP" -> whatsAppService.enviarWhatsApp(telefono, texto);
                case "EMAIL" -> emailService.enviarEmail(email, asunto, texto);
                default -> throw new IllegalArgumentException("Canal desconocido: " + canal);
            };
            notificacion.setEstadoEnvio("ENVIADO");
            notificacion.setProveedorMessageId(proveedorMessageId);
            notificacion.setEnviadoEn(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Fallo al enviar recordatorio {} ({}) a paciente {}: {}", tipo, canal, pacienteId, e.getMessage());
            notificacion.setEstadoEnvio("FALLIDO");
        }

        return notificacionRepository.save(notificacion);
    }
}
