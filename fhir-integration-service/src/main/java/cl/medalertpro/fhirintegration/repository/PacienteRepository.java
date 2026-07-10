package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
}
