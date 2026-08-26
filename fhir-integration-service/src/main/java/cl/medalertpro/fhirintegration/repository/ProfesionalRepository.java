package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.ProfesionalSalud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfesionalRepository extends JpaRepository<ProfesionalSalud, Long> {

    Optional<ProfesionalSalud> findByEmail(String email);
}
