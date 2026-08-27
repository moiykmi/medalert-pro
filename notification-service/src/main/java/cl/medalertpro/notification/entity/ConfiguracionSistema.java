package cl.medalertpro.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionSistema {

    public static final Long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(name = "canal_sms_habilitado", nullable = false)
    private boolean canalSmsHabilitado = true;

    @Column(name = "canal_whatsapp_habilitado", nullable = false)
    private boolean canalWhatsappHabilitado = true;

    @Column(name = "canal_email_habilitado", nullable = false)
    private boolean canalEmailHabilitado = true;

    @Column(name = "recordatorio_48h_habilitado", nullable = false)
    private boolean recordatorio48hHabilitado = true;

    @Column(name = "recordatorio_24h_habilitado", nullable = false)
    private boolean recordatorio24hHabilitado = true;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
