package cl.medalertpro.notification.entity;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "cita_id")
    private Long citaId;

    @Column(nullable = false, length = 20)
    private String canal; // SMS | WHATSAPP | EMAIL

    @Column(name = "intento_numero", nullable = false)
    private short intentoNumero = 1;

    @Column(name = "estado_envio", nullable = false, length = 30)
    private String estadoEnvio = "PENDIENTE"; // PENDIENTE | ENVIADO | FALLIDO | LEIDO | CONFIRMADO

    @Column(name = "proveedor_message_id", length = 150)
    private String proveedorMessageId;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "confirmado_en")
    private LocalDateTime confirmadoEn;
}
