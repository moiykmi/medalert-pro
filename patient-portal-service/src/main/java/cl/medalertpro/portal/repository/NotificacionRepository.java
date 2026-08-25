package cl.medalertpro.portal.repository;

import cl.medalertpro.portal.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByPacienteIdOrderByEnviadoEnDesc(Long pacienteId);
}
