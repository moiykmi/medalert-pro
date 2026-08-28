package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Necesario antes de poder borrar una cita: notificacion.cita_id
    // referencia cita sin ON DELETE CASCADE.
    void deleteByCitaId(Long citaId);
}
