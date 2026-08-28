package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Todas las citas AGENDADAS de un profesional en una fecha específica (día completo)
    List<Cita> findByProfesionalIdAndEstadoAndFechaHoraBetween(
            Long profesionalId, String estado, LocalDateTime desde, LocalDateTime hasta);

    // Todas las citas de todos los profesionales en una fecha (cualquier estado) — vista calendario del día.
    List<Cita> findByFechaHoraBetweenOrderByFechaHoraAsc(LocalDateTime desde, LocalDateTime hasta);
}
