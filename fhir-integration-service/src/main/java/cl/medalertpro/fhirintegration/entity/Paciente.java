package cl.medalertpro.fhirintegration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "paciente")
@Getter
@Setter
@NoArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "canal_preferido", nullable = false, length = 20)
    private String canalPreferido = "SMS";

    @Column(name = "adulto_mayor", nullable = false)
    private boolean adultoMayor = false;

    @Column(name = "datos_actualizados_en")
    private LocalDateTime datosActualizadosEn;
}
