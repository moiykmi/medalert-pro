package cl.medalertpro.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_cancelacion")
@Getter
@Setter
@NoArgsConstructor
public class EventoCancelacion {

    @Id
    private Long id;

    @Column(length = 200)
    private String motivo;
}
