package cl.medalertpro.fhirintegration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profesional_salud")
@Getter
@Setter
@NoArgsConstructor
public class ProfesionalSalud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String especialidad;

    @Column(name = "establecimiento_id", nullable = false)
    private Long establecimientoId;
}
