package cl.medalertpro.portal.repository;

import cl.medalertpro.portal.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByPacienteIdOrderByFechaHoraDesc(Long pacienteId);
}
