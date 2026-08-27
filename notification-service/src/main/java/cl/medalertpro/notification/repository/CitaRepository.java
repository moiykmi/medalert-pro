package cl.medalertpro.notification.repository;

import cl.medalertpro.notification.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByEstadoAndFechaHoraBetween(String estado, LocalDateTime desde, LocalDateTime hasta);
}
