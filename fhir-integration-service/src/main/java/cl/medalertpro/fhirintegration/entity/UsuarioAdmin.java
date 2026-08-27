package cl.medalertpro.fhirintegration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_admin")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 50)
    private String rol; // ADMIN | PERSONAL_ADMINISTRATIVO | AUDITORIA

    @Column(name = "establecimiento_id", nullable = false)
    private Long establecimientoId;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;
}
