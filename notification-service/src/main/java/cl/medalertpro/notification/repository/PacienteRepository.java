package cl.medalertpro.notification.repository;

import cl.medalertpro.notification.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
