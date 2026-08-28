package cl.medalertpro.fhirintegration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reagendamiento")
@Getter
@Setter
@NoArgsConstructor
public class Reagendamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_original_id", nullable = false)
    private Long citaOriginalId;

    @Column(name = "cita_nueva_id")
    private Long citaNuevaId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(nullable = false, length = 30)
    private String estado = "SOLICITADO";
}
