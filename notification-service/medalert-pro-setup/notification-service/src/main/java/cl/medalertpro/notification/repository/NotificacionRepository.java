package cl.medalertpro.notification.repository;

import cl.medalertpro.notification.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Notificaciones enviadas hace más de X minutos, sin confirmación del paciente todavía
    List<Notificacion> findByEstadoEnvioAndConfirmadoEnIsNullAndEnviadoEnBefore(
            String estadoEnvio, LocalDateTime cutoff);

    // Historial completo de intentos para un paciente dentro de un mismo evento
    List<Notificacion> findByEventoIdAndPacienteIdOrderByIntentoNumeroAsc(Long eventoId, Long pacienteId);

    // Usado para idempotencia: evita procesar el mismo evento+paciente dos veces
    // si RabbitMQ reentrega un mensaje (ej. tras un reinicio del microservicio
    // con un mensaje a medio procesar sin confirmar).
    boolean existsByEventoIdAndPacienteId(Long eventoId, Long pacienteId);
}
