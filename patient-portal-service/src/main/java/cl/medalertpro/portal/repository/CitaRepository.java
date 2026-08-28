package cl.medalertpro.portal.repository;

import cl.medalertpro.portal.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);

    // Usado al reagendar: evita que dos citas AGENDADA queden en el mismo
    // horario para el mismo profesional (doble reserva del mismo cupo).
    boolean existsByProfesionalIdAndFechaHoraAndEstado(Long profesionalId, LocalDateTime fechaHora, String estado);
}
