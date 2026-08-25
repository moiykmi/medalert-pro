package cl.medalertpro.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "cita_id")
    private Long citaId;

    @Column(nullable = false, length = 20)
    private String canal;

    @Column(name = "intento_numero", nullable = false)
    private short intentoNumero;

    @Column(name = "estado_envio", nullable = false, length = 30)
    private String estadoEnvio;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "confirmado_en")
    private LocalDateTime confirmadoEn;
}
