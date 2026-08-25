package cl.medalertpro.portal.entity;

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
    private String canalPreferido;

    @Column(name = "adulto_mayor", nullable = false)
    private boolean adultoMayor;

    @Column(name = "datos_actualizados_en")
    private LocalDateTime datosActualizadosEn;
}
