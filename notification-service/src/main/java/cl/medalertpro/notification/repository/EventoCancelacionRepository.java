package cl.medalertpro.notification.repository;

import cl.medalertpro.notification.entity.EventoCancelacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoCancelacionRepository extends JpaRepository<EventoCancelacion, Long> {
}
