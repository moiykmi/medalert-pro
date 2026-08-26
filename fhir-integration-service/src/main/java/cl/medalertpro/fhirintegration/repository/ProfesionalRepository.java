package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfesionalRepository extends JpaRepository<ProfesionalSalud, Long> {
}
