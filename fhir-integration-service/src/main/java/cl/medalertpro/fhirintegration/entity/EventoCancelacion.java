package cl.medalertpro.fhirintegration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_cancelacion")
@Getter
@Setter
@NoArgsConstructor
public class EventoCancelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento = LocalDateTime.now();

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false, length = 30)
    private String estado = "REGISTRADO";

    @Column(name = "registrado_por")
    private Long registradoPor;
}
