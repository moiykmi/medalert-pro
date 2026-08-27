package cl.medalertpro.notification.dto;

import cl.medalertpro.notification.entity.ConfiguracionSistema;

import java.time.LocalDateTime;

public class ConfiguracionResponse {

    private boolean canalSmsHabilitado;
    private boolean canalWhatsappHabilitado;
    private boolean canalEmailHabilitado;
    private boolean recordatorio48hHabilitado;
    private boolean recordatorio24hHabilitado;
    private LocalDateTime actualizadoEn;

    public static ConfiguracionResponse desde(ConfiguracionSistema c) {
        ConfiguracionResponse dto = new ConfiguracionResponse();
        dto.canalSmsHabilitado = c.isCanalSmsHabilitado();
        dto.canalWhatsappHabilitado = c.isCanalWhatsappHabilitado();
        dto.canalEmailHabilitado = c.isCanalEmailHabilitado();
        dto.recordatorio48hHabilitado = c.isRecordatorio48hHabilitado();
        dto.recordatorio24hHabilitado = c.isRecordatorio24hHabilitado();
        dto.actualizadoEn = c.getActualizadoEn();
        return dto;
    }

    public boolean isCanalSmsHabilitado() { return canalSmsHabilitado; }
    public boolean isCanalWhatsappHabilitado() { return canalWhatsappHabilitado; }
    public boolean isCanalEmailHabilitado() { return canalEmailHabilitado; }
    public boolean isRecordatorio48hHabilitado() { return recordatorio48hHabilitado; }
    public boolean isRecordatorio24hHabilitado() { return recordatorio24hHabilitado; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
