package cl.medalertpro.fhirintegration.repository;

import cl.medalertpro.fhirintegration.entity.UsuarioAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, Long> {

    Optional<UsuarioAdmin> findByEmail(String email);
}
