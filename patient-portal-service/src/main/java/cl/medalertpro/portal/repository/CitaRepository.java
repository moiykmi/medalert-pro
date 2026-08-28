package cl.medalertpro.portal.repository;

import cl.medalertpro.portal.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    // Usado al reagendar: evita que dos citas AGENDADA del mismo profesional
    // queden con bloques de 30 min que se solapan (ver ReglaHorarioCita).
    boolean existsByProfesionalIdAndEstadoAndFechaHoraBetween(
            Long profesionalId, String estado, LocalDateTime desde, LocalDateTime hasta);
}
