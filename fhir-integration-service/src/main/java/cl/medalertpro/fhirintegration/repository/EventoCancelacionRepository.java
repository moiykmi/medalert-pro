package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.EventoCancelacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoCancelacionRepository extends JpaRepository<EventoCancelacion, Long> {
}
