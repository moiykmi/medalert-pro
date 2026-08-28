package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.Reagendamiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReagendamientoRepository extends JpaRepository<Reagendamiento, Long> {

    // Necesario antes de poder borrar una cita: reagendamiento referencia
    // cita tanto en cita_original_id como en cita_nueva_id sin ON DELETE CASCADE.
    void deleteByCitaOriginalIdOrCitaNuevaId(Long citaOriginalId, Long citaNuevaId);
}
