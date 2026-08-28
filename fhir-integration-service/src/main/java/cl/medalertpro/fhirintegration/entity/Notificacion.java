package cl.medalertpro.fhirintegration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapeo mínimo de la tabla notificacion (propiedad de notification-service) —
 * solo lo necesario para poder soltar su referencia a cita(id) antes de un
 * borrado administrativo de citas de prueba (ver AdminCitaController).
 */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id")
    private Long citaId;
}
