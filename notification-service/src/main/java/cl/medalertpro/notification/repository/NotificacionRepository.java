package cl.medalertpro.notification.repository;

import cl.medalertpro.notification.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Notificaciones de cancelación enviadas hace más de X minutos, sin confirmación
    // todavía — acotado a tipo=CANCELACION para que el ciclo de escalamiento
    // SMS→WHATSAPP→EMAIL no alcance a los recordatorios (que no escalan).
    List<Notificacion> findByEstadoEnvioAndTipoAndConfirmadoEnIsNullAndEnviadoEnBefore(
            String estadoEnvio, String tipo, LocalDateTime cutoff);

    // Historial completo de intentos para un paciente dentro de un mismo evento
    List<Notificacion> findByEventoIdAndPacienteIdOrderByIntentoNumeroAsc(Long eventoId, Long pacienteId);

    // Usado para idempotencia: evita procesar el mismo evento+paciente dos veces
    // si RabbitMQ reentrega un mensaje (ej. tras un reinicio del microservicio
    // con un mensaje a medio procesar sin confirmar).
    boolean existsByEventoIdAndPacienteId(Long eventoId, Long pacienteId);

    // Idempotencia de recordatorios: evita reenviar el mismo recordatorio (48h/24h)
    // dos veces para la misma cita si el scheduler corre más de una vez el mismo día.
    boolean existsByCitaIdAndTipo(Long citaId, String tipo);
}
