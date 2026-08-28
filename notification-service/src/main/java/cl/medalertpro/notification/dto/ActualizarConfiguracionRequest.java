package cl.medalertpro.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ActualizarConfiguracionRequest {

    @NotNull
    private Boolean canalSmsHabilitado;

    @NotNull
    private Boolean canalWhatsappHabilitado;

    @NotNull
    private Boolean canalEmailHabilitado;

    @NotNull
    private Boolean recordatorio48hHabilitado;

    @NotNull
    private Boolean recordatorio24hHabilitado;

    @NotNull
    @Min(1)
    private Integer escalacionMinutosEspera;

    @NotNull
    @Min(1) @Max(10)
    private Integer escalacionMaxIntentos;

    public Boolean getCanalSmsHabilitado() { return canalSmsHabilitado; }
    public void setCanalSmsHabilitado(Boolean canalSmsHabilitado) { this.canalSmsHabilitado = canalSmsHabilitado; }

    public Boolean getCanalWhatsappHabilitado() { return canalWhatsappHabilitado; }
    public void setCanalWhatsappHabilitado(Boolean canalWhatsappHabilitado) { this.canalWhatsappHabilitado = canalWhatsappHabilitado; }

    public Boolean getCanalEmailHabilitado() { return canalEmailHabilitado; }
    public void setCanalEmailHabilitado(Boolean canalEmailHabilitado) { this.canalEmailHabilitado = canalEmailHabilitado; }

    public Boolean getRecordatorio48hHabilitado() { return recordatorio48hHabilitado; }
    public void setRecordatorio48hHabilitado(Boolean recordatorio48hHabilitado) { this.recordatorio48hHabilitado = recordatorio48hHabilitado; }

    public Boolean getRecordatorio24hHabilitado() { return recordatorio24hHabilitado; }
    public void setRecordatorio24hHabilitado(Boolean recordatorio24hHabilitado) { this.recordatorio24hHabilitado = recordatorio24hHabilitado; }

    public Integer getEscalacionMinutosEspera() { return escalacionMinutosEspera; }
    public void setEscalacionMinutosEspera(Integer escalacionMinutosEspera) { this.escalacionMinutosEspera = escalacionMinutosEspera; }

    public Integer getEscalacionMaxIntentos() { return escalacionMaxIntentos; }
    public void setEscalacionMaxIntentos(Integer escalacionMaxIntentos) { this.escalacionMaxIntentos = escalacionMaxIntentos; }
}
